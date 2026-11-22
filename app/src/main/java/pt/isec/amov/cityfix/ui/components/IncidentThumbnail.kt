package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import pt.isec.amov.cityfix.utils.FileUtils
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R

/**
 * Miniatura de uma ocorrência na lista (o enunciado pede "categoria, situação e thumbnail").
 * Equivalente ao ContactAvatar da aula P09: imagem com Coil 3 ou, sem imagem, um ícone.
 */
@Composable
fun IncidentThumbnail(
    picturePath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val shape = RoundedCornerShape(8.dp)
    // Antes do envio, a miniatura é o primeiro ficheiro local, que pode ser um vídeo (o Coil não o lê)
    val isVideo = FileUtils.isVideo(picturePath)
    if (!picturePath.isNullOrBlank() && !isVideo) {
        AsyncImage(
            model = picturePath,
            contentDescription = stringResource(R.string.incident_photo),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Image,
                contentDescription = stringResource(if (isVideo) R.string.incident_video else R.string.no_media),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}