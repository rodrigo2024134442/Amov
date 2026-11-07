package pt.isec.amov.cityfix.model

/**
 * Em que ponto está a sessão do utilizador. A MainActivity decide o que mostrar a partir daqui.
 */
sealed interface Session {

    // Ainda a ler o estado do Firebase (arranque da app)
    data object Loading : Session

    // Sem sessão: munícipe anónimo
    data object Guest : Session

    // Password certa, mas ainda sem app autenticadora configurada (logo a seguir ao registo)
    data class MfaSetup(val email: String) : Session

    // Password certa, à espera do código da app autenticadora (2.º fator)
    data class AwaitingMfa(val email: String) : Session

    // Sessão completa, mas o munícipe ainda não foi validado por um operador
    data class PendingValidation(val user: User) : Session

    // Sessão completa e conta ativa (munícipe validado ou operador)
    data class Active(val user: User) : Session
}