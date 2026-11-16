package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pt.isec.amov.cityfix.data.IncidentQuery
import pt.isec.amov.cityfix.data.IncidentsRepository
import pt.isec.amov.cityfix.data.StatsRepository
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.MunicipalStats

/**
 * ViewModel do dashboard (enunciado I.1.A), associado à MainActivity.
 * Junta os indicadores (stats/global) e as ocorrências públicas ativas mais recentes.
 * O dashboard é o mesmo para o anónimo e para quem tem sessão (I.2.D: o munícipe registado
 * tem acesso às funcionalidades do anónimo).
 */
class DashboardViewModel(
    statsRepository: StatsRepository,
    incidentsRepository: IncidentsRepository
) : ViewModel() {

    // null enquanto o primeiro valor não chega (o ecrã mostra "—")
    val stats: StateFlow<MunicipalStats?> = statsRepository.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Pré-visualização: as mais recentes; a lista completa está no separador "Ocorrências"
    val recentPublic: StateFlow<List<Incident>> = incidentsRepository.observeIncidents(IncidentQuery.PublicActive)
        .map { list -> list.sortedByDescending { it.timestamp }.take(PREVIEW_SIZE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        const val PREVIEW_SIZE = 3
    }
}
