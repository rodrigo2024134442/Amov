package pt.isec.amov.cityfix.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import pt.isec.amov.cityfix.CityFixApp

/**
 * Recebe as entradas do operador nas geofences de 20 m (enviadas pelo Android, mesmo com a app
 * fechada) e regista-as no documento da ocorrência (operatorNearbyAt). Os autores são então
 * notificados: pelo NotificationsWatcher no telemóvel deles e pela Cloud Function (push).
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "Erro da geofence: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val ids = event.triggeringGeofences?.map { it.requestId }.orEmpty()
        if (ids.isEmpty()) return
        val repository = (context.applicationContext as CityFixApp).repository
        // goAsync: dá alguns segundos para a escrita chegar ao Firestore antes de o processo poder terminar
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ids.forEach { id ->
                    Log.i(TAG, "Operador a menos de 20 m da ocorrência $id")
                    withTimeoutOrNull(8_000) { repository.markOperatorNearby(id).await() }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível registar a proximidade", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
