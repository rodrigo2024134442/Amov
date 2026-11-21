package pt.isec.amov.cityfix

/**
 * Valores de configuração da app, num só sítio.
 */
object AppConfig {
    // Domínio de e-mail da autarquia (enunciado I.3.A). Quem se regista com um e-mail
    // deste domínio fica registado como operador municipal; os outros como munícipes.
    const val OPERATOR_EMAIL_DOMAIN = "isec.pt"

    // Área do munícipe anónimo (enunciado I.1): raio de 10 km com centro na Praça da República, Coimbra
    const val GUEST_CENTER_LAT = 40.2086
    const val GUEST_CENTER_LNG = -8.4214
    const val GUEST_RADIUS_METERS = 10_000f

    // 2.º fator por e-mail (coleção "mail" + extensão Trigger Email do Firebase).
    // Validade do código enviado por e-mail e tentativas permitidas antes de ser preciso pedir outro
    const val MFA_CODE_VALIDITY_MINUTES = 10
    const val MFA_MAX_ATTEMPTS = 5
}