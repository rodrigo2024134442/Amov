package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.ImageUtils
import android.net.Uri
import com.google.firebase.storage.storageMetadata
import java.io.File

/**
 * Envia para o Firebase Storage a fotografia (ou o vídeo) comprovativa da resolução (enunciado II.4),
 * como o MediaUploadWorker faz com as imagens da ocorrência: comprime (1600 px) e só corre com rede.
 * Corre no telemóvel do operador, onde está a fotografia.
 */
class ProofUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val incidentId = inputData.getString(KEY_INCIDENT_ID) ?: return Result.failure()
        val document = Firebase.firestore.collection("incidents").document(incidentId)

        return try {
            val incident = document.get().await().toIncident() ?: return Result.failure()
            val path = incident.proofPath ?: return Result.success() // nada para enviar
            val ref = if (FileUtils.isVideo(path)) {
                // Prova em vídeo: enviada como está
                val file = File(path).takeIf { it.exists() } ?: return Result.failure()
                Firebase.storage.reference.child("incidents/$incidentId/proof.mp4").also {
                    it.putFile(Uri.fromFile(file), storageMetadata { contentType = "video/mp4" }).await()
                }
            } else {
                val bytes = ImageUtils.compress(path, maxSide = 1600, quality = 80)
                    ?: return Result.failure() // o ficheiro já não existe neste telemóvel
                Firebase.storage.reference.child("incidents/$incidentId/proof.jpg").also {
                    it.putBytes(bytes).await()
                }
            }
            document.update("proofUrl", ref.downloadUrl.await().toString()).await()
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Falha no envio da prova de $incidentId (tentativa $runAttemptCount)", e)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_INCIDENT_ID = "incidentId"
        private const val MAX_ATTEMPTS = 5
        private const val TAG = "ProofUploadWorker"
    }
}
