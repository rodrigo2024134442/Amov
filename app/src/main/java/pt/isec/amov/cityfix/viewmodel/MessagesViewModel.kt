package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.IncidentsRepository
import pt.isec.amov.cityfix.data.MessagesRepository
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentLifecycle
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.UserRole

/**
 * ViewModel da Parte B para UMA ocorrência (ecrã de detalhe), associado à rota do detalhe,
 * tal como o TicketViewModel: mensagens trocadas com a câmara (enunciado I.2.E e II.2)
 * e reabertura pelo autor até 5 dias depois de resolvida (enunciado I.2.E e II.4).
 *
 * O texto a escrever, o diálogo "Reabrir" e a justificação ficam no SavedStateHandle:
 * sobrevivem à rotação e à recriação da app.
 */
class MessagesViewModel(
    private val savedState: SavedStateHandle,
    incidentId: String,
    private val messagesRepository: MessagesRepository,
    private val incidentsRepository: IncidentsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    /**
     * Mensagens da ocorrência em tempo real (mais antiga primeiro).
     */
    val messages: StateFlow<List<Message>> = messagesRepository.observeMessages(incidentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Para pôr à direita as mensagens do próprio utilizador
    val currentUid: String? = authRepository.currentUid

    // Texto da mensagem a escrever
    val draft: StateFlow<String> = savedState.getStateFlow(KEY_DRAFT, "")

    // Diálogo "Reabrir", justificação e erro (ID de strings.xml)
    val reopenDialogOpen: StateFlow<Boolean> = savedState.getStateFlow(KEY_REOPEN_OPEN, false)
    val reopenReason: StateFlow<String> = savedState.getStateFlow(KEY_REOPEN_REASON, "")
    val reopenError: StateFlow<Int?> = savedState.getStateFlow(KEY_REOPEN_ERROR, null)

    // Diálogo "Pedir esclarecimento" do munícipe (enunciado II.2) e o texto da pergunta
    val clarifyDialogOpen: StateFlow<Boolean> = savedState.getStateFlow(KEY_CLARIFY_OPEN, false)
    val clarifyText: StateFlow<String> = savedState.getStateFlow(KEY_CLARIFY_TEXT, "")

    fun openClarifyDialog() {
        savedState[KEY_CLARIFY_OPEN] = true
    }

    fun onClarifyTextChange(text: String) {
        savedState[KEY_CLARIFY_TEXT] = text
    }

    fun dismissClarify() {
        savedState[KEY_CLARIFY_OPEN] = false
        savedState[KEY_CLARIFY_TEXT] = ""
    }

    /**
     * O munícipe pede esclarecimentos (estimativa temporal, procedimento...): a ocorrência passa a
     * Pedido de Informações, com a pergunta no histórico, e a pergunta segue como mensagem.
     * A câmara é notificada (o documento muda de estado e recebe a mensagem).
     */
    fun requestClarification(incident: Incident) {
        val question = clarifyText.value.trim()
        if (question.isEmpty()) return
        if (incidentsRepository.changeStatus(incident, IncidentStatus.INFO_REQUESTED, question)) {
            messagesRepository.send(incident.id, question)
        }
        dismissClarify()
    }

    fun onDraftChange(text: String) {
        savedState[KEY_DRAFT] = text
    }

    /**
     * Envia o texto escrito. Numa ocorrência encerrada não envia (as regras também recusam).
     * Se a mensagem responde ao pedido de informações em curso, a ocorrência volta ao estado anterior.
     */
    fun send(incident: Incident) {
        val text = draft.value.trim()
        if (text.isEmpty() || incident.status == IncidentStatus.CLOSED) return
        messagesRepository.send(incident.id, text)
        savedState[KEY_DRAFT] = ""
        // Resposta ao pedido de informações em curso (do operador ao autor, ou do autor ao operador):
        // o ticket volta ao estado em que estava antes do pedido, com a resposta no histórico
        val user = (authRepository.session.value as? Session.Active)?.user ?: return
        if (IncidentLifecycle.isAnswerToInfoRequest(incident, user.uid, user.role == UserRole.OPERATOR)) {
            incidentsRepository.changeStatus(incident, IncidentLifecycle.statusBeforeInfoRequest(incident), text)
        }
    }

    fun openReopenDialog() {
        savedState[KEY_REOPEN_ERROR] = null
        savedState[KEY_REOPEN_OPEN] = true
    }

    fun onReopenReasonChange(text: String) {
        savedState[KEY_REOPEN_REASON] = text
        savedState[KEY_REOPEN_ERROR] = null
    }

    fun dismissReopen() {
        savedState[KEY_REOPEN_OPEN] = false
        savedState[KEY_REOPEN_REASON] = ""
        savedState[KEY_REOPEN_ERROR] = null
    }

    /**
     * "Reabrir": volta a Aberto com a justificação (obrigatória) no histórico e envia-a como
     * mensagem. O changeStatus trata das estatísticas e limpa o resolvedAt; devolve false se já
     * passaram os 5 dias.
     */
    fun reopen(incident: Incident) {
        val reason = reopenReason.value.trim()
        when {
            reason.isEmpty() -> savedState[KEY_REOPEN_ERROR] = R.string.reopen_reason_required
            !IncidentLifecycle.canReopen(incident) ||
                !incidentsRepository.changeStatus(incident, IncidentStatus.OPEN, reason) ->
                savedState[KEY_REOPEN_ERROR] = R.string.reopen_expired
            else -> {
                messagesRepository.send(incident.id, reason)
                dismissReopen()
            }
        }
    }

    companion object {
        private const val KEY_DRAFT = "messageDraft"
        private const val KEY_REOPEN_OPEN = "reopenOpen"
        private const val KEY_REOPEN_REASON = "reopenReason"
        private const val KEY_REOPEN_ERROR = "reopenError"
        private const val KEY_CLARIFY_OPEN = "clarifyOpen"
        private const val KEY_CLARIFY_TEXT = "clarifyText"
    }
}

/**
 * Fábrica do MessagesViewModel, como a TicketViewModelFactory (CreationExtras + SavedStateHandle).
 */
class MessagesViewModelFactory(
    private val incidentId: String,
    private val messagesRepository: MessagesRepository,
    private val incidentsRepository: IncidentsRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        MessagesViewModel(
            extras.createSavedStateHandle(), incidentId, messagesRepository, incidentsRepository, authRepository
        ) as T
}
