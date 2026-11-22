package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.AppConfig
import kotlinx.coroutines.withTimeoutOrNull
import pt.isec.amov.cityfix.model.IncidentLifecycle
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.StatusChange
import pt.isec.amov.cityfix.model.UserRole
import java.util.concurrent.TimeUnit

/**
 * Que incidentes pedir ao Firestore, conforme quem está a ver.
 * As regras de segurança (passo 4.k) só deixam fazer estas três consultas.
 */
sealed interface IncidentQuery {
    // Públicos e ativos: Aberto, Pedido de Informações ou Em Resolução (anónimo, dashboard e separador "Públicas")
    data object PublicActive : IncidentQuery

    // Os do próprio munícipe
    data class ByUser(val uid: String) : IncidentQuery

    // Todos (operador)
    data object All : IncidentQuery

    // Ocorrências de grupo que o munícipe acompanha (reportou o mesmo problema)
    data class Followed(val uid: String) : IncidentQuery
}

/**
 * Repositório dos incidentes, agora sobre o Cloud Firestore (coleção "incidents").
 * Substitui o ficheiro JSON da etapa anterior: o Firestore guarda uma cópia local (cache)
 * e aceita leituras e escritas sem rede, que sincroniza quando a rede volta.
 * Os ViewModels continuam a só conhecer esta classe.
 */
class IncidentsRepository(
    private val context: Context,
    private val statsRepository: StatsRepository
) {

    private val db = Firebase.firestore
    private val incidentsCollection = db.collection("incidents")

    // Incidentes recebidos de todas as consultas ativas, por ID (para getIncidentById, que é síncrono).
    // Um mapa e não "a última lista": o dashboard e a lista têm consultas diferentes ao mesmo tempo.
    private val cachedIncidents = ConcurrentHashMap<String, Incident>()

    /**
     * Procura nos incidentes já recebidos (síncrono). Usado pelo EditIncidentViewModel.
     */
    fun getIncidentById(id: String): Incident? = cachedIncidents[id]

    /**
     * Lista de incidentes em tempo real: um Flow que emite uma lista nova sempre que
     * algum documento muda (neste dispositivo, noutro, ou quando chega a sincronização).
     * O listener é removido quando ninguém está a recolher o Flow (awaitClose).
     */
    fun observeIncidents(query: IncidentQuery): Flow<List<Incident>> = callbackFlow {
        val firestoreQuery = when (query) {
            IncidentQuery.PublicActive -> incidentsCollection
                .whereEqualTo("scope", Scope.PUBLIC.name)
                .whereIn("status", IncidentStatus.entries.filter { it.isActive }.map { it.name })
            is IncidentQuery.ByUser -> incidentsCollection.whereEqualTo("userId", query.uid)
            IncidentQuery.All -> incidentsCollection
            is IncidentQuery.Followed -> incidentsCollection.whereArrayContains("followers", query.uid)
        }
        var received = false
        val registration = firestoreQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Ex: regras do Firestore a recusar a leitura. Mantém-se a última lista
                // (ou uma lista vazia, se ainda não tinha chegado nenhuma: quem junta várias
                // consultas com combine não pode ficar à espera desta para sempre).
                Log.w(TAG, "Erro a ler os incidentes", error)
                if (!received) trySend(emptyList())
                return@addSnapshotListener
            }
            received = true
            val list = snapshot?.documents?.mapNotNull { it.toIncident() } ?: emptyList()
            list.forEach { cachedIncidents[it.id] = it }
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Cria ou atualiza o incidente (o ID do documento é o ID do incidente).
     * Se for novo, atualiza também os indicadores (stats/global) no mesmo WriteBatch.
     * Não se espera pela resposta do servidor (sem await): sem rede, a escrita fica na fila
     * local do Firestore e o snapshot listener mostra-a logo na lista.
     */
    fun saveIncident(incident: Incident, isNew: Boolean = false) {
        val batch = db.batch()
        batch.set(incidentsCollection.document(incident.id), incident.toFirestoreMap())
        if (isNew) statsRepository.addCreated(batch)
        batch.commit()
    }

    /**
     * Apaga o documento e, se já tinham sido enviadas, as imagens no Storage.
     */
    fun deleteIncident(incident: Incident) {
        val batch = db.batch()
        batch.delete(incidentsCollection.document(incident.id))
        statsRepository.addDeleted(batch, incident)
        batch.commit()
        cachedIncidents.remove(incident.id)
        deleteRemoteMedia(incident)
    }

    /**
     * Apaga do Storage as imagens e a miniatura de um incidente (se existirem).
     */
    fun deleteRemoteMedia(incident: Incident) {
        (incident.mediaUrls + listOfNotNull(incident.thumbnailUrl)).forEach { url ->
            Firebase.storage.getReferenceFromUrl(url).delete()
        }
    }

    /**
     * Pede ao WorkManager para enviar as imagens do incidente quando houver rede.
     * O trabalho sobrevive ao fecho da app e a reinícios do telemóvel.
     * REPLACE: se o mesmo incidente já tinha um envio pendente, fica só o mais recente.
     */
    fun scheduleMediaUpload(incidentId: String) {
        val request = OneTimeWorkRequestBuilder<MediaUploadWorker>()
            .setInputData(workDataOf(MediaUploadWorker.KEY_INCIDENT_ID to incidentId))
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("upload_$incidentId", ExistingWorkPolicy.REPLACE, request)
    }

    // ---------------------------------------------------------------------------------------------
    // Ocorrências de grupo (enunciado I.2, Obs.)
    // ---------------------------------------------------------------------------------------------

    /**
     * Procura uma ocorrência ativa e pública com a mesma categoria e situação a menos de 10 m,
     * de outro munícipe e que este ainda não acompanhe. As privadas de outros não se podem ler
     * (regras), por isso só as públicas contam. Sem resposta em 5 s (ex.: sem rede e sem cache),
     * devolve null e a ocorrência é criada normalmente.
     */
    suspend fun findGroupMatch(
        category: String,
        situation: Situation,
        latitude: Double,
        longitude: Double,
        uid: String
    ): Incident? = try {
        withTimeoutOrNull(5_000) {
            incidentsCollection
                .whereEqualTo("scope", Scope.PUBLIC.name)
                .whereEqualTo("category", category)
                .whereEqualTo("situation", situation.name)
                .whereIn("status", IncidentStatus.entries.filter { it.isActive }.map { it.name })
                .get().await()
                .documents.mapNotNull { it.toIncident() }
                .filter { it.userId != uid && uid !in it.followers && it.latitude != null && it.longitude != null }
                .map { it to distanceMeters(latitude, longitude, it.latitude!!, it.longitude!!) }
                .filter { (_, distance) -> distance <= AppConfig.GROUP_RADIUS_METERS }
                .minByOrNull { (_, distance) -> distance }
                ?.first
        }
    } catch (e: Exception) {
        Log.w(TAG, "Não foi possível procurar ocorrências iguais", e)
        null
    }

    /**
     * O munícipe passa a acompanhar a ocorrência (ocorrência de grupo): fica na lista "followers".
     */
    fun joinGroup(incidentId: String, uid: String) {
        incidentsCollection.document(incidentId).update("followers", FieldValue.arrayUnion(uid))
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }

    /**
     * Um operador entrou na geofence de 20 m da ocorrência (enunciado I.3, Obs.): fica registado
     * no documento, e os autores são notificados (NotificationsWatcher e Cloud Functions).
     */
    fun markOperatorNearby(incidentId: String): com.google.android.gms.tasks.Task<Void> =
        incidentsCollection.document(incidentId).update("operatorNearbyAt", System.currentTimeMillis())

    // ---------------------------------------------------------------------------------------------
    // Ciclo de vida do ticket (enunciado I.3.E e II)
    // ---------------------------------------------------------------------------------------------

    /**
     * FUNÇÃO PARTILHADA (Parte A e Parte B): muda o estado da ocorrência e acrescenta a entrada
     * ao histórico (estado, data/hora, quem, nota). A Parte B usa-a no pedido de esclarecimento
     * do munícipe (OPEN -> INFO_REQUESTED) e na reabertura (RESOLVED -> OPEN, com a justificação na nota).
     * Transições inválidas (ver IncidentLifecycle) são ignoradas e devolvem false.
     * Também atualiza os indicadores do dashboard no mesmo WriteBatch (reabertura).
     */
    fun changeStatus(incident: Incident, newStatus: IncidentStatus, note: String? = null): Boolean =
        applyStatusChange(incident, newStatus, note, byUid = currentUid())

    /**
     * O operador declara a ocorrência resolvida, com a fotografia comprovativa (enunciado II.4).
     * A fotografia é enviada para o Storage pelo ProofUploadWorker quando houver rede.
     */
    fun resolve(incident: Incident, proofPath: String, note: String? = null): Boolean {
        val done = applyStatusChange(incident, IncidentStatus.RESOLVED, note, byUid = currentUid(), proofPath = proofPath)
        if (done) scheduleProofUpload(incident.id)
        return done
    }

    /**
     * Fecho automático (enunciado II.5): passa a Encerrado as ocorrências resolvidas há mais de 5 dias.
     * Corre no AutoCloseWorker. O operador fecha todas; o munícipe só as suas (as regras não deixam mais).
     * Lê do servidor: sem rede falha e o WorkManager volta a tentar.
     */
    suspend fun closeExpiredResolved(now: Long = System.currentTimeMillis()): Int {
        val uid = Firebase.auth.currentUser?.uid ?: return 0
        val role = db.collection("users").document(uid).get(Source.SERVER).await().getString("role")
        var query = incidentsCollection.whereEqualTo("status", IncidentStatus.RESOLVED.name)
        if (role != UserRole.OPERATOR.name) query = query.whereEqualTo("userId", uid)
        val expired = query.get(Source.SERVER).await().documents
            .mapNotNull { it.toIncident() }
            .filter { incident -> IncidentLifecycle.closesAt(incident)?.let { it <= now } == true }
        expired.forEach { applyStatusChange(it, IncidentStatus.CLOSED, note = null, byUid = StatusChange.SYSTEM) }
        return expired.size
    }

    private fun currentUid(): String = Firebase.auth.currentUser?.uid.orEmpty()

    /**
     * Grava a mudança com update() (só os campos do ciclo de vida, para não apagar o que outro
     * utilizador tenha mudado entretanto) e arrayUnion() no histórico. Funciona sem rede.
     */
    private fun applyStatusChange(
        incident: Incident,
        newStatus: IncidentStatus,
        note: String?,
        byUid: String,
        proofPath: String? = null
    ): Boolean {
        if (!IncidentLifecycle.canTransition(incident.status, newStatus)) {
            Log.w(TAG, "Transição inválida ${incident.status} -> $newStatus em ${incident.id}")
            return false
        }
        val now = System.currentTimeMillis()
        val change = StatusChange(newStatus, now, byUid, note?.trim()?.ifEmpty { null })
        val updates = mutableMapOf<String, Any?>(
            "status" to newStatus.name,
            "history" to FieldValue.arrayUnion(change.toFirestoreMap())
        )
        val batch = db.batch()
        when {
            newStatus == IncidentStatus.RESOLVED -> {
                updates["resolvedAt"] = now
                updates["proofPath"] = proofPath
                updates["proofUrl"] = null
                statsRepository.addResolved(batch, now - incident.timestamp)
            }
            // Reabertura: deixa de contar como resolvida
            incident.status == IncidentStatus.RESOLVED && newStatus == IncidentStatus.OPEN -> {
                incident.resolvedAt?.let { statsRepository.addReopened(batch, it - incident.timestamp) }
                updates["resolvedAt"] = null
            }
        }
        batch.update(incidentsCollection.document(incident.id), updates)
        batch.commit()
        return true
    }

    /**
     * Envio da fotografia de prova pelo WorkManager, só com rede (como as imagens da ocorrência).
     */
    private fun scheduleProofUpload(incidentId: String) {
        val request = OneTimeWorkRequestBuilder<ProofUploadWorker>()
            .setInputData(workDataOf(ProofUploadWorker.KEY_INCIDENT_ID to incidentId))
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("proof_$incidentId", ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        private const val TAG = "IncidentsRepository"
    }
}