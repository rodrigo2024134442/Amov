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
        // Depois dos esclarecimentos, os trabalhos começam
        IncidentStatus.INFO_REQUESTED to setOf(IncidentStatus.IN_PROGRESS),
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

    /** true se o autor ainda pode reabrir agora (Parte B usa isto no botão "Reabrir"). */
    fun canReopen(incident: Incident, now: Long = System.currentTimeMillis()): Boolean =
        closesAt(incident)?.let { now < it } == true
}
