package pt.isec.amov.cityfix.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Define os diferentes perfis de utilizador suportados pela plataforma CityFix,
 * conforme exigido no enunciado para distinguir permissões e acessos.
 */
@Serializable
enum class UserProfileType {
    ANONYMOUS,
    REGISTERED_CITIZEN,
    OPERATOR
}

/**
 * Representa um utilizador registado ou operador na aplicação CityFix,
 * armazenando dados pessoais, credenciais, georreferenciação e estado de validação por NIF.
 */
@Serializable
data class User(
    // Identificador único do utilizador (gerado automaticamente como UUID)
    val id: String = UUID.randomUUID().toString(),

    // Nome completo do utilizador
    val name: String,

    // Endereço de correio eletrónico para autenticação e contacto
    val email: String,

    // Número de contacto telefónico
    val phone: String,

    // Número de Identificação Fiscal (NIF) único, sujeito a validação camarária
    val nif: String,

    // Coordenadas geográficas associadas ao registo do munícipe
    val latitude: Double,
    val longitude: Double,

    // Tipo de perfil atribuído (Munícipe Registado ou Operador Municipal)
    val profileType: UserProfileType = UserProfileType.REGISTERED_CITIZEN,

    // Indica se o registo foi validado/ativado pelo operador municipal através do NIF
    val isValidated: Boolean = false,

    // Caminho opcional para a fotografia de perfil do utilizador
    val avatarPath: String? = null
)