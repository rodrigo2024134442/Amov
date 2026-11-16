package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.MunicipalStats
import pt.isec.amov.cityfix.ui.components.IncidentCard

/**
 * Dashboard do município (enunciado I.1.A): tempo médio de reparação, nº total de ocorrências
 * e % de ativas, seguido das ocorrências públicas ativas mais recentes.
 * Stateless: recebe os dados do DashboardViewModel.
 */
@Composable
fun DashboardScreen(
    stats: MunicipalStats?,
    recentPublic: List<Incident>,
    onIncidentClick: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.kpi_unavailable)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Área de serviço do anónimo
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(R.string.dashboard_area),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Os três indicadores, lado a lado e com a mesma altura
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiCard(
                    icon = Icons.Default.Timer,
                    value = stats?.averageRepairDays?.let { stringResource(R.string.kpi_days, it) } ?: none,
                    label = stringResource(R.string.kpi_avg_repair),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                KpiCard(
                    icon = Icons.Default.Analytics,
                    value = stats?.total?.toString() ?: none,
                    label = stringResource(R.string.kpi_total),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                KpiCard(
                    icon = Icons.Default.PendingActions,
                    value = stats?.activePercent?.let { stringResource(R.string.kpi_percent, it) } ?: none,
                    label = stringResource(R.string.kpi_active),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        // Pré-visualização das ocorrências públicas ativas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.dashboard_public_title), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onSeeAll) { Text(stringResource(R.string.action_see_all)) }
            }
        }
        if (recentPublic.isEmpty()) {
            item { Text(stringResource(R.string.no_incidents), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(recentPublic, key = { it.id }) { incident ->
                IncidentCard(incident = incident, isExpanded = false, onClick = { onIncidentClick(incident.id) })
            }
        }

        // Aviso de privacidade
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.dashboard_privacy), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Cartão de um indicador. mergeDescendants: o leitor de ecrã lê valor e legenda de uma vez.
 */
@Composable
private fun KpiCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
