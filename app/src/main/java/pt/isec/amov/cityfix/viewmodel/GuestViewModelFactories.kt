package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.IncidentsRepository
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.data.StatsRepository

// Fábricas dos ViewModels do perfil anónimo, iguais às da aula P08
// (passam os repositórios ao construtor).

class GuestAreaViewModelFactory(
    private val locationRepository: LocationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GuestAreaViewModel(locationRepository) as T
}

class DashboardViewModelFactory(
    private val statsRepository: StatsRepository,
    private val incidentsRepository: IncidentsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DashboardViewModel(statsRepository, incidentsRepository) as T
}
