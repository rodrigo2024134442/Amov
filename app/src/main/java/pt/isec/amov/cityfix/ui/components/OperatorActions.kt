package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentLifecycle
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.viewmodel.AuthorInfo
import pt.isec.amov.cityfix.utils.FileUtils
import java.util.Locale

// Componentes do "Painel de Gestão Operacional" (enunciado I.3.E), com o aspeto do mockup v3:
// cartão de resumo (categoria · situação + estado, coordenadas, contactos do autor, alcance)
// e os 3 botões do ciclo de vida numa linha (pedir dados, iniciar obra, resolver + prova).

/**
 * Resumo da ocorrência para o operador, como no topo do detalhe do mockup.
 * Os contactos do autor só aparecem aqui (o operador é o único que os pode ler).
 */
@Composable
fun OperatorSummaryCard(incident: Incident, author: AuthorInfo, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${incident.category} · ${incident.situation.label()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(incident.status)
            }
            if (incident.latitude != null && incident.longitude != null) {
                InfoLine(
                    Icons.Default.LocationOn,
                    String.format(Locale.ROOT, "%.4f, %.4f", incident.latitude, incident.longitude)
                )
            }
            when (author) {
                AuthorInfo.Loading -> InfoLine(Icons.Default.Person, stringResource(R.string.author_loading))
                AuthorInfo.Unknown -> InfoLine(Icons.Default.Person, stringResource(R.string.author_unknown))
                is AuthorInfo.Known -> {
                    val user = author.user
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InfoLine(Icons.Default.Person, "${user.name} • ${user.phone}", Modifier.weight(1f))
                        if (user.phone.isNotBlank()) {
                            TextButton(onClick = { uriHandler.openUri("tel:${user.phone}") }) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(stringResource(R.string.action_call), modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                    InfoLine(Icons.Default.Email, user.email)
                }
            }
            InfoLine(
                if (incident.scope == Scope.PRIVATE) Icons.Default.Lock else Icons.Default.Public,
                incident.scope.label()
            )
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Os 3 botões do ciclo de vida numa linha, com as cores do mockup (âmbar, índigo, verde).
 * Só ficam ativos os que levam a uma transição válida (IncidentLifecycle).
 * Resolvida ou encerrada: em vez dos botões, o aviso de reabertura/bloqueio.
 * O diálogo "Resolver" é mostrado pela rota do detalhe (ResolveProofDialog), para sobreviver à câmara;
 * aqui o botão só pede para o abrir ([onOpenResolve]).
 */
@Composable
fun OperatorActionsCard(
    incident: Incident,
    onRequestInfo: (String) -> Unit,
    onStartWork: () -> Unit,
    onOpenResolve: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Diálogo aberto: nenhum, "info" ou "start" (sobrevive à rotação do ecrã)
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val status = incident.status

    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            if (status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED) {
                LifecycleNotice(incident)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionButton(
                        text = R.string.action_request_info,
                        container = Color(0xFFFEF3C7), content = Color(0xFF78350F),
                        enabled = IncidentLifecycle.canTransition(status, IncidentStatus.INFO_REQUESTED),
                        onClick = { dialog = DIALOG_INFO },
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = R.string.action_start_work,
                        container = Color(0xFFE0E7FF), content = Color(0xFF3730A3),
                        enabled = IncidentLifecycle.canTransition(status, IncidentStatus.IN_PROGRESS),
                        onClick = { dialog = DIALOG_START },
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = R.string.action_resolve,
                        container = Color(0xFF059669), content = Color.White,
                        enabled = IncidentLifecycle.canTransition(status, IncidentStatus.RESOLVED),
                        onClick = onOpenResolve,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    when (dialog) {
        DIALOG_INFO -> RequestInfoDialog(
            onConfirm = { question -> onRequestInfo(question); dialog = null },
            onDismiss = { dialog = null }
        )
        DIALOG_START -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.start_work_title)) },
            text = { Text(stringResource(R.string.start_work_text)) },
            confirmButton = {
                TextButton(onClick = { onStartWork(); dialog = null }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/**
 * Botão colorido do mockup; desativado fica esbatido. Altura mínima de 48 dp (acessibilidade).
 */
@Composable
private fun ActionButton(
    text: Int,
    container: Color,
    content: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.4f),
            disabledContentColor = content.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text(
            stringResource(text),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * "Pedir dados": a pergunta é obrigatória (fica no histórico e segue como mensagem para o munícipe).
 */
@Composable
private fun RequestInfoDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var question by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.request_info_title)) },
        text = {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text(stringResource(R.string.request_info_hint)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(question.trim()) }, enabled = question.isNotBlank()) {
                Text(stringResource(R.string.action_send))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/**
 * "Resolver": fotografia ou vídeo comprovativo obrigatório (galeria, câmara ou vídeo) e nota opcional (enunciado II.4).
 * Stateless: a fotografia e a nota vêm do TicketViewModel (sobrevivem à recriação da app pela câmara).
 * [canConfirm] é false enquanto a ocorrência ainda não voltou a carregar.
 */
@Composable
fun ResolveProofDialog(
    proofPath: String?,
    note: String,
    onNoteChange: (String) -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    canConfirm: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.resolve_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.resolve_text))
                if (proofPath != null && FileUtils.isVideo(proofPath)) {
                    VideoTile(proofPath, Modifier.height(160.dp))
                } else if (proofPath != null) {
                    AsyncImage(
                        model = proofPath,
                        contentDescription = stringResource(R.string.proof_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Button(onClick = onPickGallery) { Text(stringResource(R.string.action_gallery)) }
                OutlinedButton(onClick = onTakePhoto) { Text(stringResource(R.string.action_camera)) }
                OutlinedButton(onClick = onRecordVideo) { Text(stringResource(R.string.action_record_video)) }
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.resolve_note_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = proofPath != null && canConfirm) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

private const val DIALOG_INFO = "info"
private const val DIALOG_START = "start"
