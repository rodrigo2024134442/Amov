package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.utils.Totp
import pt.isec.amov.cityfix.utils.Validators

/**
 * ViewModel global da sessão (associado à MainActivity): estado da sessão, 2.º fator,
 * sair e editar o perfil.
 */
class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {

    /**
     * Estado da sessão (Loading, Guest, MfaSetup, AwaitingMfa, PendingValidation, Active).
     */
    val session: StateFlow<Session> = authRepository.session

    /**
     * Segredo para configurar a app autenticadora. Criado uma vez e guardado no ViewModel,
     * para não mudar se o ecrã rodar enquanto o utilizador o está a ler.
     */
    val newMfaSecret: String = Totp.generateSecret()

    // Mensagem de erro do ecrã do 2.º fator, como ID de strings.xml
    private val _mfaMessage = MutableStateFlow<Int?>(null)
    val mfaMessage: StateFlow<Int?> = _mfaMessage.asStateFlow()

    // Erro do telefone no ecrã de perfil
    private val _profilePhoneError = MutableStateFlow<Int?>(null)
    val profilePhoneError: StateFlow<Int?> = _profilePhoneError.asStateFlow()

    /**
     * Logo a seguir ao registo: confirma o primeiro código e grava o segredo.
     */
    fun confirmMfaSetup(code: String) = runMfa { authRepository.confirmMfaSetup(newMfaSecret, code) }

    /**
     * No login: confirma o código da app autenticadora.
     */
    fun verifyMfa(code: String) = runMfa { authRepository.verifyMfa(code) }

    private fun runMfa(check: suspend () -> Boolean) {
        viewModelScope.launch {
            _mfaMessage.value = try {
                if (check()) null else R.string.mfa_wrong_code
            } catch (_: Exception) {
                R.string.error_network
            }
        }
    }

    fun signOut() {
        _mfaMessage.value = null
        authRepository.signOut()
    }

    // Erro ao enviar a fotografia de perfil
    private val _profileMessage = MutableStateFlow<Int?>(null)
    val profileMessage: StateFlow<Int?> = _profileMessage.asStateFlow()

    /**
     * Grava o perfil se o telefone for válido e, se foi escolhida, envia a fotografia nova;
     * chama onSuccess no fim.
     */
    fun saveProfile(phone: String, impactDetection: Boolean, newPhotoPath: String?, onSuccess: () -> Unit) {
        if (!Validators.isValidPhone(phone)) {
            _profilePhoneError.value = R.string.error_phone
            return
        }
        _profilePhoneError.value = null
        authRepository.updateProfile(phone.trim(), impactDetection)
        viewModelScope.launch {
            try {
                if (newPhotoPath != null) authRepository.uploadProfilePhoto(newPhotoPath)
                _profileMessage.value = null
                onSuccess()
            } catch (_: Exception) {
                _profileMessage.value = R.string.error_network
            }
        }
    }
}