package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.model.IncidentLifecycle
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.StatusChange
import java.text.DateFormat
import java.util.Date

// Componentes stateless do ciclo de vida, com o aspeto do mockup v3 (docs/cityfix_mockup_v3.html):
// etiqueta colorida do estado, progresso numerado, aviso de reabertura/fecho, faixa da prova e histórico.

/**
 * Cores das etiquetas de estado, iguais às do mockup (fundo claro e texto escuro: legível também no tema escuro).
 */
private fun statusColors(status: IncidentStatus): Pair<Color, Color> = when (status) {
    IncidentStatus.OPEN -> Color(0xFFE0F2FE) to Color(0xFF075985)
    IncidentStatus.INFO_REQUESTED -> Color(0xFFFEF3C7) to Color(0xFF78350F)
    IncidentStatus.IN_PROGRESS -> Color(0xFFE0E7FF) to Color(0xFF3730A3)
    IncidentStatus.RESOLVED -> Color(0xFFD1FAE5) to Color(0xFF064E3B)
    IncidentStatus.CLOSED -> Color(0xFFE2E8F0) to Color(0xFF334155)
}

/**
 * Etiqueta do estado (ex.: "Em Resolução" em fundo índigo claro).
 */
@Composable
fun StatusBadge(status: IncidentStatus, modifier: Modifier = Modifier) {
    val (background, content) = statusColors(status)
    Text(
        text = status.label(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = content,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(1.dp, content.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/**
 * Progresso do ticket para o autor: 5 círculos numerados (enunciado, secção II); até ao atual ficam preenchidos.
 */
@Composable
fun StatusStepper(status: IncidentStatus, modifier: Modifier = Modifier) {
    val current = IncidentStatus.entries.indexOf(status)
    Card(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(10.dp)) {
            IncidentStatus.entries.forEachIndexed { index, step ->
                val reached = index <= current
                Column(
                    modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (reached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        step.label(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (index == current) FontWeight.Bold else FontWeight.Normal,
                        color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/**
 * Aviso do fim do ciclo: Resolvido (pode ser reaberta até…) ou Encerrado (arquivado e bloqueado).
 */
@Composable
fun LifecycleNotice(incident: Incident, modifier: Modifier = Modifier) {
    val (icon, text) = when (incident.status) {
        IncidentStatus.RESOLVED -> Icons.Default.Schedule to (
            IncidentLifecycle.closesAt(incident)?.let { stringResource(R.string.reopen_until, formatDateTime(it)) }
                ?: return
            )
        IncidentStatus.CLOSED -> Icons.Default.Lock to stringResource(R.string.closed_locked)
        else -> return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Faixa verde da prova de resolução (enunciado II.4), como no mockup, com a miniatura da fotografia.
 */
@Composable
fun ProofBanner(proof: String, modifier: Modifier = Modifier) {
    val (background, content) = statusColors(IncidentStatus.RESOLVED)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (pt.isec.amov.cityfix.utils.FileUtils.isVideo(proof)) {
            VideoTile(proof, Modifier.size(56.dp))
        } else {
            AsyncImage(
                model = proof,
                contentDescription = stringResource(R.string.proof_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
        Icon(Icons.Default.Verified, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(
            stringResource(R.string.proof_title),
            style = MaterialTheme.typography.bodySmall,
            color = content,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Histórico (mockup: "HISTÓRICO"): cada mudança com a etiqueta do estado e a nota, e a data/hora à direita.
 */
@Composable
fun HistoryList(timeline: List<StatusChange>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.history_title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        timeline.forEach { change ->
            Row(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    StatusBadge(change.status)
                    change.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (change.byUid == StatusChange.SYSTEM) {
                        Text(stringResource(R.string.history_automatic), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    formatDateTime(change.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Data e hora no formato curto do idioma do dispositivo (ex.: 18/11/26, 23:59).
 */
@Composable
fun formatDateTime(millis: Long): String {
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    return remember(millis) { format.format(Date(millis)) }
}
