package pt.isec.amov.cityfix.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pt.isec.amov.cityfix.MainActivity
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.IncidentStatus

/**
 * Tipos de notificação (enunciado III). Cada tipo tem um canal: o utilizador pode desligar
 * só um deles nas definições do Android.
 */
enum class NotificationType(val channel: String) {
    STATUS(NotificationHelper.CHANNEL_TICKETS),      // mudança de estado do ticket
    MESSAGE(NotificationHelper.CHANNEL_TICKETS),     // pedido de informação / resposta
    NEW_INCIDENT(NotificationHelper.CHANNEL_TICKETS),// nova ocorrência (para o operador)
    PROXIMITY(NotificationHelper.CHANNEL_PROXIMITY), // equipa camarária a menos de 20 m
    CLOSING(NotificationHelper.CHANNEL_TICKETS),     // faltam 24 h para o fecho definitivo
    IMPACT(NotificationHelper.CHANNEL_IMPACT)        // impacto detetado (sugestão de ocorrência)
}

/**
 * Constrói e mostra as notificações locais. As que chegam por push (FCM) usam as mesmas funções,
 * com o mesmo ID: se a mesma notificação chegar pelos dois caminhos, a segunda substitui a primeira.
 */
object NotificationHelper {

    const val CHANNEL_TICKETS = "tickets"
    const val CHANNEL_PROXIMITY = "proximity"
    const val CHANNEL_IMPACT = "impact"

    // Extra da Intent: ID da ocorrência a abrir quando se toca na notificação
    const val EXTRA_INCIDENT_ID = "incidentId"
    const val EXTRA_OPEN_IMPACT = "openImpact"

    /** Cria os canais (Android 8+). Chamado ao arrancar a app; repetir não faz mal. */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_TICKETS, context.getString(R.string.channel_tickets), NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    CHANNEL_PROXIMITY, context.getString(R.string.channel_proximity), NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_IMPACT, context.getString(R.string.channel_impact), NotificationManager.IMPORTANCE_HIGH
                )
            )
        )
    }

    /** true se a app pode mostrar notificações (Android 13+ precisa da permissão POST_NOTIFICATIONS). */
    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Mostra uma notificação sobre a ocorrência [incidentId]. Ao tocar, abre o detalhe.
     * O ID depende da ocorrência e do tipo: uma notificação nova do mesmo tipo substitui a anterior.
     */
    fun show(
        context: Context,
        type: NotificationType,
        incidentId: String?,
        title: String,
        text: String
    ) {
        if (!canNotify(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            incidentId?.let { putExtra(EXTRA_INCIDENT_ID, it) }
            if (type == NotificationType.IMPACT) putExtra(EXTRA_OPEN_IMPACT, true)
        }
        val id = notificationId(type, incidentId)
        val pending = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, type.channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(
                if (type == NotificationType.PROXIMITY || type == NotificationType.IMPACT) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permissão retirada entretanto: não há nada a fazer
        }
    }

    private fun notificationId(type: NotificationType, incidentId: String?): Int =
        "${type.name}:${incidentId.orEmpty()}".hashCode()

    // --- Textos, na língua do dispositivo ---

    fun statusText(context: Context, status: IncidentStatus): String =
        context.getString(R.string.notif_status_text, context.getString(statusLabel(status)))

    fun statusLabel(status: IncidentStatus): Int = when (status) {
        IncidentStatus.OPEN -> R.string.status_open
        IncidentStatus.INFO_REQUESTED -> R.string.status_info_requested
        IncidentStatus.IN_PROGRESS -> R.string.status_in_progress
        IncidentStatus.RESOLVED -> R.string.status_resolved
        IncidentStatus.CLOSED -> R.string.status_closed
    }

    /** Mostra a notificação de um evento, com o texto certo para cada tipo. */
    fun showEvent(
        context: Context,
        type: NotificationType,
        incidentId: String,
        incidentTitle: String,
        status: IncidentStatus? = null
    ) {
        val text = when (type) {
            NotificationType.STATUS -> status?.let { statusText(context, it) } ?: return
            NotificationType.MESSAGE -> context.getString(R.string.notif_message_text)
            NotificationType.NEW_INCIDENT -> context.getString(R.string.notif_new_incident_text)
            NotificationType.PROXIMITY -> context.getString(R.string.notif_proximity_text)
            NotificationType.CLOSING -> context.getString(R.string.notif_closing_text)
            NotificationType.IMPACT -> context.getString(R.string.impact_text)
        }
        show(context, type, incidentId, incidentTitle, text)
    }
}
