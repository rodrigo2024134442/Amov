package pt.isec.amov.cityfix.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import pt.isec.amov.cityfix.CityFixApp
import java.util.concurrent.TimeUnit

/**
 * Fecho automático (enunciado II.5): passa a "Encerrado" as ocorrências resolvidas há mais de 5 dias.
 * Sem servidor (Cloud Functions), corre nos telemóveis com a app: de 6 em 6 horas e sempre que a app abre.
 * Basta um telemóvel (de um operador ou do autor) para a ocorrência fechar.
 */
class AutoCloseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val repository = (applicationContext as CityFixApp).repository
        val closed = repository.closeExpiredResolved()
        if (closed > 0) Log.i(TAG, "$closed ocorrência(s) encerrada(s) automaticamente")
        Result.success()
    } catch (e: Exception) {
        // Ex.: sem sessão ainda confirmada no servidor, ou sem rede a meio
        Log.w(TAG, "Fecho automático falhou (tentativa $runAttemptCount)", e)
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        private const val TAG = "AutoCloseWorker"
        private const val PERIODIC = "auto_close_periodic"
        private const val ON_START = "auto_close_on_start"

        /**
         * Agenda a verificação periódica (6 h) e uma imediata. Chamado ao arrancar a app.
         */
        fun schedule(context: Context) {
            val constraints = Constraints(requiredNetworkType = NetworkType.CONNECTED)
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AutoCloseWorker>(6, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()
            )
            workManager.enqueueUniqueWork(
                ON_START,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AutoCloseWorker>().setConstraints(constraints).build()
            )
        }
    }
}
