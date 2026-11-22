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

    // Ocorrência de grupo (enunciado I.2, Obs.): mesma categoria e situação a menos de 10 m
    const val GROUP_RADIUS_METERS = 10f

    // Geofence do operador (enunciado I.3, Obs. e II.3): alerta aos autores a menos de 20 m
    const val GEOFENCE_RADIUS_METERS = 20f

    // Deteção de impactos (enunciado I.2.F): aceleração vertical (sem a gravidade) a partir da qual
    // se sugere abrir uma ocorrência, em m/s², e o intervalo mínimo entre duas sugestões
    const val IMPACT_THRESHOLD = 12f
    const val IMPACT_COOLDOWN_MS = 30_000L
}
