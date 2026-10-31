package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.model.Incident

/**
 * Ecrã stateless que apresenta os detalhes completos de um incidente selecionado.
 * Inclui um FloatingActionButton para editar a ocorrência.
 */
@Composable
fun ShowScreen(
    incident: Incident?,
    onEditClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (incident == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Incidente não encontrado.")
        }
        return
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { onEditClick(incident.id) }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar Incidente")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Categoria: ${incident.category}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Descrição: ${incident.description}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Estado: ${incident.status}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Situação: ${incident.situation} | Âmbito: ${incident.scope}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Coordenadas: (${incident.latitude}, ${incident.longitude})",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
