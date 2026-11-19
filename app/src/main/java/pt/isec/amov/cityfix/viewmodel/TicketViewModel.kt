package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.IncidentsRepository
import pt.isec.amov.cityfix.data.MessagesRepository
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.utils.FileUtils

/**
 * Contactos do autor no painel do operador: ainda a ler, sem perfil (ex.: ocorrências de teste
 * criadas antes de haver contas, com o autor "user_demo_1") ou o perfil encontrado.
 */
sealed interface AuthorInfo {
    data object Loading : AuthorInfo
    data object Unknown : AuthorInfo
    data class Known(val user: User) : AuthorInfo
}

/**
 * ViewModel do ciclo de vida de UMA ocorrência (ecrã de detalhe), associado à rota do detalhe:
 * é criado ao abrir o detalhe e destruído ao sair.
 * Ações do operador (enunciado I.3.E): pedir dados, iniciar trabalhos, resolver com prova.
 * Também dá os contactos do autor (só ao operador).
 *
 * O diálogo "Resolver" (aberto ou não), a fotografia de prova e a nota ficam no SavedStateHandle:
 * ao abrir a câmara, o Android pode destruir a app em segundo plano; quando volta, o diálogo
 * reaparece logo com a fotografia (antes desaparecia e só voltava uns segundos depois).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TicketViewModel(
    private val savedState: SavedStateHandle,
    private val incidentId: String,
    private val incidentsRepository: IncidentsRepository,
    private val messagesRepository: MessagesRepository,
    authRepository: AuthRepository
) : ViewModel() {

    /**
     * Perfil do autor (nome, telefone, e-mail) para o "Painel de Gestão Operacional".
     * Só o operador o lê (as regras não deixam ler perfis alheios); para os outros fica Unknown.
     */
    val author: StateFlow<AuthorInfo> = authRepository.session
        .map { (it as? Session.Active)?.user?.role == UserRole.OPERATOR }
        .distinctUntilChanged()
        .flatMapLatest { isOperator ->
            val authorUid = incidentsRepository.getIncidentById(incidentId)?.userId
            if (isOperator && !authorUid.isNullOrEmpty()) {
                authRepository.observeUserProfile(authorUid)
                    .map { user -> if (user != null) AuthorInfo.Known(user) else AuthorInfo.Unknown }
                    .onStart { emit(AuthorInfo.Loading) }
            } else flowOf(AuthorInfo.Unknown)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthorInfo.Loading)

    // Diálogo "Resolver" aberto, fotografia de prova ainda não gravada e nota (sobrevivem à recriação)
    val resolveDialogOpen: StateFlow<Boolean> = savedState.getStateFlow(KEY_RESOLVE_OPEN, false)
    val proofPath: StateFlow<String?> = savedState.getStateFlow(KEY_PROOF_PATH, null)
    val resolveNote: StateFlow<String> = savedState.getStateFlow(KEY_NOTE, "")

    /**
     * "Pedir dados": muda para Pedido de Informações (com a pergunta no histórico) e envia a
     * pergunta como mensagem ao munícipe (função partilhada da Parte B).
     */
    fun requestInfo(incident: Incident, question: String) {
        if (question.isBlank()) return
        if (incidentsRepository.changeStatus(incident, IncidentStatus.INFO_REQUESTED, question)) {
            messagesRepository.send(incident.id, question)
        }
    }

    /** "Iniciar trabalhos": passa a Em Resolução. */
    fun startWork(incident: Incident) {
        incidentsRepository.changeStatus(incident, IncidentStatus.IN_PROGRESS)
    }

    /** Abre o diálogo "Resolver". */
    fun openResolveDialog() {
        savedState[KEY_RESOLVE_OPEN] = true
    }

    fun onResolveNoteChange(note: String) {
        savedState[KEY_NOTE] = note
    }

    /** Fotografia escolhida (galeria ou câmara) no diálogo "Resolver". Apaga a anterior, se havia. */
    fun onProofPicked(path: String) {
        proofPath.value?.takeIf { it != path }?.let { FileUtils.deleteFileFromPath(it) }
        savedState[KEY_PROOF_PATH] = path
    }

    /** A câmara vai abrir: guarda onde vai escrever a fotografia (para a encontrar ao voltar). */
    fun onCameraLaunched(path: String) {
        savedState[KEY_CAMERA_PATH] = path
    }

    /** Volta da câmara: com fotografia passa a ser a prova; cancelada, apaga o ficheiro vazio. */
    fun onCameraResult(success: Boolean) {
        val path = savedState.get<String>(KEY_CAMERA_PATH) ?: return
        savedState[KEY_CAMERA_PATH] = null
        if (success) onProofPicked(path) else FileUtils.deleteFileFromPath(path)
    }

    /** Diálogo "Resolver" cancelado: fecha-o e a fotografia escolhida deixa de servir. */
    fun discardProof() {
        proofPath.value?.let { FileUtils.deleteFileFromPath(it) }
        savedState[KEY_PROOF_PATH] = null
        savedState[KEY_NOTE] = ""
        savedState[KEY_RESOLVE_OPEN] = false
    }

    /** "Resolver": passa a Resolvido com a fotografia (obrigatória) e a nota opcional. */
    fun resolve(incident: Incident) {
        val path = proofPath.value ?: return
        if (incidentsRepository.resolve(incident, path, resolveNote.value)) {
            // A fotografia agora pertence à ocorrência (o ProofUploadWorker envia-a): não se apaga
            savedState[KEY_PROOF_PATH] = null
            savedState[KEY_NOTE] = ""
            savedState[KEY_RESOLVE_OPEN] = false
        }
    }

    // Sair do detalhe com uma fotografia escolhida mas não usada: apaga-a (sem ficheiros órfãos)
    override fun onCleared() {
        proofPath.value?.let { FileUtils.deleteFileFromPath(it) }
        super.onCleared()
    }

    companion object {
        private const val KEY_RESOLVE_OPEN = "resolveOpen"
        private const val KEY_PROOF_PATH = "proofPath"
        private const val KEY_CAMERA_PATH = "cameraPath"
        private const val KEY_NOTE = "resolveNote"
    }
}

/**
 * Fábrica do TicketViewModel (como as da aula P08), com o ID da ocorrência.
 * Usa as CreationExtras para criar o SavedStateHandle da rota do detalhe.
 */
class TicketViewModelFactory(
    private val incidentId: String,
    private val incidentsRepository: IncidentsRepository,
    private val messagesRepository: MessagesRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        TicketViewModel(
            extras.createSavedStateHandle(), incidentId, incidentsRepository, messagesRepository, authRepository
        ) as T
}
