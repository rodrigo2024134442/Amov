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
            val destinationFile = File(imagesDir(context), "IMG_${UUID.randomUUID()}.jpg")
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