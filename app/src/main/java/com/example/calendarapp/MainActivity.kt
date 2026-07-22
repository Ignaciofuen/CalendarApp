package com.example.calendarapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.calendarapp.ui.components.BottomNavigationBar
import com.example.calendarapp.ui.screens.*
import com.example.calendarapp.ui.theme.CalendarAppTheme
import com.example.calendarapp.ui.viewmodel.CalendarViewModel
import com.example.calendarapp.ui.viewmodel.SettingsViewModel
import com.example.calendarapp.ui.viewmodel.ThemeViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita edge-to-edge: la app dibuja detrás de las barras del sistema.
        // El BottomNavigationBar usará WindowInsets para subirse automáticamente
        // cuando el usuario tiene activos los botones de navegación del sistema.
        enableEdgeToEdge()

        createNotificationChannel()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val calendarViewModel: CalendarViewModel = viewModel()
            val themeViewModel: ThemeViewModel = viewModel()
            val context = LocalContext.current

            // --- GESTIÓN DE PERMISOS (Android 13+) ---
            val notifPermissionSnackbar = remember { androidx.compose.material3.SnackbarHostState() }
            val permissionScope = rememberCoroutineScope()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    // Informar al usuario que las notificaciones no funcionarán
                    permissionScope.launch {
                        notifPermissionSnackbar.showSnackbar(
                            message = "Las notificaciones de eventos requieren permiso / Event notifications require permission",
                            duration = androidx.compose.material3.SnackbarDuration.Long
                        )
                    }
                }
            }

            // Solicitar permiso automáticamente al iniciar si no se tiene (API 33+)
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val currentLang by settingsViewModel.currentLanguage.collectAsState()

            LaunchedEffect(currentLang) {
                val localeCode = if (currentLang.contains("Español", ignoreCase = true)) "es" else "en"
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(localeCode)
                AppCompatDelegate.setApplicationLocales(appLocale)
            }

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Deep link desde widget: navega a la fecha recibida como extra
            LaunchedEffect(Unit) {
                val dateExtra = intent?.getStringExtra("navigate_to_date")
                if (!dateExtra.isNullOrEmpty()) {
                    try {
                        val date = java.time.LocalDate.parse(dateExtra)
                        calendarViewModel.onDateSelected(date)
                    } catch (e: Exception) { /* fecha inválida, ignorar */ }
                }
            }

            // Orden de las pestañas para el swipe y las animaciones de slide
            val tabRoutes = listOf("calendar", "agenda", "design", "settings")
            fun tabIndex(route: String?) = tabRoutes.indexOf(route ?: "")

            CalendarAppTheme(themeViewModel = themeViewModel) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        bottomBar = {
                            if (currentRoute != "splash") {
                                BottomNavigationBar(
                                    navController = navController,
                                    themeViewModel = themeViewModel,
                                    settingsViewModel = settingsViewModel
                                )
                            }
                        }
                    ) { innerPadding ->

                        // Wrapper con gesto horizontal de swipe entre pestañas.
                        // Se deshabilita en "calendar" porque ese screen ya usa swipe
                        // horizontal internamente para cambiar de mes.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .pointerInput(currentRoute) {
                                    val swipeableRoutes = listOf("agenda", "design", "settings")
                                    if (currentRoute == null || currentRoute !in swipeableRoutes) return@pointerInput

                                    var totalDragX = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { totalDragX = 0f },
                                        onHorizontalDrag = { _, dragAmount ->
                                            totalDragX += dragAmount
                                        },
                                        onDragEnd = {
                                            val threshold = 80.dp.toPx()
                                            if (kotlin.math.abs(totalDragX) >= threshold) {
                                                val currentIndex = tabRoutes.indexOf(currentRoute)
                                                // Swipe izquierda → avanza (índice mayor)
                                                // Swipe derecha  → retrocede (índice menor)
                                                val targetIndex = if (totalDragX < 0) currentIndex + 1 else currentIndex - 1
                                                if (targetIndex in tabRoutes.indices) {
                                                    navController.navigate(tabRoutes[targetIndex]) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                        },
                                        onDragCancel = { totalDragX = 0f }
                                    )
                                }
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = "splash",
                                modifier = Modifier.fillMaxSize(),
                                // Animación de entrada: slide desde la dirección correcta para tabs,
                                // fade+scale para otras rutas (splash, etc.)
                                enterTransition = {
                                    val from = tabIndex(initialState.destination.route)
                                    val to   = tabIndex(targetState.destination.route)
                                    if (from >= 0 && to >= 0) {
                                        // Slide desde derecha si avanzamos, desde izquierda si retrocedemos
                                        slideInHorizontally(
                                            initialOffsetX = { w -> if (to > from) w else -w },
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        ) + fadeIn(tween(220, easing = FastOutSlowInEasing))
                                    } else {
                                        fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                                        scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.96f)
                                    }
                                },
                                exitTransition = {
                                    val from = tabIndex(initialState.destination.route)
                                    val to   = tabIndex(targetState.destination.route)
                                    if (from >= 0 && to >= 0) {
                                        slideOutHorizontally(
                                            targetOffsetX = { w -> if (to > from) -w else w },
                                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                                        ) + fadeOut(tween(200, easing = FastOutSlowInEasing))
                                    } else {
                                        fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                                        scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 1.02f)
                                    }
                                },
                                popEnterTransition = {
                                    val from = tabIndex(initialState.destination.route)
                                    val to   = tabIndex(targetState.destination.route)
                                    if (from >= 0 && to >= 0) {
                                        slideInHorizontally(
                                            initialOffsetX = { w -> if (to > from) w else -w },
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        ) + fadeIn(tween(220, easing = FastOutSlowInEasing))
                                    } else {
                                        fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                                        scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 1.02f)
                                    }
                                },
                                popExitTransition = {
                                    val from = tabIndex(initialState.destination.route)
                                    val to   = tabIndex(targetState.destination.route)
                                    if (from >= 0 && to >= 0) {
                                        slideOutHorizontally(
                                            targetOffsetX = { w -> if (to > from) -w else w },
                                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                                        ) + fadeOut(tween(200, easing = FastOutSlowInEasing))
                                    } else {
                                        fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                                        scaleOut(tween(200, easing = FastOutSlowInEasing), targetScale = 0.96f)
                                    }
                                }
                            ) {
                                composable("splash") {
                                    SplashScreen(onNavigateToMain = {
                                        navController.navigate("calendar") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    })
                                }

                                composable("calendar") {
                                    CalendarScreen(
                                        calendarViewModel = calendarViewModel,
                                        themeViewModel = themeViewModel,
                                        settingsViewModel = settingsViewModel,
                                        navController = navController
                                    )
                                }

                                composable("agenda") {
                                    AgendaScreen(
                                        calendarViewModel = calendarViewModel,
                                        themeViewModel = themeViewModel,
                                        settingsViewModel = settingsViewModel,
                                        navController = navController
                                    )
                                }

                                composable("settings") {
                                    SettingsScreen(
                                        settingsViewModel = settingsViewModel,
                                        themeViewModel = themeViewModel
                                    )
                                }

                                // IMPORTANTE: Ruta "design" alineada con BottomNavigationBar para evitar crash
                                composable("design") {
                                    ThemeBuilderScreen(
                                        viewModel = themeViewModel,
                                        navController = navController,
                                        settingsViewModel = settingsViewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Registra el canal de notificaciones necesario para Android 8.0+
     * ID: "EVENT_CHANNEL" sincronizado con el Receiver y el ViewModel.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Event Notifications"
            val descriptionText = "Canal para recordatorios de eventos"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("EVENT_CHANNEL", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}