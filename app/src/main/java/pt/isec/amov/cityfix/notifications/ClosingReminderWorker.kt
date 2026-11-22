package pt.isec.amov.cityfix.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.data.toIncident
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentLifecycle
import pt.isec.amov.cityfix.model.IncidentStatus
import java.util.concurrent.TimeUnit

/**
 * Aviso ao munícipe quando faltam 24 horas para o fecho definitivo do ticket (enunciado III).
 * Agendado pelo NotificationsWatcher quando a ocorrência passa a Resolvido; ao correr, confirma
 * que ainda está resolvida (pode ter sido reaberta entretanto) antes de notificar.
 */
class ClosingReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_INCIDENT_ID) ?: return Result.failure()
        val incident = try {
            Firebase.firestore.collection("incidents").document(id).get().await().toIncident()
        } catch (_: Exception) {
            null
        } ?: return Result.success()
        val closesAt = IncidentLifecycle.closesAt(incident)
        if (incident.status == IncidentStatus.RESOLVED && closesAt != null && System.currentTimeMillis() < closesAt) {
            NotificationHelper.showEvent(applicationContext, NotificationType.CLOSING, incident.id, incident.title)
        }
        return Result.success()
    }

    companion object {
        private const val KEY_INCIDENT_ID = "incidentId"
        private const val DAY_MS = 24L * 60 * 60 * 1000

        private fun tag(incidentId: String) = "closing_$incidentId"

        /**
         * Agenda o aviso para 24 h antes do fecho. O nome inclui o resolvedAt: se a ocorrência for
         * reaberta e resolvida de novo, é um aviso diferente.
         */
        fun schedule(context: Context, incident: Incident) {
            val closesAt = IncidentLifecycle.closesAt(incident) ?: return
            val now = System.currentTimeMillis()
            if (now >= closesAt) return
            val delay = (closesAt - DAY_MS - now).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<ClosingReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_INCIDENT_ID to incident.id))
                .addTag(tag(incident.id))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${tag(incident.id)}_${incident.resolvedAt}", ExistingWorkPolicy.KEEP, request
            )
        }

        /** A ocorrência deixou de estar resolvida (reaberta ou encerrada): o aviso já não faz sentido. */
        fun cancel(context: Context, incidentId: String) {
            WorkManager.getInstance(context).cancelAllWorkByTag(tag(incidentId))
        }
    }
}
