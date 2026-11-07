package pt.isec.amov.cityfix.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Scope
import pt.isec.amov.cityfix.model.Situation

// Texto de cada valor dos enums, na língua do dispositivo (values/ ou values-pt/strings.xml).
// Fica na camada de UI para o modelo não depender de recursos Android.

@Composable
fun Situation.label(): String = stringResource(
    when (this) {
        Situation.DAMAGE -> R.string.situation_damage
        Situation.MALFUNCTION -> R.string.situation_malfunction
        Situation.DESTRUCTION -> R.string.situation_destruction
        Situation.THEFT -> R.string.situation_theft
    }
)

@Composable
fun Scope.label(): String = stringResource(
    when (this) {
        Scope.PUBLIC -> R.string.scope_public
        Scope.PRIVATE -> R.string.scope_private
    }
)

@Composable
fun IncidentStatus.label(): String = stringResource(
    when (this) {
        IncidentStatus.OPEN -> R.string.status_open
        IncidentStatus.INFO_REQUESTED -> R.string.status_info_requested
        IncidentStatus.IN_PROGRESS -> R.string.status_in_progress
        IncidentStatus.RESOLVED -> R.string.status_resolved
        IncidentStatus.CLOSED -> R.string.status_closed
    }
)