package pt.isec.amov.cityfix.notifications

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import pt.isec.amov.cityfix.model.IncidentStatus

/**
 * Notificações push (Firebase Cloud Messaging, enunciado III), enviadas pelas Cloud Functions
 * (pasta functions/) quando uma ocorrência muda. São mensagens só de dados: o texto é construído
 * aqui, na língua do telemóvel. Têm o mesmo ID das locais (NotificationsWatcher): se a mesma
 * notificação chegar pelos dois caminhos, aparece uma só vez.
 */
class CityFixMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"]?.let { runCatching { NotificationType.valueOf(it) }.getOrNull() } ?: return
        val incidentId = data["incidentId"] ?: return
        val title = data["title"].orEmpty()
        val status = data["status"]?.let { runCatching { IncidentStatus.valueOf(it) }.getOrNull() }
        NotificationHelper.showEvent(applicationContext, type, incidentId, title, status)
    }

    // O token mudou (reinstalação, dados apagados...): guarda o novo no perfil
    override fun onNewToken(token: String) {
        Firebase.auth.currentUser?.uid?.let { NotificationsWatcher.saveToken(it, token) }
    }
}
