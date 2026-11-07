package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.model.SyncStatus
import pt.isec.amov.cityfix.utils.ImageUtils

/**
 * Trabalho em segundo plano (WorkManager) que envia as imagens de um incidente para o Firebase Storage.
 * Só corre com rede (restrição definida em IncidentsRepository.scheduleMediaUpload) e é repetido
 * automaticamente se falhar. Passos:
 *  1. lê o incidente no Firestore;
 *  2. comprime cada imagem local e envia-a para incidents/{id}/photo_N.jpg;
 *  3. cria e envia a miniatura (incidents/{id}/thumb.jpg);
 *  4. grava no documento os endereços e marca-o como SYNCED.
 */
class MediaUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val incidentId = inputData.getString(KEY_INCIDENT_ID) ?: return Result.failure()
        val document = Firebase.firestore.collection("incidents").document(incidentId)
        val folder = Firebase.storage.reference.child("incidents/$incidentId")

        return try {
            val incident = document.get().await().toIncident()
                ?: return Result.failure() // o incidente foi apagado entretanto

            // Imagens: até 1600 px, qualidade 80 (ficheiros que já não existem são ignorados)
            val urls = incident.mediaPaths.mapIndexedNotNull { index, path ->
                val bytes = ImageUtils.compress(path, maxSide = 1600, quality = 80) ?: return@mapIndexedNotNull null
                val ref = folder.child("photo_$index.jpg")
                ref.putBytes(bytes).await()
                ref.downloadUrl.await().toString()
            }

            // Miniatura: 320 px, gerada a partir da primeira imagem
            val thumbnailUrl = incident.mediaPaths.firstOrNull()
                ?.let { ImageUtils.compress(it, maxSide = 320, quality = 70) }
                ?.let { bytes ->
                    val ref = folder.child("thumb.jpg")
                    ref.putBytes(bytes).await()
                    ref.downloadUrl.await().toString()
                }

            document.update(
                mapOf(
                    "mediaUrls" to urls,
                    "thumbnailUrl" to thumbnailUrl,
                    "syncStatus" to SyncStatus.SYNCED.name
                )
            ).await()
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Falha no envio das imagens de $incidentId (tentativa $runAttemptCount)", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_INCIDENT_ID = "incidentId"
        private const val MAX_ATTEMPTS = 5
        private const val TAG = "MediaUploadWorker"
    }
}