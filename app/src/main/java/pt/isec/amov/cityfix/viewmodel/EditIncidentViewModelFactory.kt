package pt.isec.amov.cityfix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.CategoriesRepository
import pt.isec.amov.cityfix.data.IncidentsRepository

/**
 * Fábrica do EditIncidentViewModel: passa-lhe os repositórios e o ID do incidente
 * (null = criar um novo; com valor = editar esse incidente).
 */
class EditIncidentViewModelFactory(
    private val repository: IncidentsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val authRepository: AuthRepository,
    private val incidentId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        EditIncidentViewModel(repository, categoriesRepository, authRepository, incidentId) as T
}