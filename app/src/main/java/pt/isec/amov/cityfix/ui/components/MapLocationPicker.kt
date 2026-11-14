package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.utils.LocationUtils

@Composable
fun MapLocationPicker(
    initialLat: Double?,
    initialLng: Double?,
    onLocationSelected: (lat: Double, lng: Double, address: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val defaultPos = LatLng(initialLat ?: 40.2056, initialLng ?: -8.4195) // Coimbra, PT
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 15f)
    }
    var selectedPin by remember {
        mutableStateOf(if (initialLat != null && initialLng != null) LatLng(initialLat, initialLng) else null)
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraState,
        onMapClick = { point ->
            selectedPin = point
            coroutineScope.launch {
                val addr = LocationUtils.reverseGeocode(context, point.latitude, point.longitude)
                onLocationSelected(point.latitude, point.longitude, addr)
            }
        }
    ) {
        selectedPin?.let { pin ->
            val markerState = remember(pin) {
                MarkerState(position = pin)
            }
            Marker(
                state = markerState,
                title = "Localização Selecionada"
            )
        }
    }
}
