package pt.isec.amov.cityfix

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import pt.isec.amov.cityfix.data.LocationRepository
import pt.isec.amov.cityfix.model.IncidentLifecycle
import pt.isec.amov.cityfix.model.IncidentStatus
import pt.isec.amov.cityfix.model.Session
import pt.isec.amov.cityfix.model.UserRole
import pt.isec.amov.cityfix.ui.components.AppBottomBar
import pt.isec.amov.cityfix.ui.components.AppNavigationRail
import pt.isec.amov.cityfix.ui.components.MainTopAppBar
import pt.isec.amov.cityfix.ui.components.ResolveProofDialog
import pt.isec.amov.cityfix.ui.components.MessagesSection
import pt.isec.amov.cityfix.ui.components.ReopenButton
import pt.isec.amov.cityfix.ui.components.ReopenDialog
import pt.isec.amov.cityfix.ui.components.TicketSection
import pt.isec.amov.cityfix.ui.components.TopLevelDestination
import pt.isec.amov.cityfix.ui.navigation.CategoriesRoute
import pt.isec.amov.cityfix.ui.navigation.DashboardRoute
import pt.isec.amov.cityfix.ui.navigation.EditIncidentRoute
import pt.isec.amov.cityfix.ui.navigation.IncidentDetailsRoute
import pt.isec.amov.cityfix.ui.navigation.IncidentListRoute
import pt.isec.amov.cityfix.ui.navigation.LoginRoute
import pt.isec.amov.cityfix.ui.navigation.ProfileRoute
import pt.isec.amov.cityfix.ui.navigation.RegisterRoute
import pt.isec.amov.cityfix.ui.navigation.ValidateUsersRoute
import pt.isec.amov.cityfix.ui.screens.CategoriesScreen
import pt.isec.amov.cityfix.ui.screens.DashboardScreen
import pt.isec.amov.cityfix.ui.screens.EditScreen
import pt.isec.amov.cityfix.ui.screens.GuestAreaScreen
import pt.isec.amov.cityfix.ui.screens.ListScreen
import pt.isec.amov.cityfix.ui.screens.LoadingScreen
import pt.isec.amov.cityfix.ui.screens.LoginScreen
import pt.isec.amov.cityfix.ui.screens.MfaScreen
import pt.isec.amov.cityfix.ui.screens.PendingScreen
import pt.isec.amov.cityfix.ui.screens.ProfileScreen
import pt.isec.amov.cityfix.ui.screens.RegisterScreen
import pt.isec.amov.cityfix.ui.screens.ShowScreen
import pt.isec.amov.cityfix.ui.screens.ValidateUsersScreen
import pt.isec.amov.cityfix.ui.theme.CityFixTheme
import pt.isec.amov.cityfix.utils.FileUtils
import pt.isec.amov.cityfix.viewmodel.AreaState
import pt.isec.amov.cityfix.viewmodel.DashboardViewModel
import pt.isec.amov.cityfix.viewmodel.DashboardViewModelFactory
import pt.isec.amov.cityfix.viewmodel.EditIncidentViewModel
import pt.isec.amov.cityfix.viewmodel.EditIncidentViewModelFactory
import pt.isec.amov.cityfix.viewmodel.GuestAreaViewModel
import pt.isec.amov.cityfix.viewmodel.GuestAreaViewModelFactory
import pt.isec.amov.cityfix.viewmodel.IncidentsViewModel
import pt.isec.amov.cityfix.viewmodel.IncidentsViewModelFactory
import pt.isec.amov.cityfix.viewmodel.ListFilter
import pt.isec.amov.cityfix.viewmodel.LoginViewModel
import pt.isec.amov.cityfix.viewmodel.LoginViewModelFactory
import pt.isec.amov.cityfix.viewmodel.OperatorViewModel
import pt.isec.amov.cityfix.viewmodel.OperatorViewModelFactory
import pt.isec.amov.cityfix.viewmodel.RegisterViewModel
import pt.isec.amov.cityfix.viewmodel.RegisterViewModelFactory
import pt.isec.amov.cityfix.viewmodel.SessionViewModel
import pt.isec.amov.cityfix.viewmodel.SessionViewModelFactory
import pt.isec.amov.cityfix.viewmodel.MessagesViewModel
import pt.isec.amov.cityfix.viewmodel.MessagesViewModelFactory
import pt.isec.amov.cityfix.viewmodel.TicketViewModel
import pt.isec.amov.cityfix.viewmodel.TicketViewModelFactory

class MainActivity : ComponentActivity() {

    private val app get() = application as CityFixApp

    // ViewModel da sessão, associado à atividade
    private val sessionViewModel: SessionViewModel by viewModels {
        SessionViewModelFactory(app.authRepository)
    }

    // Verificação dos 10 km do munícipe anónimo, associada à atividade:
    // o resultado mantém-se ao rodar o ecrã e ao entrar/sair da sessão
    private val guestAreaViewModel: GuestAreaViewModel by viewModels {
        GuestAreaViewModelFactory(app.locationRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CityFixTheme {
                val session by sessionViewModel.session.collectAsStateWithLifecycle()
                val mfaMessage by sessionViewModel.mfaMessage.collectAsStateWithLifecycle()
                val mfaInfo by sessionViewModel.mfaInfo.collectAsStateWithLifecycle()

                // Enquanto a sessão não está completa, mostra-se um ecrã próprio em vez da navegação
                when (val s = session) {
                    Session.Loading -> Scaffold { p ->
                        LoadingScreen(onCancel = sessionViewModel::signOut, modifier = Modifier.padding(p))
                    }
                    is Session.AwaitingMfa -> Scaffold { p ->
                        // Envia o código por e-mail ao entrar neste ecrã (não reenvia se já houver um válido)
                        LaunchedEffect(s.email) { sessionViewModel.ensureMfaCode() }
                        MfaScreen(
                            email = s.email,
                            message = mfaMessage,
                            info = mfaInfo,
                            onConfirm = sessionViewModel::verifyMfa,
                            onResend = sessionViewModel::resendMfaCode,
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
     * mais as rotas de login, registo, perfil e do operador, o dashboard e a barra de navegação
     * (inferior em portrait, lateral em landscape) com os separadores Painel e Ocorrências.
     * O anónimo começa no dashboard e só vê o conteúdo depois de confirmada a área de 10 km.
     */
    @Composable
    private fun MainNavigation(session: Session) {
        val viewModel: IncidentsViewModel = viewModel(
            factory = IncidentsViewModelFactory(app.repository, app.authRepository)
        )
        val operatorViewModel: OperatorViewModel = viewModel(
            factory = OperatorViewModelFactory(app.authRepository, app.categoriesRepository, app.statsRepository)
        )
        val dashboardViewModel: DashboardViewModel = viewModel(
            factory = DashboardViewModelFactory(app.statsRepository, app.repository)
        )
        val navController = rememberNavController()
        var isExpanded by rememberSaveable { mutableStateOf(false) }
        val user = (session as? Session.Active)?.user
        val isGuest = session is Session.Guest

        // --- Área de 10 km do munícipe anónimo (enunciado I.1) ---
        val areaState by guestAreaViewModel.state.collectAsStateWithLifecycle()
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results -> guestAreaViewModel.onPermissionResult(results.values.any { it }) }
        val requestOrCheck = {
            if (guestAreaViewModel.hasPermission) guestAreaViewModel.checkLocation()
            else permissionLauncher.launch(LocationRepository.LOCATION_PERMISSIONS)
        }
        LaunchedEffect(isGuest) {
            if (isGuest && guestAreaViewModel.state.value == AreaState.Unknown) requestOrCheck()
        }
        // Anónimo fora da área (ou ainda por confirmar): o dashboard, a lista e o detalhe ficam bloqueados.
        // O login e o registo continuam acessíveis (com sessão a app funciona em qualquer lugar).
        val blocked = isGuest && areaState != AreaState.Inside
        val areaScreen: @Composable () -> Unit = {
            GuestAreaScreen(
                state = areaState,
                onAllowLocation = { permissionLauncher.launch(LocationRepository.LOCATION_PERMISSIONS) },
                onOpenSettings = {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                    )
                },
                onRetry = requestOrCheck,
                onSignIn = { navController.navigate(LoginRoute) }
            )
        }

        // --- Barra de navegação (Painel / Ocorrências), como no mockup ---
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentTab = when {
            backStackEntry?.destination?.hasRoute<DashboardRoute>() == true -> TopLevelDestination.DASHBOARD
            backStackEntry?.destination?.hasRoute<IncidentListRoute>() == true -> TopLevelDestination.INCIDENTS
            else -> null
        }
        // Só nos dois ecrãs principais (nos outros há o botão de voltar)
        val showNavigation = currentTab != null && !blocked
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val navigateToTab: (TopLevelDestination) -> Unit = { tab ->
            val route: Any = when (tab) {
                TopLevelDestination.DASHBOARD -> DashboardRoute
                TopLevelDestination.INCIDENTS -> IncidentListRoute
            }
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

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
            },
            bottomBar = {
                if (showNavigation && !isLandscape) AppBottomBar(selected = currentTab, onSelect = navigateToTab)
            }
        ) { innerPadding ->
            Row(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                if (showNavigation && isLandscape) AppNavigationRail(selected = currentTab, onSelect = navigateToTab)
                NavHost(
                    navController = navController,
                    // O anónimo começa no dashboard; com sessão começa-se na lista (as minhas / todas)
                    startDestination = if (isGuest) DashboardRoute else IncidentListRoute,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    composable<DashboardRoute> {
                        if (blocked) {
                            areaScreen()
                            return@composable
                        }
                        val stats by dashboardViewModel.stats.collectAsStateWithLifecycle()
                        val recentPublic by dashboardViewModel.recentPublic.collectAsStateWithLifecycle()
                        DashboardScreen(
                            stats = stats,
                            recentPublic = recentPublic,
                            onIncidentClick = { id -> navController.navigate(IncidentDetailsRoute(id)) },
                            onSeeAll = {
                                // O munícipe tem dois separadores na lista: abre já o das públicas
                                viewModel.onFilterChanged(ListFilter.PUBLIC)
                                navigateToTab(TopLevelDestination.INCIDENTS)
                            }
                        )
                    }
                    composable<IncidentListRoute> {
                        if (blocked) {
                            areaScreen()
                            return@composable
                        }
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
                        if (blocked) {
                            areaScreen()
                            return@composable
                        }
                        val route = backStack.toRoute<IncidentDetailsRoute>()
                        // Ler as listas faz este ecrã atualizar-se quando se volta da edição.
                        // Procura-se também na pré-visualização do dashboard (pode vir de lá).
                        val incidents by viewModel.incidentsList.collectAsStateWithLifecycle()
                        val recentPublic by dashboardViewModel.recentPublic.collectAsStateWithLifecycle()
                        val incident = incidents.find { it.id == route.incidentId }
                            ?: recentPublic.find { it.id == route.incidentId }
                        val isOwner = user != null && incident?.userId == user.uid
                        val isOperator = user?.role == UserRole.OPERATOR
                        val showLocation = isOwner || isOperator
                        // Ciclo de vida desta ocorrência (associado a esta rota: sai do detalhe, é destruído)
                        val ticketViewModel: TicketViewModel = viewModel(
                            factory = TicketViewModelFactory(
                                route.incidentId, app.repository, app.messagesRepository, app.authRepository
                            )
                        )
                        val author by ticketViewModel.author.collectAsStateWithLifecycle()
                        val proofPath by ticketViewModel.proofPath.collectAsStateWithLifecycle()
                        val resolveDialogOpen by ticketViewModel.resolveDialogOpen.collectAsStateWithLifecycle()
                        val resolveNote by ticketViewModel.resolveNote.collectAsStateWithLifecycle()
                        // Galeria e câmara da prova de resolução, registadas aqui (a rota existe sempre),
                        // e não dentro do diálogo: se o Android recriar a app enquanto a câmara está aberta,
                        // o resultado chega na mesma e o diálogo reaparece logo com a fotografia.
                        val context = LocalContext.current
                        val proofGallery = rememberLauncherForActivityResult(
                            ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            uri?.let { FileUtils.copyUriToInternalStorage(context, it) }
                                ?.let(ticketViewModel::onProofPicked)
                        }
                        val proofCamera = rememberLauncherForActivityResult(
                            ActivityResultContracts.TakePicture()
                        ) { success -> ticketViewModel.onCameraResult(success) }
                        if (resolveDialogOpen && isOperator) {
                            ResolveProofDialog(
                                proofPath = proofPath,
                                note = resolveNote,
                                onNoteChange = ticketViewModel::onResolveNoteChange,
                                onPickGallery = {
                                    proofGallery.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onTakePhoto = {
                                    val (file, uri) = FileUtils.createTempImageFile(context)
                                    ticketViewModel.onCameraLaunched(file.absolutePath)
                                    proofCamera.launch(uri)
                                },
                                // Enquanto a ocorrência volta a carregar (depois da recriação), espera
                                canConfirm = incident != null,
                                onConfirm = { incident?.let { ticketViewModel.resolve(it) } },
                                onDismiss = ticketViewModel::discardProof
                            )
                        }
                        ShowScreen(
                            incident = incident,
                            // O autor só edita/apaga enquanto ninguém pegou na ocorrência
                            canEdit = isOwner && incident?.status == IncidentStatus.OPEN,
                            showLocation = showLocation,
                            // Os outros (anónimo e munícipes que não são o autor) veem só a zona aproximada
                            showApproximateMap = !showLocation,
                            onEditClick = { id -> navController.navigate(EditIncidentRoute(id)) },
                            onDeleteClick = { id -> viewModel.deleteIncident(id) { navController.popBackStack() } },
                            extraContent = {
                                // Ciclo de vida: só o autor e o operador (enunciado I.2.E e I.3.E)
                                if (incident != null && (isOwner || isOperator)) {
                                    TicketSection(
                                        incident = incident,
                                        isOperator = isOperator,
                                        author = author,
                                        onRequestInfo = { question -> ticketViewModel.requestInfo(incident, question) },
                                        onStartWork = { ticketViewModel.startWork(incident) },
                                        onOpenResolve = ticketViewModel::openResolveDialog,
                                        partB = {
                                            // PARTE B: mensagens e botão "Reabrir" (no sítio do mockup:
                                            // operador antes do histórico, autor depois do histórico).
                                            // Associado à rota do detalhe, como o TicketViewModel.
                                            val messagesViewModel: MessagesViewModel = viewModel(
                                                factory = MessagesViewModelFactory(
                                                    route.incidentId, app.messagesRepository,
                                                    app.repository, app.authRepository
                                                )
                                            )
                                            val messages by messagesViewModel.messages.collectAsStateWithLifecycle()
                                            val draft by messagesViewModel.draft.collectAsStateWithLifecycle()
                                            val reopenOpen by messagesViewModel.reopenDialogOpen.collectAsStateWithLifecycle()
                                            val reopenReason by messagesViewModel.reopenReason.collectAsStateWithLifecycle()
                                            val reopenError by messagesViewModel.reopenError.collectAsStateWithLifecycle()
                                            // Só o autor reabre, até 5 dias depois de resolvida
                                            if (isOwner && IncidentLifecycle.canReopen(incident)) {
                                                ReopenButton(onClick = messagesViewModel::openReopenDialog)
                                            }
                                            MessagesSection(
                                                messages = messages,
                                                currentUid = messagesViewModel.currentUid,
                                                draft = draft,
                                                onDraftChange = messagesViewModel::onDraftChange,
                                                onSend = { messagesViewModel.send(incident) },
                                                closed = incident.status == IncidentStatus.CLOSED
                                            )
                                            if (reopenOpen && isOwner) {
                                                ReopenDialog(
                                                    reason = reopenReason,
                                                    error = reopenError,
                                                    onReasonChange = messagesViewModel::onReopenReasonChange,
                                                    onConfirm = { messagesViewModel.reopen(incident) },
                                                    onDismiss = messagesViewModel::dismissReopen
                                                )
                                            }
                                        }
                                    )
                                }
                            }
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
}
