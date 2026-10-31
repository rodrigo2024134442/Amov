package pt.isec.amov.cityfix.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.model.Incident

/**
 * Componente stateless que representa um cartão de incidente individual.
 * Suporta expansão animada para mostrar mais detalhes (descrição, categoria, estado).
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
            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Categoria: ${incident.category} | Estado: ${incident.status}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Descrição: ${incident.description}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Situação: ${incident.situation} (${incident.scope})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
