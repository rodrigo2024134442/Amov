package pt.isec.amov.cityfix.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import pt.isec.amov.cityfix.AppConfig

/**
 * Localização do dispositivo (Fused Location Provider do Google Play Services).
 * Usada para restringir o munícipe anónimo a 10 km da Praça da República (enunciado I.1).
 */
class LocationRepository(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    /**
     * true se a app já tem permissão de localização (precisa ou aproximada).
     */
    fun hasPermission(): Boolean = LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Posição atual, ou a última conhecida se não for possível obter uma nova em 15 s.
     * Devolve null sem permissão, com a localização desligada ou se não houver nenhuma posição.
     */
    @SuppressLint("MissingPermission") // verificado em hasPermission()
    suspend fun getCurrentLocation(): Location? {
        if (!hasPermission()) return null
        return try {
            val cancellation = CancellationTokenSource()
            val current = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token).await()
            }
            if (current == null) cancellation.cancel()
            current ?: client.lastLocation.await()
        } catch (e: Exception) {
            Log.w(TAG, "Não foi possível obter a localização", e)
            null
        }
    }

    /**
     * Distância em metros entre [location] e o centro da área do anónimo (Praça da República).
     */
    fun distanceToGuestCenter(location: Location): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            AppConfig.GUEST_CENTER_LAT, AppConfig.GUEST_CENTER_LNG,
            result
        )
        return result[0]
    }

    companion object {
        private const val TAG = "LocationRepository"
        private const val LOCATION_TIMEOUT_MS = 15_000L

        // Pedidas juntas: o utilizador pode escolher "precisa" ou "aproximada" (ambas chegam para 10 km)
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
