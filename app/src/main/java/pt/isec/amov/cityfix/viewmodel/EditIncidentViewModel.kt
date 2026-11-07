package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.CategoriesRepository
import pt.isec.amov.cityfix.data.IncidentsRepository
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.SyncStatus
import pt.isec.amov.cityfix.utils.FileUtils
import java.util.UUID

/**
 * ViewModel do formulário de criar/editar incidente, associado ao ecrã (NavBackStackEntry):
 * sobrevive à rotação e é destruído quando se sai do ecrã (Back ou depois de gravar).
 */
class EditIncidentViewModel(
    private val repository: IncidentsRepository,
    categoriesRepository: CategoriesRepository,
    private val authRepository: AuthRepository,
    private val incidentId: String?
) : ViewModel() {

    private val _editState = MutableStateFlow(EditIncidentUiState())
    val editState: StateFlow<EditIncidentUiState> = _editState.asStateFlow()

    // Categorias para o menu de escolha: só as ativas, lidas do Firestore
    val categories: StateFlow<List<Category>> = categoriesRepository.observeActiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ao editar, guarda-se o incidente original para manter o que o formulário não mostra
    // (autor, estado, datas, coordenadas...)
    private var original: Incident? = null

    // Passa a true ao gravar; se o ecrã fechar sem gravar, onCleared apaga as fotografias novas
    private var saved = false

    init {
        if (incidentId != null) {
            repository.getIncidentById(incidentId)?.let { i ->
                original = i
                _editState.value = EditIncidentUiState(
                    incidentId = i.id,
                    title = i.title,
                    description = i.description,
                    category = i.category,
                    situation = i.situation,
                    scope = i.scope,
                    mediaPaths = i.mediaPaths
                )
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _editState.update {
            it.copy(
                title = newTitle,
                titleError = if (newTitle.isBlank()) R.string.error_title_empty else null
            )
        }
    }

    fun onDescriptionChanged(newDescription: String) {
        _editState.update {
            it.copy(
                description = newDescription,
                descriptionError = if (newDescription.trim().length < 10) R.string.error_description_short else null
            )
        }
    }

    fun onCategoryChanged(newCategory: String) {
        _editState.update { it.copy(category = newCategory) }
    }

    fun onSituationChanged(newSituation: Situation) {
        _editState.update { it.copy(situation = newSituation) }
    }

    fun onScopeChanged(newScope: Scope) {
        _editState.update { it.copy(scope = newScope) }
    }

    /**
     * Acrescenta as fotografias já copiadas para a pasta privada (da galeria ou da câmara),
     * sem passar o limite de MAX_MEDIA.
     */
    fun onMediaAttached(newPaths: List<String>) {
        _editState.update {
            it.copy(mediaPaths = (it.mediaPaths + newPaths).take(EditIncidentUiState.MAX_MEDIA))
        }
    }

    /**
     * Retira uma fotografia do formulário. O ficheiro só é apagado ao gravar
     * (se o utilizador sair sem gravar, o incidente original continua a precisar dele).
     */
    fun onMediaRemoved(path: String) {
        _editState.update { it.copy(mediaPaths = it.mediaPaths - path) }
    }

    /**
     * Valida, cria o Incident, grava-o no Firestore, agenda o envio das imagens e chama onSuccess.
     */
    fun saveIncident(onSuccess: () -> Unit) {
        val current = _editState.value
        if (!current.isValid) return
        _editState.update { it.copy(isSaving = true) }
        val base = original
        // Apaga do disco as fotografias que o utilizador retirou (evita ficheiros órfãos, desafio da aula P09)
        base?.mediaPaths
            ?.filter { it !in current.mediaPaths }
            ?.forEach { FileUtils.deleteFileFromPath(it) }

        // As imagens mudaram (ou o incidente é novo)? Então as da nuvem deixam de servir:
        // apagam-se, e o incidente volta a PENDING_SYNC até o MediaUploadWorker enviar as novas.
        val mediaChanged = base == null || base.mediaPaths != current.mediaPaths
        if (mediaChanged && base != null) repository.deleteRemoteMedia(base)
        // O que se mantém da nuvem: só se as imagens não mudaram
        val kept = base?.takeUnless { mediaChanged }

        val incident = Incident(
            id = current.incidentId ?: UUID.randomUUID().toString(),
            // O autor é o utilizador com sessão iniciada
            userId = base?.userId ?: authRepository.currentUid.orEmpty(),
            title = current.title.trim(),
            description = current.description.trim(),
            category = current.category,
            situation = current.situation!!,
            scope = current.scope,
            latitude = base?.latitude,
            longitude = base?.longitude,
            mediaPaths = current.mediaPaths,
            mediaUrls = kept?.mediaUrls ?: emptyList(),
            thumbnailUrl = kept?.thumbnailUrl,
            status = base?.status ?: IncidentStatus.OPEN,
            timestamp = base?.timestamp ?: System.currentTimeMillis(),
            resolvedAt = base?.resolvedAt,
            syncStatus = kept?.syncStatus ?: SyncStatus.PENDING_SYNC
        )
        repository.saveIncident(incident)
        saved = true
        if (mediaChanged) repository.scheduleMediaUpload(incident.id)
        _editState.update { it.copy(isSaving = false) }
        onSuccess()
    }

    /**
     * Chamado quando o ViewModel é destruído (ao sair do ecrã de edição).
     * Se o utilizador saiu sem gravar, as fotografias tiradas/escolhidas nesta edição
     * já foram copiadas para a pasta privada: apagam-se para não ficarem órfãs.
     */
    override fun onCleared() {
        super.onCleared()
        if (!saved) {
            val originalPaths = original?.mediaPaths.orEmpty()
            _editState.value.mediaPaths
                .filter { it !in originalPaths }
                .forEach { FileUtils.deleteFileFromPath(it) }
        }
    }
}