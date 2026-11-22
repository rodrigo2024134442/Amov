package pt.isec.amov.cityfix.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import pt.isec.amov.cityfix.AppConfig
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentStatus

/**
 * Geofences de 20 m à volta das ocorrências ativas, no telemóvel do operador (enunciado I.3, Obs.
 * e II.3). Quando o operador entra numa, o GeofenceReceiver regista-o no Firestore e os autores
 * são notificados de que a equipa camarária está na área.
 * O Android limita cada app a 100 geofences: têm prioridade as "Em Resolução".
 */
class GeofenceManager(private val context: Context) {

    private val client = LocationServices.getGeofencingClient(context)

    // Ocorrências com geofence ativa (para só voltar a registar quando o conjunto muda)
    private var registered: Set<String> = emptySet()

    // Últimas ocorrências recebidas (para voltar a registar depois de a permissão ser dada)
    private var lastIncidents: List<Incident> = emptyList()

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java)
        // O Android preenche a Intent com o evento: tem de ser MUTABLE (Android 12+)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Localização em segundo plano: precisa dela para as geofences funcionarem com a app fechada. */
    fun hasBackgroundLocation(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Regista as geofences das ocorrências ativas com coordenadas. [force] volta a registar mesmo
     * que o conjunto não tenha mudado (ex.: depois de a permissão ser dada).
     */
    @SuppressLint("MissingPermission") // verificado em hasFineLocation()
    fun update(incidents: List<Incident>, force: Boolean = false) {
        lastIncidents = incidents
        if (!hasFineLocation()) return
        val targets = incidents
            .filter { it.status.isActive && it.latitude != null && it.longitude != null }
            .sortedByDescending { it.status == IncidentStatus.IN_PROGRESS }
            .take(MAX_GEOFENCES)
        val ids = targets.map { it.id }.toSet()
        if (!force && ids == registered) return
        registered = ids
        client.removeGeofences(pendingIntent).addOnCompleteListener {
            if (targets.isEmpty()) return@addOnCompleteListener
            val geofences = targets.map {
                Geofence.Builder()
                    .setRequestId(it.id)
                    .setCircularRegion(it.latitude!!, it.longitude!!, AppConfig.GEOFENCE_RADIUS_METERS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            }
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()
            try {
                client.addGeofences(request, pendingIntent)
                    .addOnSuccessListener { Log.i(TAG, "${geofences.size} geofence(s) registada(s)") }
                    .addOnFailureListener { e ->
                        registered = emptySet()
                        Log.w(TAG, "Não foi possível registar as geofences", e)
                    }
            } catch (e: SecurityException) {
                registered = emptySet()
                Log.w(TAG, "Sem permissão de localização para as geofences", e)
            }
        }
    }

    /** A permissão foi dada agora: regista as geofences das últimas ocorrências recebidas. */
    fun refresh() = update(lastIncidents, force = true)

    /** Sem sessão de operador: retira todas as geofences. */
    fun clear() {
        lastIncidents = emptyList()
        if (registered.isEmpty()) return
        client.removeGeofences(pendingIntent)
        registered = emptySet()
    }

    companion object {
        private const val TAG = "GeofenceManager"
        private const val MAX_GEOFENCES = 100
    }
}
