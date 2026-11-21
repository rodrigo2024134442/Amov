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
import pt.isec.amov.cityfix.utils.Validators

/**
 * ViewModel global da sessão (associado à MainActivity): estado da sessão, 2.º fator,
 * sair e editar o perfil.
 */
class SessionViewModel(private val authRepository: AuthRepository) : ViewModel() {

    /**
     * Estado da sessão (Loading, Guest, AwaitingMfa, PendingValidation, Active).
     */
    val session: StateFlow<Session> = authRepository.session

    // Mensagem de erro do ecrã do 2.º fator, como ID de strings.xml
    private val _mfaMessage = MutableStateFlow<Int?>(null)
    val mfaMessage: StateFlow<Int?> = _mfaMessage.asStateFlow()

    // Aviso (não erro) do ecrã do 2.º fator, por exemplo "Novo código enviado"
    private val _mfaInfo = MutableStateFlow<Int?>(null)
    val mfaInfo: StateFlow<Int?> = _mfaInfo.asStateFlow()

    // Erro do telefone no ecrã de perfil
    private val _profilePhoneError = MutableStateFlow<Int?>(null)
    val profilePhoneError: StateFlow<Int?> = _profilePhoneError.asStateFlow()

    /**
     * Ao abrir o ecrã do 2.º fator: envia o código por e-mail, se ainda não houver um válido.
     */
    fun ensureMfaCode() = sendMfaCode(force = false)

    /**
     * Botão "Enviar novo código".
     */
    fun resendMfaCode() = sendMfaCode(force = true)

    private fun sendMfaCode(force: Boolean) {
        viewModelScope.launch {
            try {
                authRepository.sendMfaCode(force)
                if (force) {
                    _mfaMessage.value = null
                    _mfaInfo.value = R.string.mfa_code_sent
                }
            } catch (_: Exception) {
                _mfaInfo.value = null
                _mfaMessage.value = R.string.mfa_send_error
            }
        }
    }

    /**
     * Confirma o código recebido por e-mail.
     */
    fun verifyMfa(code: String) {
        _mfaInfo.value = null
        _mfaMessage.value = if (authRepository.verifyMfa(code)) null else R.string.mfa_wrong_code
    }

    fun signOut() {
        _mfaMessage.value = null
        _mfaInfo.value = null
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