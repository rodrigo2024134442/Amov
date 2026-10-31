package pt.isec.amov.cityfix.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import pt.isec.amov.cityfix.ui.navigation.IncidentListRoute

/**
 * TopAppBar contextual que se adapta dependendo do ecrã atual na pilha de navegação.
 * Mostra botão de voltar em ecrãs de detalhe/edição e botões de alternância de vista/adição no ecrã principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    navController: NavHostController,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddIncident: () -> Unit
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val isList = navBackStackEntry?.destination?.hasRoute<IncidentListRoute>() == true

    TopAppBar(
        title = { Text("CityFix") },
        navigationIcon = {
            if (!isList) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }
            }
        },
        actions = {
            if (isList) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ViewAgenda else Icons.AutoMirrored.Filled.List,
                        contentDescription = "Alternar Visualização"
                    )
                }
                IconButton(onClick = onAddIncident) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Incidente"
                    )
                }
            }
        }
    )
}
