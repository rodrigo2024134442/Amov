package pt.isec.amov.cityfix.utils

import pt.isec.amov.cityfix.AppConfig

/**
 * Validações dos formulários de registo e login (enunciado I.2.A e I.3.A:
 * "Os dados devem ser validados (formato de nº telefone, e-mail, etc.)").
 */
object Validators {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    // E-mail do domínio da autarquia: identifica os operadores
    fun isOperatorEmail(email: String): Boolean =
        email.trim().lowercase().endsWith("@" + AppConfig.OPERATOR_EMAIL_DOMAIN)

    // Telefone português: 9 dígitos, a começar por 2 (fixo) ou 9 (móvel)
    fun isValidPhone(phone: String): Boolean = Regex("^[29][0-9]{8}$").matches(phone.trim())

    // O Firebase Authentication exige pelo menos 6 caracteres
    fun isValidPassword(password: String): Boolean = password.length >= 6

    // NIF: 9 dígitos (verificação simples, como indicado pelo professor)
    fun isValidNif(nif: String): Boolean = Regex("^[0-9]{9}$").matches(nif.trim())
}