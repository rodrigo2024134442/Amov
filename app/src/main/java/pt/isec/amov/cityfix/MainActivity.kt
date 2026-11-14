package pt.isec.amov.cityfix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.ui.components.MainTopAppBar
import pt.isec.amov.cityfix.ui.navigation.CategoriesRoute
import pt.isec.amov.cityfix.ui.navigation.EditIncidentRoute
import pt.isec.amov.cityfix.ui.navigation.IncidentDetailsRoute
import pt.isec.amov.cityfix.ui.navigation.IncidentListRoute
import pt.isec.amov.cityfix.ui.navigation.LoginRoute
import pt.isec.amov.cityfix.ui.navigation.ProfileRoute
import pt.isec.amov.cityfix.ui.navigation.RegisterRoute
import pt.isec.amov.cityfix.ui.navigation.ValidateUsersRoute
import pt.isec.amov.cityfix.ui.screens.CategoriesScreen
import pt.isec.amov.cityfix.ui.screens.EditScreen
import pt.isec.amov.cityfix.ui.screens.ListScreen
import pt.isec.amov.cityfix.ui.screens.LoadingScreen
import pt.isec.amov.cityfix.ui.screens.LoginScreen
import pt.isec.amov.cityfix.ui.screens.MfaScreen
import pt.isec.amov.cityfix.ui.screens.MfaSetupScreen
import pt.isec.amov.cityfix.ui.screens.PendingScreen
import pt.isec.amov.cityfix.ui.screens.ProfileScreen
import pt.isec.amov.cityfix.ui.screens.RegisterScreen
import pt.isec.amov.cityfix.ui.screens.ShowScreen
import pt.isec.amov.cityfix.ui.screens.ValidateUsersScreen
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.viewmodel.EditIncidentViewModel
import pt.isec.amov.cityfix.viewmodel.EditIncidentViewModelFactory
import pt.isec.amov.cityfix.viewmodel.IncidentsViewModel
import pt.isec.amov.cityfix.viewmodel.IncidentsViewModelFactory
import pt.isec.amov.cityfix.viewmodel.LoginViewModel
import pt.isec.amov.cityfix.viewmodel.LoginViewModelFactory
import pt.isec.amov.cityfix.viewmodel.OperatorViewModel
import pt.isec.amov.cityfix.viewmodel.OperatorViewModelFactory
import pt.isec.amov.cityfix.viewmodel.RegisterViewModel
import pt.isec.amov.cityfix.viewmodel.RegisterViewModelFactory
import pt.isec.amov.cityfix.viewmodel.SessionViewModel
import pt.isec.amov.cityfix.viewmodel.SessionViewModelFactory

class MainActivity : ComponentActivity() {

    private val app get() = application as CityFixApp

    // ViewModel da sessão, associado à atividade
    private val sessionViewModel: SessionViewModel by viewModels {
        SessionViewModelFactory(app.authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CityFixTheme {
                val session by sessionViewModel.session.collectAsStateWithLifecycle()
                val mfaMessage by sessionViewModel.mfaMessage.collectAsStateWithLifecycle()

                // Enquanto a sessão não está completa, mostra-se um ecrã próprio em vez da navegação
                when (val s = session) {
                    Session.Loading -> Scaffold { p ->
                        LoadingScreen(onCancel = sessionViewModel::signOut, modifier = Modifier.padding(p))
                    }
                    is Session.MfaSetup -> Scaffold { p ->
                        MfaSetupScreen(
                            email = s.email,
                            secret = sessionViewModel.newMfaSecret,
                            message = mfaMessage,
                            onConfirm = sessionViewModel::confirmMfaSetup,
                            onCancel = sessionViewModel::signOut,
                            modifier = Modifier.padding(p)
                        )
                    }
                    is Session.AwaitingMfa -> Scaffold { p ->
                        MfaScreen(
                            email = s.email,
                            message = mfaMessage,
                            onConfirm = sessionViewModel::verifyMfa,
                            onCancel = sessionViewModel::signOut,
                            modifier = Modifier.padding(p)
                        )
                    }
                    is Session.PendingValidation -> Scaffold { p ->
                        PendingScreen(s.user.name, onSignOut = sessionViewModel::signOut, modifier = Modifier.padding(p))
                    }
                    // key: ao passar de anónimo a sessão ativa (ou vice-versa) a navegação recomeça na lista
                    Session.Guest, is Session.Active -> key(s is Session.Active) {
                        MainNavigation(s)
                    }
                }
            }
        }
    }

    /**
     * Navegação da app (anónimo ou sessão ativa), igual à das secções anteriores
     * mais as rotas de login, registo, perfil e do operador.
     */
    @Composable
    private fun MainNavigation(session: Session) {
        val viewModel: IncidentsViewModel = viewModel(
            factory = IncidentsViewModelFactory(app.repository, app.authRepository)
        )
        val operatorViewModel: OperatorViewModel = viewModel(
            factory = OperatorViewModelFactory(app.authRepository, app.categoriesRepository)
        )
        val navController = rememberNavController()
        var isExpanded by rememberSaveable { mutableStateOf(false) }
        val user = (session as? Session.Active)?.user

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MainTopAppBar(
                    navController = navController,
                    session = session,
                    isExpanded = isExpanded,
                    onToggleExpand = { isExpanded = !isExpanded },
                    onAddIncident = { navController.navigate(EditIncidentRoute(null)) },
                    onLogin = { navController.navigate(LoginRoute) },
                    onProfile = { navController.navigate(ProfileRoute) },
                    onValidateUsers = { navController.navigate(ValidateUsersRoute) },
                    onCategories = { navController.navigate(CategoriesRoute) }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = IncidentListRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<IncidentListRoute> {
                    val incidents by viewModel.incidentsList.collectAsStateWithLifecycle()
                    val filter by viewModel.filter.collectAsStateWithLifecycle()
                    ListScreen(
                        incidents = incidents,
                        isExpanded = isExpanded,
                        onIncidentClick = { id -> navController.navigate(IncidentDetailsRoute(id)) },
                        // Só o munícipe escolhe entre as suas e as públicas
                        filter = if (user?.role == UserRole.CITIZEN) filter else null,
                        onFilterChange = viewModel::onFilterChanged
                    )
                }
                composable<IncidentDetailsRoute> { backStack ->
                    val route = backStack.toRoute<IncidentDetailsRoute>()
                    // Ler a lista faz este ecrã atualizar-se quando se volta da edição
                    val incidents by viewModel.incidentsList.collectAsStateWithLifecycle()
                    val incident = incidents.find { it.id == route.incidentId }
                    val isOwner = user != null && incident?.userId == user.uid
                    ShowScreen(
                        incident = incident,
                        canEdit = isOwner,
                        showLocation = isOwner || user?.role == UserRole.OPERATOR,
                        onEditClick = { id -> navController.navigate(EditIncidentRoute(id)) },
                        onDeleteClick = { id -> viewModel.deleteIncident(id) { navController.popBackStack() } }
                    )
                }
                composable<EditIncidentRoute> { backStack ->
                    val route = backStack.toRoute<EditIncidentRoute>()
                    // Associado só a este destino: sobrevive à rotação, desaparece ao sair
                    val editViewModel: EditIncidentViewModel = viewModel(
                        viewModelStoreOwner = backStack,
                        factory = EditIncidentViewModelFactory(
                            app.repository, app.categoriesRepository, app.authRepository, route.incidentId
                        )
                    )
                    val editState by editViewModel.editState.collectAsStateWithLifecycle()
                    val categories by editViewModel.categories.collectAsStateWithLifecycle()
                    EditScreen(
                        state = editState,
                        categories = categories,
                        onTitleChange = editViewModel::onTitleChanged,
                        onDescriptionChange = editViewModel::onDescriptionChanged,
                        onCategoryChange = editViewModel::onCategoryChanged,
                        onSituationChange = editViewModel::onSituationChanged,
                        onScopeChange = editViewModel::onScopeChanged,
                        onMediaAttached = editViewModel::onMediaAttached,
                        onMediaRemoved = editViewModel::onMediaRemoved,
                        onAddressChange = editViewModel::onAddressChanged,
                        onLocationSelected = editViewModel::onLocationSelected,
                        onSaveClick = {
                            // A lista atualiza-se sozinha (snapshot listener do Firestore)
                            editViewModel.saveIncident { navController.popBackStack() }
                        }
                    )
                }
                composable<LoginRoute> { backStack ->
                    val loginViewModel: LoginViewModel = viewModel(
                        viewModelStoreOwner = backStack,
                        factory = LoginViewModelFactory(app.authRepository)
                    )
                    val state by loginViewModel.state.collectAsStateWithLifecycle()
                    LoginScreen(
                        state = state,
                        onEmailChange = loginViewModel::onEmailChanged,
                        onPasswordChange = loginViewModel::onPasswordChanged,
                        onSignIn = loginViewModel::signIn,
                        onForgotPassword = loginViewModel::resetPassword,
                        onRegister = { navController.navigate(RegisterRoute) }
                    )
                }
                composable<RegisterRoute> { backStack ->
                    val registerViewModel: RegisterViewModel = viewModel(
                        viewModelStoreOwner = backStack,
                        factory = RegisterViewModelFactory(app.authRepository)
                    )
                    val state by registerViewModel.state.collectAsStateWithLifecycle()
                    RegisterScreen(
                        state = state,
                        onNameChange = registerViewModel::onNameChanged,
                        onEmailChange = registerViewModel::onEmailChanged,
                        onPasswordChange = registerViewModel::onPasswordChanged,
                        onPhoneChange = registerViewModel::onPhoneChanged,
                        onNifChange = registerViewModel::onNifChanged,
                        onImpactDetectionChange = registerViewModel::onImpactDetectionChanged,
                        onPhotoChange = registerViewModel::onPhotoChanged,
                        onRegister = registerViewModel::register
                    )
                }
                composable<ProfileRoute> {
                    val phoneError by sessionViewModel.profilePhoneError.collectAsStateWithLifecycle()
                    val profileMessage by sessionViewModel.profileMessage.collectAsStateWithLifecycle()
                    if (user != null) {
                        ProfileScreen(
                            user = user,
                            phoneError = phoneError,
                            message = profileMessage,
                            onSave = { phone, impact, photo ->
                                sessionViewModel.saveProfile(phone, impact, photo) { navController.popBackStack() }
                            },
                            onSignOut = sessionViewModel::signOut
                        )
                    }
                }
                composable<ValidateUsersRoute> {
                    val pending by operatorViewModel.pendingCitizens.collectAsStateWithLifecycle()
                    ValidateUsersScreen(pending = pending, onActivate = operatorViewModel::activateCitizen)
                }
                composable<CategoriesRoute> {
                    val categories by operatorViewModel.categories.collectAsStateWithLifecycle()
                    CategoriesScreen(
                        categories = categories,
                        onSave = operatorViewModel::saveCategory,
                        onActiveChange = operatorViewModel::setCategoryActive,
                        onCreateDefaults = operatorViewModel::createDefaultCategories
                    )
                }
            }
        }
    }
}