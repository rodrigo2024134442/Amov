package pt.isec.amov.cityfix.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.isec.amov.cityfix.data.AuthRepository
import pt.isec.amov.cityfix.data.IncidentQuery
import pt.isec.amov.cityfix.data.IncidentsRepository
import pt.isec.amov.cityfix.geofence.GeofenceManager
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole

/**
 * Notificações locais (enunciado III), geradas a partir dos snapshot listeners do Firestore
 * enquanto o processo da app está vivo (em primeiro ou segundo plano):
 *  - munícipe (autor ou seguidor de uma ocorrência de grupo): mudança de estado, mensagem nova,
 *    equipa camarária a menos de 20 m e, por WorkManager, o aviso das 24 h antes do fecho;
 *  - operador: nova ocorrência, mensagem nova, pedido de esclarecimento ou reabertura pelo autor.
 * O último estado visto de cada ocorrência fica guardado no telemóvel: ao voltar a abrir a app,
 * as mudanças que aconteceram com ela fechada também são notificadas.
 * Com a app fechada há ainda as notificações push (FCM), enviadas pelas Cloud Functions.
 *
 * Também mantém as geofences do operador (GeofenceManager) e regista o token FCM do telemóvel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsWatcher(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val repository: IncidentsRepository,
    val geofenceManager: GeofenceManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)

    fun start() {
        scope.launch {
            authRepository.session
                .map { (it as? Session.Active)?.user }
                .distinctUntilChangedBy { it?.uid to it?.role }
                .flatMapLatest { user ->
                    if (user == null) {
                        geofenceManager.clear()
                        flowOf(null)
                    } else {
                        registerPushToken(user.uid)
                        incidentsFor(user).map { user to it }
                    }
                }
                .collect { pair -> pair?.let { (user, incidents) -> onIncidents(user, incidents) } }
        }
    }

    // O operador acompanha todas; o munícipe as suas e as de grupo que segue
    private fun incidentsFor(user: User): Flow<List<Incident>> =
        if (user.role == UserRole.OPERATOR) repository.observeIncidents(IncidentQuery.All)
        else combine(
            repository.observeIncidents(IncidentQuery.ByUser(user.uid)),
            repository.observeIncidents(IncidentQuery.Followed(user.uid))
        ) { mine, followed -> (mine + followed).distinctBy { it.id } }

    private fun onIncidents(user: User, incidents: List<Incident>) {
        val me = user.uid
        val isOperator = user.role == UserRole.OPERATOR
        // Primeira vez neste telemóvel com esta conta: só se guarda o estado (não se notifica o passado)
        val initKey = "init_$me"
        val firstSync = !prefs.getBoolean(initKey, false)
        val editor = prefs.edit()

        incidents.forEach { incident ->
            val key = "$me|${incident.id}"
            val previous = prefs.getString(key, null)
            editor.putString(key, stateOf(incident))

            // Aviso das 24 h antes do fecho (só para quem acompanha a ocorrência)
            if (!isOperator) {
                if (incident.status == IncidentStatus.RESOLVED) ClosingReminderWorker.schedule(context, incident)
                else ClosingReminderWorker.cancel(context, incident.id)
            }
            if (firstSync) return@forEach

            if (previous == null) {
                // Ocorrência que este utilizador ainda não conhecia: para o operador, é nova
                if (isOperator && incident.status == IncidentStatus.OPEN) {
                    NotificationHelper.showEvent(context, NotificationType.NEW_INCIDENT, incident.id, incident.title)
                }
                return@forEach
            }
            val (oldStatus, oldMessage, oldNearby) = previous.split("|").let {
                Triple(it.getOrNull(0), it.getOrNull(1)?.toLongOrNull() ?: 0L, it.getOrNull(2)?.toLongOrNull() ?: 0L)
            }

            // Mudança de estado feita por outra pessoa (o operador só quer saber das do autor:
            // pedido de esclarecimento, resposta ou reabertura)
            if (oldStatus != incident.status.name) {
                val by = incident.history.lastOrNull()?.byUid
                val notify = if (isOperator) by == incident.userId else by != me
                if (notify) {
                    NotificationHelper.showEvent(
                        context, NotificationType.STATUS, incident.id, incident.title, incident.status
                    )
                }
            }
            // Mensagem nova de outra pessoa (pedido de informação ou resposta)
            if ((incident.lastMessageAt ?: 0L) > oldMessage && incident.lastMessageBy != me) {
                NotificationHelper.showEvent(context, NotificationType.MESSAGE, incident.id, incident.title)
            }
            // Equipa camarária a menos de 20 m (geofence do operador)
            if (!isOperator && (incident.operatorNearbyAt ?: 0L) > oldNearby && incident.status.isActive) {
                NotificationHelper.showEvent(context, NotificationType.PROXIMITY, incident.id, incident.title)
            }
        }
        editor.putBoolean(initKey, true).apply()

        if (isOperator) geofenceManager.update(incidents)
    }

    private fun stateOf(incident: Incident): String =
        "${incident.status.name}|${incident.lastMessageAt ?: 0}|${incident.operatorNearbyAt ?: 0}"

    /**
     * Guarda no perfil o token FCM deste telemóvel, para as Cloud Functions enviarem push
     * quando a app está fechada.
     */
    private fun registerPushToken(uid: String) {
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                saveToken(uid, token)
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível obter o token FCM", e)
            }
        }
    }

    companion object {
        private const val TAG = "NotificationsWatcher"

        fun saveToken(uid: String, token: String) {
            Firebase.firestore.collection("users").document(uid).update("fcmToken", token)
        }
    }
}
