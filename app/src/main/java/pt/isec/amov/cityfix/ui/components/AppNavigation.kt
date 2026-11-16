package pt.isec.amov.cityfix.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import pt.isec.amov.cityfix.R

/**
 * Separadores principais da app (como no mockup): o dashboard e a lista de ocorrências.
 */
enum class TopLevelDestination(val icon: ImageVector, @StringRes val labelRes: Int) {
    DASHBOARD(Icons.Default.Dashboard, R.string.nav_dashboard),
    INCIDENTS(Icons.AutoMirrored.Filled.List, R.string.nav_incidents)
}

/**
 * Barra inferior (orientação portrait). Stateless: o separador ativo vem de fora.
 * O ícone não tem contentDescription porque o texto do separador já o descreve.
 */
@Composable
fun AppBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) }
            )
        }
    }
}

/**
 * Barra lateral (orientação landscape), com os mesmos separadores.
 */
@Composable
fun AppNavigationRail(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) }
            )
        }
    }
}
