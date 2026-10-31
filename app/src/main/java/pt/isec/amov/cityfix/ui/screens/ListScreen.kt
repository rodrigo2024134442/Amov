package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.ui.components.IncidentCard

/**
 * Ecrã stateless que apresenta a lista virtualizada de incidentes (LazyColumn)
 * ou uma mensagem informativa caso a lista esteja vazia.
 */
@Composable
fun ListScreen(
    incidents: List<Incident>,
    isExpanded: Boolean,
    onIncidentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (incidents.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Sem incidentes registados.")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = incidents, key = { it.id }) { incident ->
                IncidentCard(
                    incident = incident,
                    isExpanded = isExpanded,
                    onClick = { onIncidentClick(incident.id) }
                )
            }
        }
    }
}
