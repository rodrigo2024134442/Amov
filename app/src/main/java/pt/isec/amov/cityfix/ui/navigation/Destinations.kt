package pt.isec.amov.cityfix.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Rota para o ecrã principal de listagem de incidentes.
 */
@Serializable
data object IncidentListRoute

/**
 * Rota para o ecrã de detalhes de um incidente específico.
 */
@Serializable
data class IncidentDetailsRoute(val incidentId: String)

/**
 * Rota para o ecrã de criação ou edição de um incidente.
 * Se incidentId for null, assume modo de criação; caso contrário, modo de edição.
 */
@Serializable
data class EditIncidentRoute(val incidentId: String? = null)
