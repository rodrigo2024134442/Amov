package pt.isec.amov.cityfix.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.ui.navigation.IncidentListRoute

/**
 * TopAppBar contextual que se adapta ao ecrã atual e a quem está a usar a app.
 * No ecrã principal: alternar vista e, conforme o perfil, entrar (anónimo), reportar (munícipe),
 * validar munícipes e gerir categorias (operador) e o perfil (com sessão).
 * Nos outros ecrãs: só o botão de voltar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    navController: NavHostController,
    session: Session,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddIncident: () -> Unit,
    onLogin: () -> Unit,
    onProfile: () -> Unit,
    onValidateUsers: () -> Unit,
    onCategories: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isList = navBackStackEntry?.destination?.hasRoute<IncidentListRoute>() == true
    val user = (session as? Session.Active)?.user

    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        navigationIcon = {
            if (!isList) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        actions = {
            if (isList) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ViewAgenda else Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.action_toggle_view)
                    )
                }
                when (user?.role) {
                    null -> IconButton(onClick = onLogin) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = stringResource(R.string.action_login))
                    }
                    UserRole.CITIZEN -> IconButton(onClick = onAddIncident) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_incident))
                    }
                    UserRole.OPERATOR -> {
                        IconButton(onClick = onValidateUsers) {
                            Icon(Icons.Default.HowToReg, contentDescription = stringResource(R.string.action_validate_users))
                        }
                        IconButton(onClick = onCategories) {
                            Icon(Icons.Default.Category, contentDescription = stringResource(R.string.action_categories))
                        }
                    }
                }
                if (user != null) {
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.action_profile))
                    }
                }
            }
        }
    )
}