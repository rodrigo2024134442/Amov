package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.IncidentQuery
import pt.isec.amov.cityfix.data.IncidentsRepository
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentsList
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.utils.FileUtils

/**
 * Separadores da lista do munícipe: as suas ocorrências ou as públicas do município.
 */
enum class ListFilter { MINE, PUBLIC }

/**
 * ViewModel global (associado à MainActivity) com a lista de incidentes,
 * usada pelo ecrã da lista e pelo ecrã de detalhe.
 * Recebe os repositórios pelo construtor (através da IncidentsViewModelFactory).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IncidentsViewModel(
    private val repository: IncidentsRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(ListFilter.MINE)
    val filter: StateFlow<ListFilter> = _filter.asStateFlow()

    fun onFilterChanged(filter: ListFilter) {
        _filter.value = filter
    }

    /**
     * Lista de incidentes, ordenada, sempre atualizada a partir do Firestore.
     * A consulta depende de quem está a ver (anónimo, munícipe ou operador) e do separador:
     * quando a sessão ou o separador mudam, flatMapLatest troca de consulta (e de listener).
     */
    val incidentsList: StateFlow<List<Incident>> =
        combine(authRepository.session, _filter) { session, filter -> queryFor(session, filter) }
            .distinctUntilChanged()
            .flatMapLatest { query -> if (query == null) flowOf(emptyList()) else repository.observeIncidents(query) }
            .map { IncidentsList(it).getAllSorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun queryFor(session: Session, filter: ListFilter): IncidentQuery? = when (session) {
        Session.Guest -> IncidentQuery.PublicActive
        is Session.Active -> when {
            session.user.role == UserRole.OPERATOR -> IncidentQuery.All
            filter == ListFilter.PUBLIC -> IncidentQuery.PublicActive
            else -> IncidentQuery.ByUser(session.user.uid)
        }
        else -> null // a sessão ainda não está completa
    }

    /**
     * Procura e retorna um incidente específico através do seu ID único.
     */
    fun getIncidentById(id: String): Incident? = incidentsList.value.find { it.id == id }

    /**
     * Apaga as fotografias locais, o documento e as imagens na nuvem, e chama onSuccess (para voltar à lista).
     * A lista atualiza-se sozinha (o snapshot listener recebe a remoção).
     */
    fun deleteIncident(incidentId: String, onSuccess: () -> Unit) {
        val incident = getIncidentById(incidentId) ?: return
        incident.mediaPaths.forEach { path -> FileUtils.deleteFileFromPath(path) }
        repository.deleteIncident(incident)
        onSuccess()
    }
}