package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * Fotografia de perfil redonda, ou as iniciais do nome se não houver fotografia.
 * Igual ao ContactAvatar da aula P09; a fotografia pode ser um caminho local ou um endereço do Storage.
 */
@Composable
fun UserAvatar(
    name: String,
    photo: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    if (!photo.isNullOrBlank()) {
        AsyncImage(
            model = photo,
            contentDescription = stringResource(R.string.profile_photo),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        val initials = name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .map { it.first().uppercase() }
            .take(2)
            .joinToString("")
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifEmpty { "?" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}