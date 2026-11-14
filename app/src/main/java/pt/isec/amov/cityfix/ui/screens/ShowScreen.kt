package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.ui.components.IncidentPhotoGallery
import pt.isec.amov.cityfix.ui.components.label

/**
 * Ecrã stateless que apresenta os detalhes completos de um incidente selecionado.
 * Inclui botões flutuantes para editar e apagar (com confirmação) a ocorrência, só para o autor.
 * As coordenadas e o mapa Google Maps só aparecem ao autor e ao operador (Ficha 10).
 */
@Composable
fun ShowScreen(
    incident: Incident?,
    canEdit: Boolean,
    showLocation: Boolean,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (incident == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.incident_not_found))
        }
        return
    }

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteClick(incident.id)
                }) { Text(stringResource(R.string.delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (canEdit) Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallFloatingActionButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete_incident)
                    )
                }
                FloatingActionButton(onClick = { onEditClick(incident.id) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_edit_incident)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (incident.displayMedia.isNotEmpty()) {
                IncidentPhotoGallery(photos = incident.displayMedia)
            }
            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.label_category, incident.category),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = incident.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.label_status, incident.status.label()),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.label_situation, incident.situation.label()),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.label_scope, incident.scope.label()),
                style = MaterialTheme.typography.bodyMedium
            )
            if (showLocation) {
                incident.address?.let { addr ->
                    Text(
                        text = "Morada: $addr",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (incident.latitude != null && incident.longitude != null) {
                    val incidentLatLng = LatLng(incident.latitude, incident.longitude)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(incidentLatLng, 15f)
                            }
                        ) {
                            val markerState = remember(incidentLatLng) {
                                MarkerState(position = incidentLatLng)
                            }
                            Marker(
                                state = markerState,
                                title = incident.title
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_coordinates),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
