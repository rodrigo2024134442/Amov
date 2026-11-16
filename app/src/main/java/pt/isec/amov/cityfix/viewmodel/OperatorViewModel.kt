package pt.isec.amov.cityfix.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.CategoriesRepository
import pt.isec.amov.cityfix.data.StatsRepository
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole

/**
 * ViewModel das funções do operador: validar munícipes e gerir categorias.
 * Associado à MainActivity (os dois ecrãs do operador partilham-no).
 */
class OperatorViewModel(
    private val authRepository: AuthRepository,
    private val categoriesRepository: CategoriesRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    init {
        // Sempre que um operador entra, recalcula os indicadores do dashboard (stats/global)
        // a partir de todas as ocorrências: corrige contadores que tenham ficado errados.
        viewModelScope.launch {
            authRepository.session
                .filterIsInstance<Session.Active>()
                .filter { it.user.role == UserRole.OPERATOR }
                .map { it.user.uid }
                .distinctUntilChanged()
                .collect {
                    try {
                        statsRepository.rebuild()
                    } catch (e: Exception) {
                        Log.w(TAG, "Não foi possível recalcular as estatísticas", e)
                    }
                }
        }
    }

    val pendingCitizens: StateFlow<List<User>> = authRepository.observePendingCitizens()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoriesRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun activateCitizen(uid: String) = authRepository.activateCitizen(uid)

    /**
     * Cria (id == null) ou edita uma categoria. Nomes vazios são ignorados.
     */
    fun saveCategory(id: String?, name: String, description: String) {
        if (name.isBlank()) return
        val existing = categories.value.find { it.id == id }
        val category = existing?.copy(name = name.trim(), description = description.trim())
            ?: Category(name = name.trim(), description = description.trim())
        categoriesRepository.saveCategory(category)
    }

    fun setCategoryActive(id: String, active: Boolean) = categoriesRepository.setActive(id, active)

    fun createDefaultCategories() = categoriesRepository.createDefaults()

    companion object {
        private const val TAG = "OperatorViewModel"
    }
}