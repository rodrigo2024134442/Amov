package pt.isec.amov.cityfix.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Envia o código do 2.º fator por e-mail, só com o Firebase: grava um documento na coleção
 * "mail" do Firestore e a extensão "Trigger Email from Firestore" (instalada na consola)
 * envia-o por SMTP. As regras só deixam criar documentos para o e-mail da própria conta
 * e ninguém os pode ler na app.
 */
class EmailCodeSender {

    private val mailCollection = Firebase.firestore.collection("mail")

    /**
     * Pede o envio do código para o e-mail indicado. Lança uma exceção se o pedido
     * não chegar ao Firestore (sem rede durante TIMEOUT_MS ou recusado pelas regras).
     */
    suspend fun send(toEmail: String, code: String, validityMinutes: Int) {
        val text = "CityFix\n\nCódigo de verificação / Verification code: $code\n\n" +
            "Válido durante $validityMinutes minutos. / Valid for $validityMinutes minutes."
        val mail = mapOf(
            "to" to toEmail,
            "message" to mapOf(
                "subject" to "CityFix: $code",
                "text" to text
            )
        )
        // Aqui espera-se pela confirmação do servidor: o utilizador está à espera do e-mail
        withTimeout(TIMEOUT_MS) { mailCollection.add(mail).await() }
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L
    }
}
