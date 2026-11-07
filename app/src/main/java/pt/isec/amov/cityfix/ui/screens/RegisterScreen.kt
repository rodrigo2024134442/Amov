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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.AppConfig
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.ui.components.PhotoSourceButtons
import pt.isec.amov.cityfix.ui.components.UserAvatar
import pt.isec.amov.cityfix.viewmodel.RegisterUiState

/**
 * Ecrã stateless de registo de munícipe ou operador (enunciado I.2.A e I.3.A).
 * O perfil não se escolhe: é o domínio do e-mail que o define (ver AppConfig).
 * A fotografia é opcional; a georreferenciação fica para a aula de GPS.
 */
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNifChange: (String) -> Unit,
    onImpactDetectionChange: (Boolean) -> Unit,
    onPhotoChange: (String) -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.operator_email_hint, AppConfig.OPERATOR_EMAIL_DOMAIN),
            style = MaterialTheme.typography.bodySmall
        )
        // Fotografia de perfil (opcional)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UserAvatar(name = state.name, photo = state.photoPath, size = 84.dp)
            Column {
                Text(stringResource(R.string.photo_optional), style = MaterialTheme.typography.bodySmall)
                PhotoSourceButtons(onPhotoPicked = onPhotoChange)
            }
        }
        RegisterField(state.name, onNameChange, R.string.field_name, state.nameError)
        RegisterField(state.email, onEmailChange, R.string.field_email, state.emailError, KeyboardType.Email)
        RegisterField(state.password, onPasswordChange, R.string.field_password, state.passwordError, KeyboardType.Password, isPassword = true)
        RegisterField(state.phone, onPhoneChange, R.string.field_phone, state.phoneError, KeyboardType.Phone)
        RegisterField(state.nif, onNifChange, R.string.field_nif, state.nifError, KeyboardType.Number)
        // Deteção de impactos e aviso de validação: só para munícipes
        if (state.role == UserRole.CITIZEN) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.field_impact_detection), modifier = Modifier.weight(1f))
                Switch(checked = state.impactDetection, onCheckedChange = onImpactDetectionChange)
            }
            Text(stringResource(R.string.register_pending_hint), style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(onClick = onRegister, enabled = state.isValid, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_register))
            }
        }
    }
}

/**
 * Um campo do formulário com a mensagem de erro por baixo (stateless, só usado aqui).
 */
@Composable
private fun RegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    error: Int?,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = { error?.let { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}