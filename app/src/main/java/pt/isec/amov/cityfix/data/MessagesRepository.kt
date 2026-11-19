package pt.isec.amov.cityfix.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.Session

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
