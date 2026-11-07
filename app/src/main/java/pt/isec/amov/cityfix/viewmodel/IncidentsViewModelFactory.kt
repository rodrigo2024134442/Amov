package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.IncidentsRepository

/**
 * Fábrica do IncidentsViewModel: o viewModel() do Compose só sabe criar ViewModels
 * sem parâmetros; esta classe ensina-o a passar os repositórios ao construtor.
 */
class IncidentsViewModelFactory(
    private val repository: IncidentsRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        IncidentsViewModel(repository, authRepository) as T
}