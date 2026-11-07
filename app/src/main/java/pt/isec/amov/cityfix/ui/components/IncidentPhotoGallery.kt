package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R

/**
 * Galeria das fotografias de uma ocorrência (slide "FixIt Project Bridge 2/3" da aula P09).
 * Stateless: recebe os caminhos e, se onRemove não for null, mostra um botão para retirar cada uma.
 * A altura máxima é fixa porque a grelha fica dentro de uma Column com scroll.
 */
@Composable
fun IncidentPhotoGallery(
    photos: List<String>,
    modifier: Modifier = Modifier,
    onRemove: ((String) -> Unit)? = null
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
    ) {
        items(photos, key = { it }) { path ->
            Box {
                AsyncImage(
                    model = path,
                    contentDescription = stringResource(R.string.incident_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .aspectRatio(1f)
                )
                if (onRemove != null) {
                    IconButton(
                        onClick = { onRemove(path) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_remove_photo))
                    }
                }
            }
        }
    }
}