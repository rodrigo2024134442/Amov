package pt.isec.amov.cityfix.data

import com.google.firebase.firestore.DocumentSnapshot
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.model.StatusChange
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
    "address" to address,
    "mediaPaths" to mediaPaths,
    "mediaUrls" to mediaUrls,
    "thumbnailUrl" to thumbnailUrl,
    "status" to status.name,
    "timestamp" to timestamp,
    "resolvedAt" to resolvedAt,
    "history" to history.map { it.toFirestoreMap() },
    "proofPath" to proofPath,
    "proofUrl" to proofUrl,
    "syncStatus" to syncStatus.name
)

/**
 * StatusChange -> Map (uma entrada da lista "history" do documento).
 */
fun StatusChange.toFirestoreMap(): Map<String, Any?> = mapOf(
    "status" to status.name,
    "timestamp" to timestamp,
    "byUid" to byUid,
    "note" to note
)

// Entrada do histórico lida do Firestore; null se estiver estragada
private fun Map<*, *>.toStatusChange(): StatusChange? = try {
    StatusChange(
        status = IncidentStatus.valueOf(this["status"] as? String ?: return null),
        timestamp = (this["timestamp"] as? Number)?.toLong() ?: return null,
        byUid = this["byUid"] as? String ?: "",
        note = this["note"] as? String
    )
} catch (_: IllegalArgumentException) {
    null
}

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
        address = getString("address"),
        mediaPaths = get("mediaPaths") as? List<String> ?: emptyList(),
        mediaUrls = get("mediaUrls") as? List<String> ?: emptyList(),
        thumbnailUrl = getString("thumbnailUrl"),
        status = IncidentStatus.valueOf(getString("status") ?: IncidentStatus.OPEN.name),
        timestamp = getLong("timestamp") ?: 0L,
        resolvedAt = getLong("resolvedAt"),
        history = (get("history") as? List<*>)
            ?.mapNotNull { (it as? Map<*, *>)?.toStatusChange() }
            ?.sortedBy { it.timestamp }
            ?: emptyList(),
        proofPath = getString("proofPath"),
        proofUrl = getString("proofUrl"),
        syncStatus = SyncStatus.valueOf(getString("syncStatus") ?: SyncStatus.PENDING_SYNC.name)
    )
} catch (_: IllegalArgumentException) {
    null
}