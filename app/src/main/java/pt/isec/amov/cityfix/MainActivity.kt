package pt.isec.amov.cityfix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import pt.isec.amov.cityfix.ui.components.MainTopAppBar
import pt.isec.amov.cityfix.ui.navigation.EditIncidentRoute
import pt.isec.amov.cityfix.ui.navigation.IncidentDetailsRoute
import pt.isec.amov.cityfix.ui.navigation.IncidentListRoute
import pt.isec.amov.cityfix.ui.screens.EditScreen
import pt.isec.amov.cityfix.ui.screens.ListScreen
import pt.isec.amov.cityfix.ui.screens.ShowScreen
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.IncidentsViewModel
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CityFixTheme {
                val viewModel: IncidentsViewModel = viewModel()
                val navController = rememberNavController()
                var isExpanded by rememberSaveable { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MainTopAppBar(
                            navController = navController,
                            isExpanded = isExpanded,
                            onToggleExpand = { isExpanded = !isExpanded },
                            onAddIncident = { navController.navigate(EditIncidentRoute(null)) }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = IncidentListRoute,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable<IncidentListRoute> {
                            val incidents by viewModel.incidentsList.collectAsState()
                            ListScreen(
                                incidents = incidents,
                                isExpanded = isExpanded,
                                onIncidentClick = { id ->
                                    navController.navigate(IncidentDetailsRoute(id))
                                }
                            )
                        }
                        composable<IncidentDetailsRoute> { backStack ->
                            val route = backStack.toRoute<IncidentDetailsRoute>()
                            val incident = viewModel.getIncidentById(route.incidentId)
                            ShowScreen(
                                incident = incident,
                                onEditClick = { id ->
                                    navController.navigate(EditIncidentRoute(id))
                                }
                            )
                        }
                        composable<EditIncidentRoute> { backStack ->
                            val route = backStack.toRoute<EditIncidentRoute>()
                            EditScreen(
                                incidentId = route.incidentId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
