package pt.isec.amov.cityfix

import android.app.Application
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.CategoriesRepository
import pt.isec.amov.cityfix.data.IncidentsRepository

/**
 * Classe Application: existe uma só instância enquanto o processo da app estiver vivo.
 * Guarda os repositórios partilhados por todos os ecrãs (cada um criado só quando é pedido pela primeira vez).
 * Registada no AndroidManifest.xml com android:name=".CityFixApp".
 */
class CityFixApp : Application() {
    val repository by lazy {
        IncidentsRepository(this)
    }
    val authRepository by lazy {
        AuthRepository(this)
    }
    val categoriesRepository by lazy {
        CategoriesRepository()
    }
}