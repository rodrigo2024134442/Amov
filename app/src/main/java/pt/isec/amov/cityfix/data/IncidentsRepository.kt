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
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Scope
import java.util.concurrent.TimeUnit

/**
 * Que incidentes pedir ao Firestore, conforme quem está a ver.
 * As regras de segurança (passo 4.k) só deixam fazer estas três consultas.
 */
sealed interface IncidentQuery {
    // Públicos e ainda não encerrados (anónimo, e separador "Públicas" do munícipe)
    data object PublicActive : IncidentQuery

    // Os do próprio munícipe
    data class ByUser(val uid: String) : IncidentQuery

    // Todos (operador)
    data object All : IncidentQuery
}

/**
 * Repositório dos incidentes, agora sobre o Cloud Firestore (coleção "incidents").
 * Substitui o ficheiro JSON da etapa anterior: o Firestore guarda uma cópia local (cache)
 * e aceita leituras e escritas sem rede, que sincroniza quando a rede volta.
 * Os ViewModels continuam a só conhecer esta classe.
 */
class IncidentsRepository(private val context: Context) {

    private val incidentsCollection = Firebase.firestore.collection("incidents")

    // Última lista recebida do Firestore (para getIncidentById, que é síncrono)
    private var cachedIncidents: List<Incident> = emptyList()

    /**
     * Procura na última lista recebida (síncrono). Usado pelo EditIncidentViewModel.
     */
    fun getIncidentById(id: String): Incident? = cachedIncidents.find { it.id == id }

    /**
     * Lista de incidentes em tempo real: um Flow que emite uma lista nova sempre que
     * algum documento muda (neste dispositivo, noutro, ou quando chega a sincronização).
     * O listener é removido quando ninguém está a recolher o Flow (awaitClose).
     */
    fun observeIncidents(query: IncidentQuery): Flow<List<Incident>> = callbackFlow {
        val firestoreQuery = when (query) {
            IncidentQuery.PublicActive -> incidentsCollection
                .whereEqualTo("scope", Scope.PUBLIC.name)
                .whereIn("status", IncidentStatus.entries.filter { it != IncidentStatus.CLOSED }.map { it.name })
            is IncidentQuery.ByUser -> incidentsCollection.whereEqualTo("userId", query.uid)
            IncidentQuery.All -> incidentsCollection
        }
        val registration = firestoreQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Ex: regras do Firestore a recusar a leitura. Mantém-se a última lista.
                Log.w(TAG, "Erro a ler os incidentes", error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toIncident() } ?: emptyList()
            cachedIncidents = list
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Cria ou atualiza o incidente (o ID do documento é o ID do incidente).
     * Não se espera pela resposta do servidor (sem await): sem rede, a escrita fica na fila
     * local do Firestore e o snapshot listener mostra-a logo na lista.
     */
    fun saveIncident(incident: Incident) {
        incidentsCollection.document(incident.id).set(incident.toFirestoreMap())
    }

    /**
     * Apaga o documento e, se já tinham sido enviadas, as imagens no Storage.
     */
    fun deleteIncident(incident: Incident) {
        incidentsCollection.document(incident.id).delete()
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

    companion object {
        private const val TAG = "IncidentsRepository"
    }
}