package pt.isec.amov.cityfix.model

/**
 * Mensagem trocada entre o munícipe e os serviços camarários sobre uma ocorrência
 * (enunciado I.2.E e II.2). Guardada em incidents/{incidentId}/messages/{id}.
 *
 * BASE COMBINADA ENTRE A PARTE A E A PARTE B: a Parte A só precisa de enviar
 * (o "Pedir dados" do operador). A Parte B acrescenta o resto (ler em tempo real, ecrã).
 */
data class Message(
    // ID do documento no Firestore
    val id: String = "",

    // Quem escreveu (uid)
    val senderUid: String,

    // Perfil de quem escreveu: o ecrã mostra "Câmara" ou "Munícipe"
    val senderRole: UserRole,

    // Texto da mensagem
    val text: String,

    // Quando (Unix Epoch em milissegundos)
    val timestamp: Long = System.currentTimeMillis()
)
