package pt.isec.amov.cityfix.data

import com.google.firebase.firestore.DocumentSnapshot
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.model.UserStatus

// Conversão entre o User (modelo) e os documentos users/{uid} do Firestore,
// feita à mão como no IncidentMapper. O uid é o ID do documento.

fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "email" to email,
    "phone" to phone,
    "nif" to nif,
    "role" to role.name,
    "status" to status.name,
    "impactDetection" to impactDetection,
    "photoUrl" to photoUrl,
    "latitude" to latitude,
    "longitude" to longitude,
    "createdAt" to createdAt
)

fun DocumentSnapshot.toUser(): User? = try {
    User(
        uid = id,
        name = getString("name") ?: return null,
        email = getString("email") ?: "",
        phone = getString("phone") ?: "",
        nif = getString("nif") ?: "",
        role = UserRole.valueOf(getString("role") ?: return null),
        status = UserStatus.valueOf(getString("status") ?: UserStatus.PENDING.name),
        impactDetection = getBoolean("impactDetection") ?: false,
        photoUrl = getString("photoUrl"),
        latitude = getDouble("latitude"),
        longitude = getDouble("longitude"),
        createdAt = getLong("createdAt") ?: 0L
    )
} catch (_: IllegalArgumentException) {
    null
}