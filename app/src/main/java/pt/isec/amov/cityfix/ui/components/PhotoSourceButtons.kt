package pt.isec.amov.cityfix.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.utils.FileUtils

/**
 * Botões "Galeria" e "Câmara" para escolher UMA fotografia (a de perfil), como no EditScreen da aula P09.
 * Devolve o caminho da fotografia já copiada para a pasta privada da app.
 */
@Composable
fun PhotoSourceButtons(
    onPhotoPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tempCameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { FileUtils.copyUriToInternalStorage(context, it) }?.let(onPhotoPicked)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = tempCameraPath
        if (success && path != null) onPhotoPicked(path) else FileUtils.deleteFileFromPath(path)
        tempCameraPath = null
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text(stringResource(R.string.action_gallery)) }
        OutlinedButton(onClick = {
            val (file, uri) = FileUtils.createTempImageFile(context)
            tempCameraPath = file.absolutePath
            cameraLauncher.launch(uri)
        }) { Text(stringResource(R.string.action_camera)) }
    }
}