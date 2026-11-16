package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.MunicipalStats

/**
 * Indicadores do município no documento stats/global (dashboard do anónimo, enunciado I.1.A).
 *
 * Porquê um documento à parte: o anónimo só pode ler as ocorrências públicas, mas o
 * "nº total de ocorrências" inclui as privadas. Sem Cloud Functions, os contadores são
 * atualizados pela própria app com FieldValue.increment(), no MESMO WriteBatch que grava a
 * ocorrência (os dois ficam gravados ou nenhum fica, também sem rede).
 *
 * Quando um operador entra, rebuild() recalcula tudo a partir das ocorrências
 * (corrige diferenças, por exemplo de ocorrências criadas antes de existir este documento).
 */
class StatsRepository {

    private val db = Firebase.firestore
    private val statsDoc = db.collection("stats").document("global")

    /**
     * Indicadores em tempo real. Se o documento ainda não existir, vem tudo a zero.
     */
    fun observeStats(): Flow<MunicipalStats> = callbackFlow {
        val registration = statsDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Erro a ler as estatísticas", error)
                return@addSnapshotListener
            }
            trySend(
                MunicipalStats(
                    total = snapshot?.getLong(TOTAL) ?: 0,
                    active = snapshot?.getLong(ACTIVE) ?: 0,
                    resolvedCount = snapshot?.getLong(RESOLVED) ?: 0,
                    repairMillisSum = snapshot?.getLong(REPAIR_SUM) ?: 0
                )
            )
        }
        awaitClose { registration.remove() }
    }

    /** Uma ocorrência nova (fica ativa). */
    fun addCreated(batch: WriteBatch) = increment(batch, TOTAL to 1L, ACTIVE to 1L)

    /** Uma ocorrência apagada: desfaz o que ela contava. */
    fun addDeleted(batch: WriteBatch, incident: Incident) {
        val changes = mutableListOf(TOTAL to -1L)
        if (incident.status.isActive) changes += ACTIVE to -1L
        repairMillis(incident)?.let {
            changes += RESOLVED to -1L
            changes += REPAIR_SUM to -it
        }
        increment(batch, *changes.toTypedArray())
    }

    /**
     * Para o ciclo de vida do operador (ainda por fazer): chamar no batch que passa a ocorrência
     * a RESOLVED, com repairMillis = resolvedAt - timestamp.
     */
    fun addResolved(batch: WriteBatch, repairMillis: Long) =
        increment(batch, ACTIVE to -1L, RESOLVED to 1L, REPAIR_SUM to repairMillis)

    /**
     * Para a reabertura (ainda por fazer): o inverso de addResolved, com o mesmo repairMillis.
     */
    fun addReopened(batch: WriteBatch, repairMillis: Long) =
        increment(batch, ACTIVE to 1L, RESOLVED to -1L, REPAIR_SUM to -repairMillis)

    /**
     * Recalcula os indicadores a partir de todas as ocorrências. Só o operador pode ler todas.
     * Lê do servidor (Source.SERVER): sem rede falha, em vez de gravar números feitos só com a cache.
     */
    suspend fun rebuild() {
        val incidents = db.collection("incidents").get(Source.SERVER).await()
            .documents.mapNotNull { it.toIncident() }
        val repairs = incidents.mapNotNull { repairMillis(it) }
        statsDoc.set(
            mapOf(
                TOTAL to incidents.size.toLong(),
                ACTIVE to incidents.count { it.status.isActive }.toLong(),
                RESOLVED to repairs.size.toLong(),
                REPAIR_SUM to repairs.sum()
            )
        ).await()
    }

    // Tempo de reparação de uma ocorrência resolvida ou encerrada (null se ainda está ativa)
    private fun repairMillis(incident: Incident): Long? =
        incident.resolvedAt
            ?.takeIf { !incident.status.isActive }
            ?.let { (it - incident.timestamp).coerceAtLeast(0) }

    // merge: cria o documento na primeira vez e só mexe nos campos indicados
    private fun increment(batch: WriteBatch, vararg changes: Pair<String, Long>) {
        val data: Map<String, Any> = changes.associate { (field, delta) -> field to FieldValue.increment(delta) }
        batch.set(statsDoc, data, SetOptions.merge())
    }

    companion object {
        private const val TAG = "StatsRepository"
        private const val TOTAL = "total"
        private const val ACTIVE = "active"
        private const val RESOLVED = "resolvedCount"
        private const val REPAIR_SUM = "repairMillisSum"
    }
}
