package pt.isec.amov.cityfix.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation
import pt.isec.amov.cityfix.ui.components.DropdownField
import pt.isec.amov.cityfix.ui.components.IncidentPhotoGallery
import pt.isec.amov.cityfix.ui.components.MapLocationPicker
import pt.isec.amov.cityfix.ui.components.label
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.viewmodel.EditIncidentUiState

/**
 * Ecrã stateless duplo (criação ou edição) para um incidente.
 * Recebe o estado já pronto e devolve cada alteração ao ViewModel pelos callbacks.
 * Inclui seleção de morada e georreferenciação por mapa (Ficha 10).
 */
@Composable
fun EditScreen(
    state: EditIncidentUiState,
    categories: List<Category>,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSituationChange: (Situation) -> Unit,
    onScopeChange: (Scope) -> Unit,
    onMediaAttached: (List<String>) -> Unit,
    onMediaRemoved: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onLocationSelected: (lat: Double, lng: Double, address: String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tempCameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = EditIncidentUiState.MAX_MEDIA)
    ) { uris ->
        val saved = uris.mapNotNull { FileUtils.copyUriToInternalStorage(context, it) }
        if (saved.isNotEmpty()) onMediaAttached(saved)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = tempCameraPath
        if (success && path != null) {
            onMediaAttached(listOf(path))
        } else {
            FileUtils.deleteFileFromPath(path)
        }
        tempCameraPath = null
    }

    Scaffold(
        modifier = modifier.imePadding(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSaveClick,
                containerColor = if (state.isValid) MaterialTheme.colorScheme.primary else
                    MaterialTheme.colorScheme.surfaceVariant
            ) { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_save)) }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.label_photos, state.mediaPaths.size, EditIncidentUiState.MAX_MEDIA),
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = state.remainingMedia > 0
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_gallery))
                }
                OutlinedButton(
                    onClick = {
                        val (file, uri) = FileUtils.createTempImageFile(context)
                        tempCameraPath = file.absolutePath
                        cameraLauncher.launch(uri)
                    },
                    enabled = state.remainingMedia > 0
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_camera))
                }
            }
            if (state.mediaPaths.isEmpty()) {
                Text(
                    text = stringResource(R.string.error_media_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                IncidentPhotoGallery(photos = state.mediaPaths, onRemove = onMediaRemoved)
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.field_title)) },
                isError = state.titleError != null,
                supportingText = { state.titleError?.let { Text(stringResource(it)) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.field_description)) },
                isError = state.descriptionError != null,
                supportingText = { state.descriptionError?.let { Text(stringResource(it)) } },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            DropdownField(
                label = stringResource(R.string.field_category),
                options = categories.map { it.name },
                selected = state.category.ifBlank { null },
                optionLabel = { it },
                onSelected = onCategoryChange,
                modifier = Modifier.fillMaxWidth()
            )
            DropdownField(
                label = stringResource(R.string.field_situation),
                options = Situation.entries,
                selected = state.situation,
                optionLabel = { it.label() },
                onSelected = onSituationChange,
                modifier = Modifier.fillMaxWidth()
            )
            DropdownField(
                label = stringResource(R.string.field_scope),
                options = Scope.entries,
                selected = state.scope,
                optionLabel = { it.label() },
                onSelected = onScopeChange,
                modifier = Modifier.fillMaxWidth()
            )

            // Localização e Mapa (Ficha 10)
            OutlinedTextField(
                value = state.address,
                onValueChange = onAddressChange,
                label = { Text(stringResource(R.string.field_address)) },
                trailingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.label_select_location),
                style = MaterialTheme.typography.labelMedium
            )
            MapLocationPicker(
                initialLat = state.latitude,
                initialLng = state.longitude,
                onLocationSelected = onLocationSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
