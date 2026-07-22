package com.example.calendarapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendarapp.utils.DataManager
import com.example.calendarapp.utils.GoogleCalendarSync
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import coil.request.ImageRequest
import com.example.calendarapp.ui.viewmodel.AppTheme
import com.example.calendarapp.ui.viewmodel.SettingsViewModel
import com.example.calendarapp.ui.viewmodel.ThemeViewModel
import com.example.calendarapp.utils.AppStrings // Importamos tu nuevo diccionario
import com.example.calendarapp.utils.DateFormatType

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    LaunchedEffect(Unit) { settingsViewModel.syncDrafts() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados y contexto — deben ir ANTES del launcher para que el lambda pueda capturarlos
    val currentLang by settingsViewModel.currentLanguage.collectAsState()
    val startWeekOn by settingsViewModel.startWeekOn.collectAsState()
    val selectedLanguage = settingsViewModel.draftLanguage
    val selectedFormat = settingsViewModel.draftFormat
    val theme by themeViewModel.currentTheme
    val context = LocalContext.current
    val bgDark = Color(0xFF0A0C0B)
    val cardBg = theme.getCardColor()

    // Launcher para importar un archivo .ics
    val icsPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val input = context.contentResolver.openInputStream(uri) ?: return@launch
                DataManager.importFromIcs(context, input).fold(
                    onSuccess = { count ->
                        snackbarHostState.showSnackbar(
                            if (currentLang.contains("Español")) "Importados: $count evento(s)" else "Imported: $count event(s)"
                        )
                    },
                    onFailure = {
                        snackbarHostState.showSnackbar(
                            if (currentLang.contains("Español")) "Error al importar" else "Import failed"
                        )
                    }
                )
            }
        }
    }

    // Estado de sesión de Google (reactivo)
    var googleAccount by remember {
        mutableStateOf(GoogleCalendarSync.getSignedInAccount(context))
    }

    // Launcher para iniciar sesión con Google
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, GoogleCalendarSync.buildSignInOptions())
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            googleAccount = task.getResult(ApiException::class.java)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (currentLang.contains("Español"))
                        "Sesión iniciada como ${googleAccount?.email}"
                    else
                        "Signed in as ${googleAccount?.email}"
                )
            }
        } catch (e: ApiException) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (currentLang.contains("Español")) "Error al iniciar sesión" else "Sign-in failed"
                )
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = Color(0xFF2C2F2E), contentColor = Color.White)
            }
        },
        bottomBar = {
            Button(
                onClick = { settingsViewModel.saveChanges() },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                shape = RoundedCornerShape(theme.getBorderRadius()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Text(
                    // Redibujado reactivo usando AppStrings
                    text = AppStrings.get("save_changes", currentLang),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = (16 * theme.globalTextScale).sp,
                    fontFamily = theme.getFontFamily()
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.systemBackgroundColor)) {
            // LÓGICA DE FONDO UNIFICADA: Prioridad Mes > Global (Reactividad Directa)
            val currentMonth = LocalDate.now().monthValue
            val monthImg = theme.backgroundImages[currentMonth]
            val backgroundUri = if (theme.showBackgroundImages) {
                if (!monthImg.isNullOrBlank()) monthImg else theme.globalBackgroundImage
            } else null
 
            // USAMOS key() PARA FORZAR EL RE-RENDERIZADO CUANDO CAMBIA EL URI
            key(backgroundUri) {
                if (backgroundUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backgroundUri)
                            .crossfade(true)
                            .setParameter("allow_hardware", false)
                            .build(),
                        contentDescription = if (currentLang.contains("Español")) "Fondo de configuración" else "Settings background",
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(theme.blurAmount.dp)
                            .graphicsLayer(rotationZ = theme.bgRotation)
                            .offset(y = theme.bgOffsetY.dp),
                        contentScale = ContentScale.Crop,
                        alpha = theme.backgroundOpacity
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            Text(
                text = AppStrings.get("preferences_title", currentLang),
                color = theme.getMainTextColor(),
                fontSize = (24 * theme.globalTextScale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp),
                fontFamily = theme.getFontFamily()
            )

            // --- SECTOR DE IDIOMA ---
            SectionTitle(AppStrings.get("app_language", currentLang), theme)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(theme.getBorderRadius()))
                    .background(cardBg)
                    .padding(4.dp)
            ) {
                LanguageOption(
                    text = "English",
                    isSelected = selectedLanguage == "English",
                    theme = theme,
                    modifier = Modifier.weight(1f)
                ) { settingsViewModel.updateDraftLanguage("English") }

                LanguageOption(
                    text = "Español",
                    isSelected = selectedLanguage == "Español",
                    theme = theme,
                    modifier = Modifier.weight(1f)
                ) { settingsViewModel.updateDraftLanguage("Español") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTOR DE FORMATO DE FECHA ---
            SectionTitle(AppStrings.get("date_format", currentLang), theme)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(theme.getBorderRadius())
            ) {
                Column {
                    DateFormatType.values().forEach { format ->
                        FormatOptionRow(
                            label = format.pattern.uppercase(),
                            example = "e.g. ${format.example}",
                            isSelected = selectedFormat == format,
                            theme = theme
                        ) { settingsViewModel.updateDraftFormat(format) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTOR DE INICIO DE SEMANA ---
            SectionTitle(AppStrings.get("start_week_on", currentLang), theme)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(theme.getBorderRadius()),
                modifier = Modifier.clickable {
                    val nextDay = if (startWeekOn == "Monday") "Sunday" else "Monday"
                    settingsViewModel.updateStartWeek(nextDay)
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = if (currentLang.contains("Español")) "Inicio de semana" else "Start of week", tint = theme.accentColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.get("start_week_on", currentLang),
                            color = theme.getMainTextColor(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (14 * theme.globalTextScale).sp,
                            fontFamily = theme.getFontFamily()
                        )
                        Text(
                            text = if (startWeekOn == "Monday") "Monday / Lunes" else "Sunday / Domingo",
                            color = theme.getLabelColor(),
                            fontSize = (12 * theme.globalTextScale).sp,
                            fontFamily = theme.getFontFamily()
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = if (currentLang.contains("Español")) "Cambiar" else "Change", tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTOR DE DATOS: EXPORTAR Y BACKUP ---
            SectionTitle(if (currentLang.contains("Español")) "Datos" else "Data", theme)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(theme.getBorderRadius())
            ) {
                Column {
                    // Exportar ICS
                    DataActionRow(
                        label = if (currentLang.contains("Español")) "Exportar a iCalendar (.ics)" else "Export to iCalendar (.ics)",
                        sublabel = if (currentLang.contains("Español")) "Compatible con Google Calendar y Apple Calendar" else "Compatible with Google Calendar & Apple Calendar",
                        theme = theme
                    ) {
                        scope.launch {
                            DataManager.exportToIcs(context).fold(
                                onSuccess = { file ->
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Exportado: ${file.name}" else "Exported: ${file.name}"
                                    )
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Error al exportar" else "Export failed"
                                    )
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    // Exportar CSV
                    DataActionRow(
                        label = if (currentLang.contains("Español")) "Exportar a CSV" else "Export to CSV",
                        sublabel = if (currentLang.contains("Español")) "Para hojas de cálculo" else "For spreadsheet apps",
                        theme = theme
                    ) {
                        scope.launch {
                            DataManager.exportToCsv(context).fold(
                                onSuccess = { file ->
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Exportado: ${file.name}" else "Exported: ${file.name}"
                                    )
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Error al exportar" else "Export failed"
                                    )
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    // Backup DB
                    DataActionRow(
                        label = if (currentLang.contains("Español")) "Hacer copia de seguridad" else "Backup database",
                        sublabel = if (currentLang.contains("Español")) "Guarda todos tus eventos localmente (sin cifrar)" else "Save all events locally (unencrypted)",
                        theme = theme
                    ) {
                        DataManager.backupDatabase(context).fold(
                            onSuccess = { file ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Backup guardado: ${file.name}" else "Backup saved: ${file.name}"
                                    )
                                }
                            },
                            onFailure = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Error en el backup" else "Backup failed"
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- IMPORTAR DATOS ---
            SectionTitle(if (currentLang.contains("Español")) "Importar" else "Import", theme)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(theme.getBorderRadius())
            ) {
                Column {
                    // Importar ICS
                    DataActionRow(
                        label = if (currentLang.contains("Español")) "Importar desde iCalendar (.ics)" else "Import from iCalendar (.ics)",
                        sublabel = if (currentLang.contains("Español")) "Importa eventos desde un archivo .ics" else "Import events from an .ics file",
                        theme = theme
                    ) { icsPickerLauncher.launch("*/*") }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Importar cumpleaños desde Contactos
                    DataActionRow(
                        label = if (currentLang.contains("Español")) "Importar cumpleaños" else "Import birthdays",
                        sublabel = if (currentLang.contains("Español")) "Desde los contactos del dispositivo" else "From device contacts",
                        theme = theme
                    ) {
                        scope.launch {
                            DataManager.importBirthdaysFromContacts(context).fold(
                                onSuccess = { count ->
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Importados: $count cumpleaño(s)" else "Imported: $count birthday(s)"
                                    )
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Error: verifica el permiso de Contactos" else "Error: check Contacts permission"
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Sincronizar Google Calendar
                    DataActionRow(
                        label = if (currentLang.contains("Español")) "Sincronizar Google Calendar" else "Sync Google Calendar",
                        sublabel = if (currentLang.contains("Español")) "Importa eventos del calendario del sistema" else "Import events from system calendar",
                        theme = theme
                    ) {
                        scope.launch {
                            DataManager.syncFromGoogleCalendar(context).fold(
                                onSuccess = { count ->
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Sincronizados: $count evento(s) nuevo(s)" else "Synced: $count new event(s)"
                                    )
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar(
                                        if (currentLang.contains("Español")) "Error: verifica el permiso de Calendario" else "Error: check Calendar permission"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CUENTA DE GOOGLE ---
            SectionTitle(if (currentLang.contains("Español")) "Cuenta de Google" else "Google Account", theme)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(theme.getBorderRadius())
            ) {
                Column {
                    // Estado de sesión
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar o icono de Google
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (googleAccount != null) theme.accentColor else Color.Gray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (googleAccount?.photoUrl != null) {
                                AsyncImage(
                                    model = googleAccount!!.photoUrl,
                                    contentDescription = if (currentLang.contains("Español")) "Foto de perfil" else "Profile photo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = if (googleAccount != null) (googleAccount!!.displayName?.firstOrNull()?.uppercase() ?: "G") else "G",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = googleAccount?.displayName
                                    ?: (if (currentLang.contains("Español")) "Sin cuenta vinculada" else "No account linked"),
                                color = theme.getMainTextColor(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (14 * theme.globalTextScale).sp,
                                fontFamily = theme.getFontFamily()
                            )
                            Text(
                                text = googleAccount?.email
                                    ?: (if (currentLang.contains("Español")) "Iniciá sesión para sincronizar" else "Sign in to sync"),
                                color = theme.getLabelColor(),
                                fontSize = (12 * theme.globalTextScale).sp,
                                fontFamily = theme.getFontFamily()
                            )
                        }
                        // Botón conectar / desconectar
                        if (googleAccount == null) {
                            OutlinedButton(
                                onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, theme.accentColor),
                                shape = RoundedCornerShape(theme.getBorderRadius())
                            ) {
                                Text(
                                    if (currentLang.contains("Español")) "Conectar" else "Connect",
                                    color = theme.accentColor,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        googleAccount = null
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                                shape = RoundedCornerShape(theme.getBorderRadius())
                            ) {
                                Text(
                                    if (currentLang.contains("Español")) "Salir" else "Sign out",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Acciones de sincronización (solo si está autenticado)
                    if (googleAccount != null) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        DataActionRow(
                            label = if (currentLang.contains("Español")) "Importar desde Google Calendar" else "Import from Google Calendar",
                            sublabel = if (currentLang.contains("Español"))
                                "Descarga eventos de los últimos 90 días y próximo año"
                            else
                                "Downloads events from last 90 days and next year",
                            theme = theme
                        ) {
                            scope.launch {
                                GoogleCalendarSync.importFromGoogleCalendar(context).fold(
                                    onSuccess = { count ->
                                        snackbarHostState.showSnackbar(
                                            if (currentLang.contains("Español"))
                                                "Importados: $count evento(s) nuevo(s)"
                                            else
                                                "Imported: $count new event(s)"
                                        )
                                    },
                                    onFailure = { e ->
                                        snackbarHostState.showSnackbar(
                                            if (currentLang.contains("Español"))
                                                "Error: ${e.message}"
                                            else
                                                "Error: ${e.message}"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- APARIENCIA ---
            SectionTitle(if (currentLang.contains("Español")) "Apariencia" else "Appearance", theme)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(theme.getBorderRadius())
            ) {
                val isLight = themeViewModel.isLightMode()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLight) Icons.Default.WbSunny
                                      else Icons.Default.NightsStay,
                        contentDescription = if (currentLang.contains("Español")) "Modo de color" else "Color mode",
                        tint = theme.accentColor
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (currentLang.contains("Español")) "Modo de color" else "Color mode",
                            color = theme.getMainTextColor(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (14 * theme.globalTextScale).sp,
                            fontFamily = theme.getFontFamily()
                        )
                        Text(
                            text = if (isLight)
                                (if (currentLang.contains("Español")) "Modo claro activo" else "Light mode active")
                            else
                                (if (currentLang.contains("Español")) "Modo oscuro activo" else "Dark mode active"),
                            color = theme.getLabelColor(),
                            fontSize = (12 * theme.globalTextScale).sp,
                            fontFamily = theme.getFontFamily()
                        )
                    }
                    Switch(
                        checked = isLight,
                        onCheckedChange = { themeViewModel.toggleLightMode() },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedThumbColor = theme.accentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
}

@Composable
fun SectionTitle(text: String, theme: AppTheme) {
    Text(
        text = text,
        color = theme.getLabelColor(),
        fontSize = (11 * theme.globalTextScale).sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
        fontFamily = theme.getFontFamily()
    )
}

@Composable
fun LanguageOption(text: String, isSelected: Boolean, theme: AppTheme, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(theme.getBorderRadius()))
            .background(if (isSelected) theme.accentColor else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else theme.getLabelColor(),
            fontWeight = FontWeight.Bold,
            fontSize = (14 * theme.globalTextScale).sp,
            fontFamily = theme.getFontFamily()
        )
    }
}

@Composable
fun DataActionRow(label: String, sublabel: String, theme: AppTheme, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = theme.getMainTextColor(), fontWeight = FontWeight.SemiBold, fontSize = (14 * theme.globalTextScale).sp, fontFamily = theme.getFontFamily())
            Text(sublabel, color = theme.getLabelColor(), fontSize = (12 * theme.globalTextScale).sp, fontFamily = theme.getFontFamily())
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Navigate", tint = Color.Gray)
    }
}

@Composable
fun FormatOptionRow(label: String, example: String, isSelected: Boolean, theme: AppTheme, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = theme.getMainTextColor(), fontWeight = FontWeight.Bold, fontSize = (14 * theme.globalTextScale).sp, fontFamily = theme.getFontFamily())
            Text(example, color = theme.getLabelColor(), fontSize = (12 * theme.globalTextScale).sp, fontFamily = theme.getFontFamily())
        }
        Box(
            modifier = Modifier.size(20.dp).border(2.dp, if (isSelected) theme.accentColor else Color.Gray, CircleShape).padding(4.dp)
        ) {
            if (isSelected) {
                Box(modifier = Modifier.fillMaxSize().background(theme.accentColor, CircleShape))
            }
        }
    }
}