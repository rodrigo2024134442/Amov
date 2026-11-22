package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Message
import pt.isec.amov.cityfix.model.UserRole

/**
 * Mensagens trocadas entre o munícipe e a câmara (Parte B), como no mockup v3: as do próprio
 * à direita, as do outro à esquerda, com o perfil de quem escreveu e a hora.
 * Numa ocorrência encerrada o campo de texto desaparece e fica a nota de arquivo.
 * Stateless: os dados e as ações vêm do MessagesViewModel.
 */
@Composable
fun MessagesSection(
    messages: List<Message>,
    currentUid: String?,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    closed: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.messages_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                // Acessibilidade: o TalkBack pode saltar de cabeçalho em cabeçalho
                modifier = Modifier.semantics { heading() }
            )
            if (messages.isEmpty()) {
                Text(
                    stringResource(R.string.messages_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // O detalhe já tem scroll: uma Column simples (não LazyColumn) com as mensagens
            messages.forEach { message ->
                MessageBubble(message, mine = message.senderUid == currentUid)
            }
            if (closed) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Text(stringResource(R.string.messages_closed), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        label = { Text(stringResource(R.string.messages_hint)) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.action_send))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, mine: Boolean) {
    val role = stringResource(
        if (message.senderRole == UserRole.OPERATOR) R.string.role_operator else R.string.role_citizen
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            // O cartão já é surfaceVariant: as do outro ficam em surface para se distinguirem
            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            // Acessibilidade: perfil, hora e texto lidos de uma só vez
            modifier = Modifier.widthIn(max = 280.dp).semantics(mergeDescendants = true) {}
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(
                    "$role · ${formatDateTime(message.timestamp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Botão "Reabrir" do autor (só aparece enquanto IncidentLifecycle.canReopen for verdadeiro).
 */
@Composable
fun ReopenButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Icon(Icons.Default.Replay, contentDescription = null)
        Text(stringResource(R.string.action_reopen), modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * Diálogo "Reabrir" com a justificação obrigatória. Stateless.
 */
@Composable
fun ReopenDialog(
    reason: String,
    error: Int?,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reopen_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.reopen_text))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text(stringResource(R.string.reopen_reason_hint)) },
                    isError = error != null,
                    supportingText = { error?.let { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_reopen)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/**
 * Botão "Pedir esclarecimento" do autor (enunciado II.2: o munícipe pode pedir esclarecimentos
 * sobre a estimativa temporal ou o procedimento).
 */
@Composable
fun ClarifyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
        Text(stringResource(R.string.action_ask_clarification), modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * Diálogo do pedido de esclarecimento: a pergunta é obrigatória. Stateless.
 */
@Composable
fun ClarifyDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clarify_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.clarify_text))
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    label = { Text(stringResource(R.string.clarify_hint)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = text.isNotBlank()) { Text(stringResource(R.string.action_send)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
