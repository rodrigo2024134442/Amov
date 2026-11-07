package pt.isec.amov.cityfix.model

import kotlinx.serialization.Serializable

// Valores fixos do enunciado, como enums em vez de String livres:
// o compilador impede valores inválidos e cada valor tem o seu texto traduzido
// em strings.xml (ver ui/components/Labels.kt). O modelo continua sem imports de Android.

/**
 * Natureza da anomalia (enunciado, I.2.E: Dano, Avaria, Destruição ou Roubo).
 */
@Serializable
enum class Situation { DAMAGE, MALFUNCTION, DESTRUCTION, THEFT }

/**
 * Alcance da ocorrência: pública (visível a todos, incluindo anónimos) ou privada.
 */
@Serializable
enum class Scope { PUBLIC, PRIVATE }

/**
 * Estados do ciclo de vida do ticket (enunciado, secção II).
 */
@Serializable
enum class IncidentStatus { OPEN, INFO_REQUESTED, IN_PROGRESS, RESOLVED, CLOSED }

/**
 * Estado de sincronização com a nuvem (slide "FixIt Project Bridge" da aula P08):
 * uma ocorrência criada sem rede fica PENDING_SYNC até ser enviada.
 */
@Serializable
enum class SyncStatus { PENDING_SYNC, SYNCED }