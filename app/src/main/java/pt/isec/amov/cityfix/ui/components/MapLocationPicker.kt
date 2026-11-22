package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.utils.LocationUtils

@Composable
fun MapLocationPicker(
    initialLat: Double?,
    initialLng: Double?,
    onLocationSelected: (lat: Double, lng: Double, address: String) -> Unit,
    modifier: Modifier = Modifier,
    // Se a posição inicial chegar depois (ex.: impacto detetado), obter também a morada
    resolveInitialAddress: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val markerTitle = stringResource(R.string.selected_location)
    val defaultPos = LatLng(initialLat ?: 40.2056, initialLng ?: -8.4195) // Coimbra, PT
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 15f)
    }
    var selectedPin by remember {
        mutableStateOf(if (initialLat != null && initialLng != null) LatLng(initialLat, initialLng) else null)
    }

    // A posição inicial pode chegar depois do primeiro desenho (ex.: GPS no impacto detetado):
    // mostra o marcador, centra o mapa e, se pedido, obtém a morada.
    LaunchedEffect(initialLat, initialLng) {
        if (initialLat == null || initialLng == null) return@LaunchedEffect
        val pos = LatLng(initialLat, initialLng)
        if (pos == selectedPin) return@LaunchedEffect
        selectedPin = pos
        cameraState.position = CameraPosition.fromLatLngZoom(pos, 15f)
        if (resolveInitialAddress) {
            val addr = LocationUtils.reverseGeocode(context, pos.latitude, pos.longitude)
            onLocationSelected(pos.latitude, pos.longitude, addr)
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraState,
        onMapClick = { point ->
            selectedPin = point
            // Grava logo a posição (sem esperar pela morada): gravar a seguir já leva a localização
            onLocationSelected(point.latitude, point.longitude, "")
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
                title = markerTitle
            )
        }
    }
}
