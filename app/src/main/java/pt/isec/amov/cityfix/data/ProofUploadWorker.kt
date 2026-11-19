package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.utils.ImageUtils

/**
 * Envia para o Firebase Storage a fotografia comprovativa da resolução (enunciado II.4),
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
            val bytes = ImageUtils.compress(path, maxSide = 1600, quality = 80)
                ?: return Result.failure() // o ficheiro já não existe neste telemóvel
            val ref = Firebase.storage.reference.child("incidents/$incidentId/proof.jpg")
            ref.putBytes(bytes).await()
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
