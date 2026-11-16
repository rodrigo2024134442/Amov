package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.WrongLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.viewmodel.AreaState

/**
 * Mostrado ao munícipe anónimo em vez do dashboard, da lista e do detalhe enquanto
 * não se confirmar que está a menos de 10 km da Praça da República (enunciado I.1).
 * Stateless: o estado vem do GuestAreaViewModel; as ações voltam pelos callbacks.
 */
@Composable
fun GuestAreaScreen(
    state: AreaState,
    onAllowLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            AreaState.Unknown, AreaState.Checking, AreaState.Inside -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.area_checking), textAlign = TextAlign.Center)
            }
            AreaState.PermissionDenied -> {
                Message(Icons.Default.LocationOff, R.string.area_permission_title, stringResource(R.string.area_permission_text))
                Button(onClick = onAllowLocation, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_allow_location))
                }
                // Depois de recusar duas vezes o Android já não mostra o pedido: só pelas definições
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_open_settings))
                }
            }
            is AreaState.Outside -> {
                Message(
                    Icons.Default.WrongLocation,
                    R.string.area_outside_title,
                    stringResource(R.string.area_outside_text, state.distanceKm)
                )
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            AreaState.LocationUnavailable -> {
                Message(Icons.Default.LocationDisabled, R.string.area_unavailable_title, stringResource(R.string.area_unavailable_text))
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }

        // Com sessão iniciada a app funciona em qualquer lugar (enunciado I.2 e I.3)
        if (state !is AreaState.Checking && state !is AreaState.Unknown && state !is AreaState.Inside) {
            HorizontalDivider()
            Text(
                stringResource(R.string.area_sign_in_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_login))
            }
        }
    }
}

@Composable
private fun Message(icon: ImageVector, title: Int, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
    Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(text, textAlign = TextAlign.Center)
}
