package pt.isec.amov.cityfix.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.utils.QrCode
import pt.isec.amov.cityfix.utils.Totp

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
 * Configurar o 2.º fator (logo a seguir ao registo): QR code e chave para a app autenticadora,
 * e o primeiro código para confirmar que ficou bem configurada.
 */
@Composable
fun MfaSetupScreen(
    email: String,
    secret: String,
    message: Int?,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uri = remember(secret, email) { Totp.otpauthUri(secret, email) }
    val qr = remember(uri) { QrCode.generate(uri) }
    var noAuthenticator by rememberSaveable { mutableStateOf(false) }

    CenteredColumn(modifier) {
        Text(stringResource(R.string.mfa_setup_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(stringResource(R.string.mfa_setup_text), textAlign = TextAlign.Center)
        Image(bitmap = qr, contentDescription = stringResource(R.string.mfa_qr_description), modifier = Modifier.size(200.dp))
        // A chave também pode ser escrita à mão na app autenticadora (selecionável para copiar)
        SelectionContainer {
            Text(secret.chunked(4).joinToString(" "), fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
        }
        // Com a app autenticadora no mesmo telemóvel, o QR não serve: abre-se o endereço otpauth://
        OutlinedButton(onClick = {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
            } catch (_: ActivityNotFoundException) {
                noAuthenticator = true
            }
        }) { Text(stringResource(R.string.action_open_authenticator)) }
        if (noAuthenticator) Text(stringResource(R.string.no_authenticator), color = MaterialTheme.colorScheme.error)
        CodeInput(message = message, onConfirm = onConfirm)
        TextButton(onClick = onCancel) { Text(stringResource(R.string.action_sign_out)) }
    }
}

/**
 * 2.º fator no login: o código atual da app autenticadora.
 */
@Composable
fun MfaScreen(
    email: String,
    message: Int?,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenteredColumn(modifier) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp))
        Text(stringResource(R.string.mfa_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.mfa_text, email), textAlign = TextAlign.Center)
        CodeInput(message = message, onConfirm = onConfirm)
        OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.action_sign_out)) }
    }
}

/**
 * Campo para o código de 6 dígitos e botão de confirmar (partilhado pelos dois ecrãs do 2.º fator).
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