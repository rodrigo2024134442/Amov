package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R

// Ecrãs mostrados pela MainActivity em vez da navegação, enquanto a sessão não está completa.

/**
 * Arranque, ou à espera do perfil no Firestore.
 */
@Composable
fun LoadingScreen(onCancel: () -> Unit, modifier: Modifier = Modifier) {
    CenteredColumn(modifier) {
        CircularProgressIndicator()
        TextButton(onClick = onCancel) { Text(stringResource(R.string.action_sign_out)) }
    }
}

/**
 * 2.º fator: o código de 6 dígitos enviado para o e-mail da conta.
 */
@Composable
fun MfaScreen(
    email: String,
    message: Int?,
    info: Int?,
    onConfirm: (String) -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenteredColumn(modifier) {
        Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(64.dp))
        Text(stringResource(R.string.mfa_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.mfa_text, email), textAlign = TextAlign.Center)
        CodeInput(message = message, onConfirm = onConfirm)
        info?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = onResend) { Text(stringResource(R.string.action_resend_code)) }
        OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.action_sign_out)) }
    }
}

/**
 * Campo para o código de 6 dígitos e botão de confirmar.
 */
@Composable
private fun CodeInput(message: Int?, onConfirm: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = code,
        onValueChange = { new -> code = new.filter { it.isDigit() }.take(6) },
        label = { Text(stringResource(R.string.field_code)) },
        isError = message != null,
        supportingText = { message?.let { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = { onConfirm(code) }, enabled = code.length == 6, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_confirm))
    }
}

/**
 * Munícipe registado mas ainda não validado pelo operador.
 */
@Composable
fun PendingScreen(name: String, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    CenteredColumn(modifier) {
        Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(64.dp))
        Text(stringResource(R.string.pending_title, name), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(stringResource(R.string.pending_text), textAlign = TextAlign.Center)
        OutlinedButton(onClick = onSignOut) { Text(stringResource(R.string.action_sign_out)) }
    }
}

@Composable
private fun CenteredColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}