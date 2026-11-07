package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.User
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.ui.components.PhotoSourceButtons
import pt.isec.amov.cityfix.ui.components.UserAvatar

/**
 * Perfil (enunciado I.2.C e I.3.C): nome, NIF e e-mail só se mostram;
 * a fotografia, o telefone e a deteção de impactos podem ser alterados.
 * Os campos em edição são estado local do ecrã (rememberSaveable), iniciados com o perfil.
 */
@Composable
fun ProfileScreen(
    user: User,
    phoneError: Int?,
    message: Int?,
    onSave: (phone: String, impactDetection: Boolean, newPhotoPath: String?) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phone by rememberSaveable(user.uid) { mutableStateOf(user.phone) }
    var impactDetection by rememberSaveable(user.uid) { mutableStateOf(user.impactDetection) }
    // Fotografia nova escolhida neste ecrã (ainda só no telemóvel; é enviada ao gravar)
    var newPhotoPath by rememberSaveable(user.uid) { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UserAvatar(name = user.name, photo = newPhotoPath ?: user.photoUrl, size = 84.dp)
            PhotoSourceButtons(onPhotoPicked = { newPhotoPath = it })
        }
        Text(user.name, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(if (user.role == UserRole.CITIZEN) R.string.role_citizen else R.string.role_operator))
        Text(stringResource(R.string.label_email, user.email))
        Text(stringResource(R.string.label_nif, user.nif))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(R.string.field_phone)) },
            isError = phoneError != null,
            supportingText = { phoneError?.let { Text(stringResource(it)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (user.role == UserRole.CITIZEN) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.field_impact_detection), modifier = Modifier.weight(1f))
                Switch(checked = impactDetection, onCheckedChange = { impactDetection = it })
            }
        }
        message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(onClick = { onSave(phone, impactDetection, newPhotoPath) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_save))
        }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_sign_out))
        }
    }
}