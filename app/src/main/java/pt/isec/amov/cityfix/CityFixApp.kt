package pt.isec.amov.cityfix

import android.app.Application
import pt.isec.amov.cityfix.data.AutoCloseWorker
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.CategoriesRepository
import pt.isec.amov.cityfix.data.IncidentsRepository
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.data.MessagesRepository
import pt.isec.amov.cityfix.data.StatsRepository
import pt.isec.amov.cityfix.geofence.GeofenceManager
import pt.isec.amov.cityfix.notifications.NotificationHelper
import pt.isec.amov.cityfix.notifications.NotificationsWatcher

/**
 * Classe Application: existe uma só instância enquanto o processo da app estiver vivo.
 * Guarda os repositórios partilhados por todos os ecrãs (cada um criado só quando é pedido pela primeira vez).
 * Registada no AndroidManifest.xml com android:name=".CityFixApp".
 */
class CityFixApp : Application() {
    val statsRepository by lazy {
        StatsRepository()
    }
    val repository by lazy {
        IncidentsRepository(this, statsRepository)
    }
    val locationRepository by lazy {
        LocationRepository(this)
    }
    val authRepository by lazy {
        AuthRepository(this)
    }
    val categoriesRepository by lazy {
        CategoriesRepository()
    }

    // Base combinada com a Parte B (mensagens)
    val messagesRepository by lazy {
        MessagesRepository(authRepository)
    }

    // Geofences do operador e notificações locais (enunciado III)
    val geofenceManager by lazy {
        GeofenceManager(this)
    }
    val notificationsWatcher by lazy {
        NotificationsWatcher(this, authRepository, repository, geofenceManager)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        notificationsWatcher.start()
        // Fecho automático das ocorrências resolvidas há mais de 5 dias (enunciado II.5)
        AutoCloseWorker.schedule(this)
    }
}