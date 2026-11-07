package pt.isec.amov.cityfix.data

import com.google.firebase.firestore.DocumentSnapshot
import pt.isec.amov.cityfix.model.Category

// Conversão entre a Category (modelo) e os documentos categories/{id} do Firestore.

fun Category.toFirestoreMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "description" to description,
    "isActive" to isActive
)

fun DocumentSnapshot.toCategory(): Category? = Category(
    id = id,
    name = getString("name") ?: return null,
    description = getString("description") ?: "",
    isActive = getBoolean("isActive") ?: true
)