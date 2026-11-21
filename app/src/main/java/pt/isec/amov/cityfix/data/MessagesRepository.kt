package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.UserRole

/**
 * Mensagens de uma ocorrência, na subcoleção incidents/{incidentId}/messages (Firestore, com cache offline).
 *
 * BASE COMBINADA ENTRE A PARTE A E A PARTE B:
 *  - send(incidentId, text) é a função partilhada (a Parte A usa-a no "Pedir dados" do operador);
 *  - a Parte B (Marco) acrescenta AQUI, neste ficheiro, o observeMessages(incidentId) e o resto.
 *    Não mudar a assinatura de send sem combinar.
 */
class MessagesRepository(private val authRepository: AuthRepository) {

    private val incidents = Firebase.firestore.collection("incidents")

    /**
     * Envia uma mensagem do utilizador com sessão ativa (munícipe ou operador).
     * Sem await: sem rede fica na fila do Firestore e é enviada quando a rede voltar.
     * Ignora texto vazio ou sem sessão ativa.
     */
    fun send(incidentId: String, text: String) {
        val user = (authRepository.session.value as? Session.Active)?.user
        val trimmed = text.trim()
        if (user == null || trimmed.isEmpty()) {
            Log.w(TAG, "Mensagem ignorada (sem sessão ativa ou texto vazio)")
            return
        }
        val message = Message(senderUid = user.uid, senderRole = user.role, text = trimmed)
        incidents.document(incidentId).collection(MESSAGES).add(message.toFirestoreMap())
    }

    /**
     * Mensagens de uma ocorrência em tempo real, da mais antiga para a mais recente (Parte B).
     * Só o autor e o operador as podem ler (regras): não chamar sem sessão ativa.
     * Com dados offline o snapshot pode chegar duas vezes (cache e depois servidor).
     */
    fun observeMessages(incidentId: String): Flow<List<Message>> = callbackFlow {
        val registration = incidents.document(incidentId).collection(MESSAGES)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Ex.: regras a recusar a leitura. Mantém-se a última lista.
                    Log.w(TAG, "Erro a ler as mensagens", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toMessage() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    companion object {
        private const val TAG = "MessagesRepository"
        const val MESSAGES = "messages"
    }
}

/**
 * Message -> Map, para gravar no Firestore (a Parte B pode acrescentar aqui o DocumentSnapshot.toMessage()).
 */
fun Message.toFirestoreMap(): Map<String, Any> = mapOf(
    "senderUid" to senderUid,
    "senderRole" to senderRole.name,
    "text" to text,
    "timestamp" to timestamp
)

/**
 * Documento do Firestore -> Message (o inverso de toFirestoreMap). Ignora documentos incompletos.
 */
fun DocumentSnapshot.toMessage(): Message? {
    val senderUid = getString("senderUid") ?: return null
    val senderRole = getString("senderRole")
        ?.let { runCatching { UserRole.valueOf(it) }.getOrNull() } ?: return null
    return Message(
        id = id,
        senderUid = senderUid,
        senderRole = senderRole,
        text = getString("text").orEmpty(),
        timestamp = getLong("timestamp") ?: 0L
    )
}
