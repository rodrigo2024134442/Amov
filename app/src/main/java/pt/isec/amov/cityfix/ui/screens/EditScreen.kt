package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Ecrã stateless duplo (criação ou edição) para um incidente.
 */
@Composable
fun EditScreen(
    incidentId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (incidentId == null) "Registar Novo Incidente" else "Editar Incidente: $incidentId"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text("Voltar")
            }
        }
    }
}
