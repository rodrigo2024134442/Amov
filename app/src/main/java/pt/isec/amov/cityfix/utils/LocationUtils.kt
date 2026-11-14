package pt.isec.amov.cityfix.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object LocationUtils {
    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String =
        suspendCancellableCoroutine { continuation ->
            val geocoder = Geocoder(context, Locale.getDefault())
            val defaultResult = "Lat: %.4f, Lng: %.4f".format(lat, lng)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val addressLine = addresses.firstOrNull()?.getAddressLine(0) ?: defaultResult
                        if (continuation.isActive) {
                            continuation.resume(addressLine)
                        }
                    }
                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resume(defaultResult)
                        }
                    }
                })
            } else {
                @Suppress("DEPRECATION")
                try {
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    val addressLine = addresses?.firstOrNull()?.getAddressLine(0) ?: defaultResult
                    continuation.resume(addressLine)
                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.resume(defaultResult)
                }
            }
        }
}
