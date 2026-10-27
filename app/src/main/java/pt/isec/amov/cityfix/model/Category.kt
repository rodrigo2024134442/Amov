package pt.isec.amov.cityfix.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Representa uma tipologia ou categoria de problema gerida pela autarquia
 * (Ex: "Vias Públicas", "Água", "Iluminação", "Saneamento").
 */
@Serializable
data class Category(
    // Identificador único da categoria
    val id: String = UUID.randomUUID().toString(),

    // Designação da tipologia do problema
    val name: String,

    // Descrição opcional detalhada da categoria
    val description: String = "",

    // Estado de ativação (permite ao operador municipal ativar ou desativar categorias)
    val isActive: Boolean = true
)