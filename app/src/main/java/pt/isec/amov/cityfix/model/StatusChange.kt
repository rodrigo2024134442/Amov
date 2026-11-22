package pt.isec.amov.cityfix.model

import kotlinx.serialization.Serializable

/**
 * Uma mudança de estado do ticket (enunciado I.3.E: "transição de estados (com timestamp)").
 * A lista destas entradas é o histórico completo da ocorrência.
 */
@Serializable
data class StatusChange(
    // Estado para onde passou
    val status: IncidentStatus,

    // Quando (Unix Epoch em milissegundos)
    val timestamp: Long,

    // Quem fez a mudança (uid), ou SYSTEM no fecho automático
    val byUid: String,

    // Nota opcional (ex.: o pedido de dados, a justificação da reabertura)
    val note: String? = null
) {
    companion object {
        // Autor das mudanças automáticas (fecho ao fim de 5 dias)
        const val SYSTEM = "system"
    }
}

/**
 * Regras do ciclo de vida do ticket (enunciado, secção II), num só sítio:
 * que transições são válidas e os prazos de reabertura e fecho.
 *
 *   Aberto ──► Pedido de Informações ──► Em Resolução ──► Resolvido ──► Encerrado
 *     │                ▲                    │   ▲             │
 *     └────────────────┼────────────────────┘   │             └──► Aberto (reabertura, até 5 dias)
 *                      └────────────────────────┘
 */
object IncidentLifecycle {

    // 5 dias depois de resolvida: deixa de poder ser reaberta e passa a Encerrado
    const val CLOSE_AFTER_MS = 5L * 24 * 60 * 60 * 1000

    private val allowed: Map<IncidentStatus, Set<IncidentStatus>> = mapOf(
        // O operador pede dados ou começa logo os trabalhos; o munícipe pode pedir esclarecimentos
        IncidentStatus.OPEN to setOf(IncidentStatus.INFO_REQUESTED, IncidentStatus.IN_PROGRESS),
        // Depois dos esclarecimentos, volta ao estado anterior (Aberto) ou os trabalhos começam
        IncidentStatus.INFO_REQUESTED to setOf(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS),
        // Durante a obra ainda se podem pedir dados; ou fica resolvido (com prova)
        IncidentStatus.IN_PROGRESS to setOf(IncidentStatus.INFO_REQUESTED, IncidentStatus.RESOLVED),
        // O autor reabre (até 5 dias) ou fecha automaticamente
        IncidentStatus.RESOLVED to setOf(IncidentStatus.OPEN, IncidentStatus.CLOSED),
        // Arquivado e bloqueado: não admite mais alterações
        IncidentStatus.CLOSED to emptySet()
    )

    fun canTransition(from: IncidentStatus, to: IncidentStatus): Boolean =
        to in allowed[from].orEmpty()

    /** Até quando a ocorrência resolvida pode ser reaberta (e quando fecha). null se não está resolvida. */
    fun closesAt(incident: Incident): Long? =
        incident.resolvedAt?.takeIf { incident.status == IncidentStatus.RESOLVED }?.plus(CLOSE_AFTER_MS)

    /**
     * Quem fez o pedido de informações em curso (uid), ou null se a ocorrência não está em
     * Pedido de Informações. Pode ser um operador (pedido de dados) ou o autor (pedido de esclarecimento).
     */
    fun infoRequester(incident: Incident): String? =
        if (incident.status != IncidentStatus.INFO_REQUESTED) null
        else incident.timeline.lastOrNull { it.status == IncidentStatus.INFO_REQUESTED }?.byUid

    /**
     * Estado em que a ocorrência estava antes do pedido de informações em curso
     * (para onde volta quando a outra parte responde). Aberto, se não se souber.
     */
    fun statusBeforeInfoRequest(incident: Incident): IncidentStatus {
        val timeline = incident.timeline
        val index = timeline.indexOfLast { it.status == IncidentStatus.INFO_REQUESTED }
        val previous = if (index > 0) timeline[index - 1].status else IncidentStatus.OPEN
        return if (previous == IncidentStatus.IN_PROGRESS) IncidentStatus.IN_PROGRESS else IncidentStatus.OPEN
    }

    /**
     * true se a mensagem de [senderUid] responde ao pedido de informações em curso:
     * o pedido foi do autor e responde um operador, ou foi de um operador e responde o autor.
     */
    fun isAnswerToInfoRequest(incident: Incident, senderUid: String, senderIsOperator: Boolean): Boolean {
        val requester = infoRequester(incident) ?: return false
        val requestedByAuthor = requester == incident.userId
        val senderIsAuthor = senderUid == incident.userId
        return if (requestedByAuthor) senderIsOperator else senderIsAuthor
    }

    /** true se o autor pode pedir esclarecimentos agora (enunciado II.2). */
    fun canAskClarification(incident: Incident): Boolean =
        canTransition(incident.status, IncidentStatus.INFO_REQUESTED)

    /** true se o autor ainda pode reabrir agora (Parte B usa isto no botão "Reabrir"). */
    fun canReopen(incident: Incident, now: Long = System.currentTimeMillis()): Boolean =
        closesAt(incident)?.let { now < it } == true
}
