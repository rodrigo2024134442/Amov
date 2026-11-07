package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.ui.components.IncidentCard
import pt.isec.amov.cityfix.viewmodel.ListFilter

/**
 * Ecrã stateless que apresenta a lista virtualizada de incidentes (LazyColumn)
 * ou uma mensagem informativa caso a lista esteja vazia.
 * Com filter != null (munícipe), mostra no topo os separadores "As minhas" e "Públicas".
 */
@Composable
fun ListScreen(
    incidents: List<Incident>,
    isExpanded: Boolean,
    onIncidentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    filter: ListFilter? = null,
    onFilterChange: (ListFilter) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (filter != null) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == ListFilter.MINE,
                    onClick = { onFilterChange(ListFilter.MINE) },
                    label = { Text(stringResource(R.string.filter_mine)) }
                )
                FilterChip(
                    selected = filter == ListFilter.PUBLIC,
                    onClick = { onFilterChange(ListFilter.PUBLIC) },
                    label = { Text(stringResource(R.string.filter_public)) }
                )
            }
        }
        if (incidents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.no_incidents))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
}