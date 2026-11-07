package pt.isec.amov.cityfix.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Otimização das imagens antes do envio para a nuvem (enunciado, secção V):
 * reduz o tamanho, corrige a rotação e comprime em JPEG.
 * Usado pelo MediaUploadWorker para a imagem "normal" e para a miniatura.
 */
object ImageUtils {

    /**
     * Lê a imagem em [path], reduz o lado maior a [maxSide] píxeis e devolve os bytes JPEG.
     * Devolve null se o ficheiro não existir ou não for uma imagem.
     */
    fun compress(path: String, maxSide: Int, quality: Int): ByteArray? {
        // 1. Só ler as dimensões (sem carregar a imagem inteira para a memória)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // 2. Carregar já reduzida por uma potência de 2 (poupa memória em fotografias grandes)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null

        // 3. Ajustar ao tamanho final exato
        val scale = maxSide.toFloat() / max(decoded.width, decoded.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).roundToInt(),
                (decoded.height * scale).roundToInt(),
                true
            )
        } else decoded

        // 4. Rodar conforme a orientação gravada pela câmara (EXIF)
        val rotated = rotateIfNeeded(path, scaled)

        // 5. Comprimir em JPEG
        return ByteArrayOutputStream().use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }

    private fun rotateIfNeeded(path: String, bitmap: Bitmap): Bitmap {
        val degrees = when (ExifInterface(path).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}