package pt.isec.amov.cityfix.model

import kotlin.math.roundToInt

/**
 * Indicadores do dashboard do munícipe anónimo (enunciado I.1.A):
 * tempo médio de reparação, nº total de ocorrências e % de ocorrências ativas.
 *
 * Vêm do documento stats/global do Firestore (contadores atualizados com FieldValue.increment),
 * porque o anónimo só pode ler as ocorrências públicas e o "total" inclui também as privadas.
 */
data class MunicipalStats(
    // Todas as ocorrências (públicas e privadas)
    val total: Long = 0,

    // Ocorrências ainda por resolver (Aberto, Pedido de Informações, Em Resolução)
    val active: Long = 0,

    // Ocorrências já resolvidas (Resolvido ou Encerrado)
    val resolvedCount: Long = 0,

    // Soma dos tempos de reparação das resolvidas (resolvedAt - timestamp), em milissegundos
    val repairMillisSum: Long = 0
) {
    // Tempo médio de reparação em dias (null se ainda não há nenhuma resolvida)
    val averageRepairDays: Double?
        get() = if (resolvedCount > 0) repairMillisSum.coerceAtLeast(0).toDouble() / resolvedCount / MILLIS_PER_DAY else null

    // Percentagem de ativas, de 0 a 100 (null se ainda não há ocorrências)
    val activePercent: Int?
        get() = if (total > 0) (active.coerceIn(0, total) * 100.0 / total).roundToInt() else null

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000.0
    }
}
