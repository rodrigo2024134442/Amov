package pt.isec.amov.cityfix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.model.Incident
import pt.isec.amov.cityfix.viewmodel.AuthorInfo

/**
 * Parte do detalhe dedicada ao ciclo de vida do ticket, pela ordem do mockup v3:
 *  - operador: resumo (estado, coordenadas, contactos do autor, alcance) → 3 botões → [Parte B] → histórico;
 *  - autor: progresso numerado → cartão com a prova, o aviso de reabertura/fecho e o histórico → [Parte B].
 * [partB] é o espaço para a secção de mensagens e o botão "Reabrir" do Marco, já no sítio do mockup.
 * Stateless: os dados e as ações vêm do TicketViewModel.
 */
@Composable
fun TicketSection(
    incident: Incident,
    isOperator: Boolean,
    author: AuthorInfo,
    onRequestInfo: (String) -> Unit,
    onStartWork: () -> Unit,
    onOpenResolve: () -> Unit,
    modifier: Modifier = Modifier,
    partB: @Composable ColumnScope.() -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isOperator) {
            OperatorSummaryCard(incident, author)
            OperatorActionsCard(
                incident = incident,
                onRequestInfo = onRequestInfo,
                onStartWork = onStartWork,
                onOpenResolve = onOpenResolve
            )
            partB()
            HistoryCard(incident, showNotice = false)
        } else {
            StatusStepper(incident.status)
            HistoryCard(incident, showNotice = true)
            partB()
        }
    }
}

/**
 * Cartão com a prova de resolução (se existir), o aviso de reabertura/fecho (só para o autor:
 * o operador já o vê no lugar dos botões) e o histórico.
 */
@Composable
private fun HistoryCard(incident: Incident, showNotice: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            incident.displayProof?.let { ProofBanner(it) }
            if (showNotice) LifecycleNotice(incident)
            HistoryList(incident.timeline)
        }
    }
}
