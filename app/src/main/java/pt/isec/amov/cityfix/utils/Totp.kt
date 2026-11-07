package pt.isec.amov.cityfix.utils

import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Códigos de 6 dígitos que mudam a cada 30 segundos (TOTP, RFC 6238), os mesmos que
 * o Google Authenticator ou o Microsoft Authenticator mostram. É o 2.º fator do login.
 *
 * A app e a app autenticadora partilham um segredo (criado no registo). Cada uma calcula
 * o código a partir do segredo e da hora atual; se coincidirem, o código está certo.
 */
object Totp {

    private const val PERIOD_SECONDS = 30L
    private const val DIGITS = 6
    private const val BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /**
     * Segredo novo: 20 bytes aleatórios, em Base32 (o formato que as apps autenticadoras leem).
     */
    fun generateSecret(): String {
        val bytes = ByteArray(20).also { SecureRandom().nextBytes(it) }
        return base32Encode(bytes)
    }

    /**
     * Endereço otpauth:// que se põe no QR code (ou que abre a app autenticadora neste telemóvel).
     */
    fun otpauthUri(secret: String, account: String, issuer: String = "CityFix"): String {
        val label = URLEncoder.encode("$issuer:$account", "UTF-8").replace("+", "%20")
        return "otpauth://totp/$label?secret=$secret&issuer=$issuer&digits=$DIGITS&period=$PERIOD_SECONDS"
    }

    /**
     * true se o código for o do intervalo atual ou de um dos vizinhos (±30 s),
     * para tolerar pequenas diferenças de relógio entre os dois telemóveis.
     */
    fun verify(secret: String, code: String, timeMillis: Long = System.currentTimeMillis()): Boolean {
        val counter = timeMillis / 1000 / PERIOD_SECONDS
        return (-1..1).any { codeAt(secret, counter + it) == code.trim() }
    }

    // Código para um intervalo: HMAC-SHA1 do contador, "truncagem dinâmica", 6 dígitos
    private fun codeAt(secret: String, counter: Long): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(base32Decode(secret), "HmacSHA1"))
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
        val offset = hash.last().toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)
        return (binary % 1_000_000).toString().padStart(DIGITS, '0')
    }

    private fun base32Encode(data: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in data) {
            buffer = (buffer shl 8) or (b.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                sb.append(BASE32[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) sb.append(BASE32[(buffer shl (5 - bits)) and 31])
        return sb.toString()
    }

    private fun base32Decode(text: String): ByteArray {
        val out = mutableListOf<Byte>()
        var buffer = 0
        var bits = 0
        for (ch in text.uppercase()) {
            val value = BASE32.indexOf(ch)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                out.add(((buffer shr (bits - 8)) and 0xff).toByte())
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}