package pt.isec.amov.cityfix.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Incident

/**
 * Componente stateless que representa um cartão de incidente individual.
 * Suporta expansão animada para mostrar mais detalhes (descrição, situação, alcance).
 */
@Composable
fun IncidentCard(
    incident: Incident,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Miniatura: a primeira fotografia da ocorrência
                IncidentThumbnail(picturePath = incident.displayThumbnail)
                Column {
                    Text(
                        text = incident.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    // O enunciado (I.1.B) pede categoria, situação e miniatura na lista
                    Text(
                        text = "${incident.category} · ${incident.situation.label()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = incident.status.label(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Ocorrência de grupo: vários munícipes acompanham o mesmo problema
                    if (incident.isGroup) {
                        Text(
                            text = stringResource(R.string.label_group, incident.groupSize),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = incident.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.label_scope, incident.scope.label()),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}