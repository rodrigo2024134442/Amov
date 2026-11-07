package pt.isec.amov.cityfix.data

import com.google.firebase.firestore.DocumentSnapshot
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.SyncStatus

// Conversão entre o Incident (modelo puro, sem Firebase) e os documentos do Firestore.
// Feita à mão, campo a campo: o toObject() do Firestore obrigava o modelo a ter um construtor
// sem argumentos e valores por omissão em todos os campos.
// Os enums guardam-se pelo nome (ex: "DAMAGE"), como no ficheiro JSON da etapa anterior.

/**
 * Incident -> Map, para gravar com document(id).set(...).
 */
fun Incident.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "title" to title,
    "description" to description,
    "category" to category,
    "situation" to situation.name,
    "scope" to scope.name,
    "latitude" to latitude,
    "longitude" to longitude,
    "mediaPaths" to mediaPaths,
    "mediaUrls" to mediaUrls,
    "thumbnailUrl" to thumbnailUrl,
    "status" to status.name,
    "timestamp" to timestamp,
    "resolvedAt" to resolvedAt,
    "syncStatus" to syncStatus.name
)

/**
 * Documento -> Incident. Devolve null se faltar um campo obrigatório ou um enum for inválido
 * (um documento estragado não deve rebentar a lista inteira).
 */
@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toIncident(): Incident? = try {
    Incident(
        id = id,
        userId = getString("userId") ?: "",
        title = getString("title") ?: return null,
        description = getString("description") ?: "",
        category = getString("category") ?: "",
        situation = Situation.valueOf(getString("situation") ?: return null),
        scope = Scope.valueOf(getString("scope") ?: Scope.PUBLIC.name),
        latitude = getDouble("latitude"),
        longitude = getDouble("longitude"),
        mediaPaths = get("mediaPaths") as? List<String> ?: emptyList(),
        mediaUrls = get("mediaUrls") as? List<String> ?: emptyList(),
        thumbnailUrl = getString("thumbnailUrl"),
        status = IncidentStatus.valueOf(getString("status") ?: IncidentStatus.OPEN.name),
        timestamp = getLong("timestamp") ?: 0L,
        resolvedAt = getLong("resolvedAt"),
        syncStatus = SyncStatus.valueOf(getString("syncStatus") ?: SyncStatus.PENDING_SYNC.name)
    )
} catch (_: IllegalArgumentException) {
    null
}