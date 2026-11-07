package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.NifAlreadyUsedException
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.model.UserStatus
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.utils.Validators

/**
 * Estado do formulário de registo: valores, erros (IDs de strings.xml) e se está a enviar.
 */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val nif: String = "",
    val impactDetection: Boolean = false,
    val photoPath: String? = null,
    val nameError: Int? = null,
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val phoneError: Int? = null,
    val nifError: Int? = null,
    val isLoading: Boolean = false,
    val errorMessage: Int? = null
) {
    val isValid: Boolean
        get() = listOf(nameError, emailError, passwordError, phoneError, nifError).all { it == null } &&
                listOf(name, email, password, phone, nif).all { it.isNotBlank() }

    // O perfil vem do e-mail: domínio da autarquia = operador; outro = munícipe
    val role: UserRole
        get() = if (Validators.isOperatorEmail(email)) UserRole.OPERATOR else UserRole.CITIZEN
}

/**
 * ViewModel do registo de munícipe ou operador, associado ao destino RegisterRoute.
 */
class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name, nameError = if (name.isBlank()) R.string.error_name_empty else null) }
    }

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email, emailError = if (Validators.isValidEmail(email)) null else R.string.error_email) }
    }

    fun onPasswordChanged(password: String) {
        _state.update {
            it.copy(password = password, passwordError = if (Validators.isValidPassword(password)) null else R.string.error_password)
        }
    }

    fun onPhoneChanged(phone: String) {
        _state.update { it.copy(phone = phone, phoneError = if (Validators.isValidPhone(phone)) null else R.string.error_phone) }
    }

    fun onNifChanged(nif: String) {
        _state.update { it.copy(nif = nif, nifError = if (Validators.isValidNif(nif)) null else R.string.error_nif) }
    }

    fun onImpactDetectionChanged(enabled: Boolean) {
        _state.update { it.copy(impactDetection = enabled) }
    }

    /**
     * Fotografia escolhida (opcional). A anterior, se havia, deixa de ser precisa e é apagada.
     */
    fun onPhotoChanged(path: String) {
        FileUtils.deleteFileFromPath(_state.value.photoPath)
        _state.update { it.copy(photoPath = path) }
    }

    // Passa a true depois de registar; se o ecrã fechar antes, a fotografia escolhida é apagada
    private var registered = false

    override fun onCleared() {
        super.onCleared()
        if (!registered) FileUtils.deleteFileFromPath(_state.value.photoPath)
    }

    /**
     * Cria a conta. Se correr bem, a sessão passa a AwaitingMfa (o link foi enviado).
     */
    fun register() {
        val s = _state.value
        if (!s.isValid || s.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val user = User(
                uid = "", // preenchido pelo repositório com o uid do Firebase Auth
                name = s.name.trim(),
                email = s.email.trim(),
                phone = s.phone.trim(),
                nif = s.nif.trim(),
                role = s.role,
                // Enunciado: o munícipe fica pendente; o operador fica logo ativo
                status = if (s.role == UserRole.OPERATOR) UserStatus.ACTIVE else UserStatus.PENDING,
                impactDetection = s.role == UserRole.CITIZEN && s.impactDetection
            )
            val error = try {
                authRepository.register(user, s.password, s.photoPath)
                registered = true
                null
            } catch (e: Exception) {
                when (e) {
                    is NifAlreadyUsedException -> R.string.error_nif_used
                    is FirebaseAuthUserCollisionException -> R.string.error_email_used
                    is FirebaseAuthWeakPasswordException -> R.string.error_password
                    is FirebaseNetworkException -> R.string.error_network
                    else -> R.string.error_generic
                }
            }
            _state.update { it.copy(isLoading = false, errorMessage = error) }
        }
    }
}