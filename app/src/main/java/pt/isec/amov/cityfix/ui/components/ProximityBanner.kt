package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R

/**
 * Aviso ao operador de que os alertas de proximidade (geofence de 20 m) precisam da localização,
 * também em segundo plano ("Permitir sempre"). Stateless: a MainActivity pede as permissões.
 */
@Composable
fun ProximityBanner(
    needsForeground: Boolean,
    onAllow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.LocationSearching, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.proximity_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(if (needsForeground) R.string.proximity_text_foreground else R.string.proximity_text_background),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onAllow) { Text(stringResource(R.string.action_allow_location)) }
        }
    }
}

/**
 * Sugestão da deteção de impactos (enunciado I.2.F): abrir de imediato uma ocorrência de anomalia
 * no pavimento. Stateless.
 */
@Composable
fun ImpactDialog(onReport: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text(stringResource(R.string.impact_title)) },
        text = { Text(stringResource(R.string.impact_text)) },
        confirmButton = { TextButton(onClick = onReport) { Text(stringResource(R.string.action_report_now)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_not_now)) } }
    )
}
