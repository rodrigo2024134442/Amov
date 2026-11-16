package pt.isec.amov.cityfix.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Rota do dashboard (indicadores do município e ocorrências públicas recentes).
 * É o ecrã inicial do munícipe anónimo.
 */
@Serializable
data object DashboardRoute

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

/**
 * Rotas da autenticação e do perfil.
 */
@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

@Serializable
data object ProfileRoute

/**
 * Rotas do operador: validar munícipes e gerir categorias.
 */
@Serializable
data object ValidateUsersRoute

@Serializable
data object CategoriesRoute