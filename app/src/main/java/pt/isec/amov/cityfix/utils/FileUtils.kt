package pt.isec.amov.cityfix.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Funções auxiliares para os ficheiros de imagem guardados no armazenamento privado da app
 * (context.filesDir/images). Igual ao FileUtils da aula P09.
 */
object FileUtils {

    // Pasta das imagens; tem de coincidir com o path="images/" do res/xml/file_paths.xml
    private fun imagesDir(context: Context): File =
        File(context.filesDir, "images").apply { if (!exists()) mkdirs() }

    /**
     * Copia uma imagem escolhida na galeria (URI temporário) para a pasta privada da app.
     * Devolve o caminho permanente, ou null se a cópia falhar.
     */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            // Vídeo ou imagem, conforme o tipo indicado pela galeria (vídeos ficam em .mp4)
            val isVideoUri = context.contentResolver.getType(sourceUri)?.startsWith("video/") == true
            val name = if (isVideoUri) "VID_${UUID.randomUUID()}.mp4" else "IMG_${UUID.randomUUID()}.jpg"
            val destinationFile = File(imagesDir(context), name)
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Cria um ficheiro vazio para a câmara escrever, e o URI content:// (FileProvider) para lhe passar.
     */
    fun createTempImageFile(context: Context): Pair<File, Uri> {
        val tempFile = File(imagesDir(context), "CAPTURE_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(tempFile, uri)
    }

    /**
     * Como createTempImageFile, mas para a câmara gravar um vídeo (enunciado I.2.E: imagens e/ou vídeos).
     */
    fun createTempVideoFile(context: Context): Pair<File, Uri> {
        val tempFile = File(imagesDir(context), "CAPTURE_${System.currentTimeMillis()}.mp4")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
        return Pair(tempFile, uri)
    }

    /**
     * true se o caminho local ou o endereço do Storage é de um vídeo (.mp4).
     * Nos endereços do Storage o nome vem antes do "?" (ex.: .../video_0.mp4?alt=media&token=...).
     */
    fun isVideo(path: String?): Boolean =
        path?.substringBefore('?')?.lowercase()?.endsWith(".mp4") == true

    /**
     * Abre um vídeo (ficheiro local ou endereço do Storage) no leitor de vídeo do sistema.
     */
    fun openVideo(context: Context, path: String) {
        val uri = if (path.startsWith("http")) Uri.parse(path)
        else FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            .setDataAndType(uri, "video/*")
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Apaga o ficheiro indicado (se existir). Usado para não deixar imagens órfãs.
     */
    fun deleteFileFromPath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return try {
            val file = File(path)
            file.exists() && file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}