package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.AppConfig
import pt.isec.amov.cityfix.data.LocationRepository

/**
 * Resultado da verificação da área do munícipe anónimo (10 km da Praça da República).
 */
sealed interface AreaState {
    // Ainda não se verificou (arranque)
    data object Unknown : AreaState

    // A obter a localização
    data object Checking : AreaState

    // O utilizador recusou a permissão de localização
    data object PermissionDenied : AreaState

    // Com permissão, mas sem posição (localização desligada, sem GPS...)
    data object LocationUnavailable : AreaState

    // Fora do raio de 10 km
    data class Outside(val distanceKm: Double) : AreaState

    // Dentro do raio: pode usar a app sem sessão
    data object Inside : AreaState
}

/**
 * ViewModel da verificação da área, associado à MainActivity: o resultado mantém-se
 * enquanto a app estiver aberta (inclusive ao rodar o ecrã ou depois de terminar sessão).
 * O pedido da permissão é feito pelo ecrã (precisa de um launcher do Compose); o resultado
 * chega aqui por onPermissionResult.
 */
class GuestAreaViewModel(private val locationRepository: LocationRepository) : ViewModel() {

    private val _state = MutableStateFlow<AreaState>(AreaState.Unknown)
    val state: StateFlow<AreaState> = _state.asStateFlow()

    val hasPermission: Boolean
        get() = locationRepository.hasPermission()

    fun onPermissionResult(granted: Boolean) {
        if (granted) checkLocation() else _state.value = AreaState.PermissionDenied
    }

    /**
     * Obtém a posição e compara com o raio definido em AppConfig.
     */
    fun checkLocation() {
        if (_state.value == AreaState.Checking) return
        _state.value = AreaState.Checking
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            _state.value = when {
                location == null ->
                    if (locationRepository.hasPermission()) AreaState.LocationUnavailable
                    else AreaState.PermissionDenied
                else -> {
                    val meters = locationRepository.distanceToGuestCenter(location)
                    if (meters <= AppConfig.GUEST_RADIUS_METERS) AreaState.Inside
                    else AreaState.Outside(meters / 1000.0)
                }
            }
        }
    }
}
