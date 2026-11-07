package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.utils.Validators

/**
 * Estado do ecrã de login. As mensagens são IDs de strings.xml.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: Int? = null,
    val infoMessage: Int? = null
) {
    val isValid: Boolean
        get() = emailError == null && email.isNotBlank() && password.isNotBlank()
}

/**
 * ViewModel do ecrã de login, associado ao destino LoginRoute (como o EditIncidentViewModel).
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChanged(email: String) {
        _state.update {
            it.copy(
                email = email,
                emailError = if (Validators.isValidEmail(email)) null else R.string.error_email,
                errorMessage = null, infoMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password, errorMessage = null, infoMessage = null) }
    }

    /**
     * 1.º fator. Se correr bem, a sessão passa a AwaitingMfa e a MainActivity mostra o ecrã do link.
     */
    fun signIn() {
        val current = _state.value
        if (!current.isValid || current.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val error = try {
                authRepository.signIn(current.email.trim(), current.password)
                null
            } catch (e: Exception) {
                errorFor(e)
            }
            _state.update { it.copy(isLoading = false, errorMessage = error) }
        }
    }

    /**
     * Recuperação de credenciais: envia o e-mail para definir uma password nova.
     */
    fun resetPassword() {
        val current = _state.value
        if (!Validators.isValidEmail(current.email)) {
            _state.update { it.copy(emailError = R.string.error_email) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.sendPasswordReset(current.email.trim())
                _state.update { it.copy(isLoading = false, infoMessage = R.string.reset_sent) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = errorFor(e)) }
            }
        }
    }

    private fun errorFor(e: Exception): Int = when (e) {
        is FirebaseNetworkException -> R.string.error_network
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException -> R.string.error_login
        else -> R.string.error_generic
    }
}