package com.example.calendarapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

import com.example.calendarapp.data.model.CalendarEvent
import com.example.calendarapp.ui.viewmodel.CalendarViewModel
import com.example.calendarapp.ui.viewmodel.SettingsViewModel
import com.example.calendarapp.ui.viewmodel.ThemeViewModel
import com.example.calendarapp.utils.AppStrings
import com.example.calendarapp.ui.viewmodel.AppTheme
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AgendaScreen(
    calendarViewModel: CalendarViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    navController: NavController? = null
) {
    val lang by settingsViewModel.currentLanguage.collectAsState()
    val startWeekOn by settingsViewModel.startWeekOn.collectAsState()

    key(lang) {
        val configuration = LocalConfiguration.current

        val currentLocale = remember(lang) {
            if (lang.contains("Español", ignoreCase = true)) java.util.Locale.forLanguageTag("es-ES") else java.util.Locale.ENGLISH
        }

        val eventsMap by calendarViewModel.events.collectAsState()
        val theme by themeViewModel.currentTheme
        var searchQuery by remember { mutableStateOf("") }
        var groupByWeek by remember { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        val today = LocalDate.now()

        // Color fluido global
        val animatedAccentColor by animateColorAsState(
            targetValue = theme.accentColor,
            animationSpec = tween(durationMillis = 600),
            label = "AgendaGlobalAccentColor"
        )

        // Pulsación del punto "En vivo" en sección Hoy
        val infiniteTransition = rememberInfiniteTransition(label = "TodayDot")
        val todayDotScale by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "TodayDotScale"
        )
        val todayDotAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "TodayDotAlpha"
        )

        // Filtro: título + nota + mes + día semana + número de día + año
        // IMPORTANTE: Envuelto en remember para evitar recálculo en cada frame
        // (la animación infinita del punto "Hoy" causa recomposición 60fps)
        val allEventsSorted = remember(eventsMap, searchQuery, currentLocale) {
            eventsMap.values.flatten()
                .filter { event ->
                    if (searchQuery.isBlank()) true
                    else {
                        val query = searchQuery.lowercase()
                        val monthName = event.date.month.getDisplayName(TextStyle.FULL, currentLocale).lowercase()
                        val dayName = event.date.dayOfWeek.getDisplayName(TextStyle.FULL, currentLocale).lowercase()
                        val dayNum = event.date.dayOfMonth.toString()
                        val yearStr = event.date.year.toString()
                        event.title.contains(query, ignoreCase = true) ||
                        event.note.contains(query, ignoreCase = true) ||
                        monthName.contains(query) ||
                        dayName.contains(query) ||
                        dayNum == query ||
                        yearStr.contains(query)
                    }
                }
                .sortedBy { it.date }
        }

        // Agrupación dinámica: por semana o por mes
        val firstDayOfWeek = if (startWeekOn == "Monday") DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        val weekFields = WeekFields.of(firstDayOfWeek, 1)

        val groupedEvents: Map<String, List<CalendarEvent>> = remember(allEventsSorted, groupByWeek, startWeekOn, currentLocale) {
            if (groupByWeek) {
                allEventsSorted.groupBy { event ->
                    val weekStart = event.date.with(weekFields.dayOfWeek(), 1)
                    val weekEnd = weekStart.plusDays(6)
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM", currentLocale)
                    "${weekStart.format(fmt)} – ${weekEnd.format(fmt)}"
                }
            } else {
                allEventsSorted.groupBy { event ->
                    val month = event.date.month.getDisplayName(TextStyle.FULL, currentLocale)
                        .replaceFirstChar { char -> char.uppercase() }
                    "$month ${event.date.year}"
                }
            }
        }

        val context = LocalContext.current

        // SnackbarHostState para deshacer eliminación
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize().background(theme.systemBackgroundColor)) {
            val currentMonth = today.monthValue
            val monthImg = theme.backgroundImages[currentMonth]
            val backgroundUri = if (theme.showBackgroundImages) {
                if (!monthImg.isNullOrBlank()) monthImg else theme.globalBackgroundImage
            } else null

            key(backgroundUri) {
                if (backgroundUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backgroundUri)
                            .crossfade(true)
                            .setParameter("allow_hardware", false)
                            .build(),
                        contentDescription = if (lang.contains("Español")) "Fondo de agenda" else "Agenda background",
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
                    .padding(16.dp)
            ) {
                // Encabezado con título y toggle mes/semana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get("schedule", lang),
                        color = theme.getMainTextColor(),
                        fontSize = (28 * theme.globalTextScale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = theme.getFontFamily(),
                        modifier = Modifier.weight(1f)
                    )
                    // Toggle agrupación mes / semana
                    TextButton(onClick = { groupByWeek = !groupByWeek }) {
                        Text(
                            text = if (groupByWeek) {
                                if (lang.contains("Español")) "Por mes" else "By month"
                            } else {
                                if (lang.contains("Español")) "Por semana" else "By week"
                            },
                            color = animatedAccentColor,
                            fontSize = (12 * theme.globalTextScale).sp,
                            fontFamily = theme.getFontFamily()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (lang.contains("Español")) "Busca eventos, notas, fechas..." else "Search events, notes, dates...",
                            color = theme.getLabelColor(),
                            fontSize = (14 * theme.globalTextScale).sp,
                            fontFamily = theme.getFontFamily()
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = if (lang.contains("Español")) "Buscar" else "Search", tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = if (lang.contains("Español")) "Limpiar búsqueda" else "Clear search", tint = Color.Gray)
                            }
                        }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { /* Cierra el teclado al presionar buscar */ }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(theme.getBorderRadius())),
                    shape = RoundedCornerShape(theme.getBorderRadius()),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.getCardColor(),
                        unfocusedContainerColor = theme.getCardColor(),
                        focusedBorderColor = animatedAccentColor,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = theme.getMainTextColor(),
                        unfocusedTextColor = theme.getMainTextColor()
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Sección "Hoy" fija al inicio (solo si no hay búsqueda activa)
                    if (searchQuery.isBlank()) {
                        item(key = "today_anchor") {
                            val todayEvents = eventsMap[today] ?: emptyList()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        animatedAccentColor.copy(alpha = 0.12f),
                                        RoundedCornerShape(theme.getBorderRadius())
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(animatedAccentColor, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = today.dayOfMonth.toString(),
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = (16 * theme.globalTextScale).sp,
                                        fontFamily = theme.getFontFamily()
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (lang.contains("Español")) "Hoy" else "Today",
                                        color = animatedAccentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (14 * theme.globalTextScale).sp,
                                        fontFamily = theme.getFontFamily()
                                    )
                                    Text(
                                        text = today.dayOfWeek.getDisplayName(TextStyle.FULL, currentLocale)
                                            .replaceFirstChar { it.uppercase() } + ", " +
                                            today.month.getDisplayName(TextStyle.FULL, currentLocale)
                                            .replaceFirstChar { it.uppercase() } + " ${today.dayOfMonth}",
                                        color = theme.getLabelColor(),
                                        fontSize = (12 * theme.globalTextScale).sp,
                                        fontFamily = theme.getFontFamily()
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                // Punto "en vivo" pulsante
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(7.dp)
                                        .graphicsLayer {
                                            scaleX = todayDotScale
                                            scaleY = todayDotScale
                                            alpha = todayDotAlpha
                                        }
                                        .background(animatedAccentColor, CircleShape)
                                )
                                if (todayEvents.isNotEmpty()) {
                                    Text(
                                        text = "${todayEvents.size} ${if (lang.contains("Español")) "evento${if (todayEvents.size > 1) "s" else ""}" else "event${if (todayEvents.size > 1) "s" else ""}"}",
                                        color = animatedAccentColor.copy(alpha = 0.7f),
                                        fontSize = (11 * theme.globalTextScale).sp,
                                        fontFamily = theme.getFontFamily()
                                    )
                                }
                            }
                        }
                    }

                    // Estado vacío
                    if (groupedEvents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = if (lang.contains("Español")) "Sin eventos" else "No events",
                                        tint = theme.getLabelColor().copy(alpha = 0.2f),
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) {
                                            if (lang.contains("Español")) "Sin resultados para \"$searchQuery\""
                                            else "No results for \"$searchQuery\""
                                        } else {
                                            if (lang.contains("Español")) "No hay eventos aún"
                                            else "No events yet"
                                        },
                                        color = theme.getLabelColor().copy(alpha = 0.45f),
                                        fontSize = (13 * theme.globalTextScale).sp,
                                        fontFamily = theme.getFontFamily()
                                    )
                                }
                            }
                        }
                    }

                    groupedEvents.forEach { (header, eventsList) ->
                        item(key = "header_$header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Badge con el nombre del grupo
                                Box(
                                    modifier = Modifier
                                        .background(
                                            animatedAccentColor.copy(alpha = 0.15f),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = header,
                                        color = animatedAccentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (13 * theme.globalTextScale).sp,
                                        fontFamily = theme.getFontFamily()
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // Línea separadora con gradiente
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    animatedAccentColor.copy(alpha = 0.30f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }
                        }
                        items(
                            items = eventsList,
                            key = { event -> event.id }
                        ) { event ->
                            val isPast = event.date.isBefore(today)
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        calendarViewModel.deleteEvent(event.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Snackbar con deshacer
                                        val capturedEvent = event
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = if (lang.contains("Español")) "Evento eliminado" else "Event deleted",
                                                actionLabel = if (lang.contains("Español")) "Deshacer" else "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                calendarViewModel.restoreEvent(capturedEvent)
                                            }
                                        }
                                        true
                                    } else false
                                }
                            )
                            Box(modifier = Modifier.animateItem()) {
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val bgAlpha by animateFloatAsState(
                                            targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0f,
                                            label = "AgendaSwipeBgAlpha"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(theme.getBorderRadius()))
                                                .background(Color(0xFFD32F2F).copy(alpha = bgAlpha))
                                                .padding(end = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = if (lang.contains("Español")) "Eliminar" else "Delete", tint = Color.White)
                                        }
                                    }
                                ) {
                                    AgendaEventCard(
                                        event = event,
                                        theme = theme,
                                        lang = lang,
                                        isPast = isPast,
                                        onAutoSaveNote = { calendarViewModel.updateEventNote(event, it) },
                                        // Si hay búsqueda activa, el tap navega al calendario
                                        onNavigate = if (searchQuery.isNotBlank() && navController != null) {
                                            {
                                                calendarViewModel.onDateSelected(event.date)
                                                navController.navigate("calendar") {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SnackbarHost posicionado sobre el contenido
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF2C2F2E),
                    contentColor = Color.White,
                    actionColor = animatedAccentColor
                )
            }
        }
    }
}

@Composable
fun AgendaEventCard(
    event: CalendarEvent,
    theme: AppTheme,
    lang: String,
    isPast: Boolean = false,
    onAutoSaveNote: (String) -> Unit,
    onNavigate: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf(event.note) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "AgendaCardBounce"
    )

    // Color base del evento
    val eventBaseColor = remember(event.colorHex) {
        if (event.colorHex.isNotEmpty()) {
            try { Color(android.graphics.Color.parseColor(event.colorHex)) }
            catch (e: Exception) { null }
        } else null
    }

    // Color animado: gris si es pasado, color propio o acento si no
    val targetAccent = when {
        isPast -> Color.Gray.copy(alpha = 0.5f)
        eventBaseColor != null -> eventBaseColor
        else -> theme.accentColor
    }
    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 600),
        label = "AgendaCardAccentColor"
    )

    val cardAlpha = if (isPast) 0.55f else 1f

    LaunchedEffect(noteInput) {
        if (noteInput != event.note) {
            delay(500)
            onAutoSaveNote(noteInput)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = cardAlpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                // Si hay navegación pendiente (búsqueda activa) → ir al calendario
                // Si no → expandir/colapsar la tarjeta
                onClick = { if (onNavigate != null) onNavigate() else expanded = !expanded }
            ),
        colors = CardDefaults.cardColors(containerColor = theme.getCardColor()),
        shape = RoundedCornerShape(theme.getBorderRadius())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val locale = remember(lang) {
                    if (lang.contains("Español", ignoreCase = true))
                        java.util.Locale.forLanguageTag("es-ES") else java.util.Locale.ENGLISH
                }
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    animatedAccentColor,
                                    animatedAccentColor.copy(alpha = 0.25f)
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = event.date.dayOfWeek
                            .getDisplayName(java.time.format.TextStyle.SHORT, locale)
                            .uppercase(),
                        color = animatedAccentColor.copy(alpha = 0.62f),
                        fontSize = (9 * theme.globalTextScale).sp,
                        letterSpacing = 1.sp,
                        fontFamily = theme.getFontFamily()
                    )
                    Text(
                        text = event.date.dayOfMonth.toString().padStart(2, '0'),
                        color = animatedAccentColor,
                        fontSize = (26 * theme.globalTextScale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = theme.getFontFamily()
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        color = if (isPast) theme.getMainTextColor().copy(alpha = 0.6f) else theme.getMainTextColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = (16 * theme.globalTextScale).sp,
                        fontFamily = theme.getFontFamily(),
                        maxLines = 1
                    )
                    Text(
                        text = event.time,
                        color = theme.getLabelColor(),
                        fontSize = (13 * theme.globalTextScale).sp,
                        fontFamily = theme.getFontFamily()
                    )
                }
                if (isPast) {
                    Text(
                        text = if (lang.contains("Español")) "pasado" else "past",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        fontFamily = theme.getFontFamily(),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(initialAlpha = 0.3f),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(theme.getBorderRadius()))
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (lang.contains("Español")) "Detalles y Notas" else "Details & Notes",
                        color = animatedAccentColor.copy(alpha = 0.8f),
                        fontSize = (11 * theme.globalTextScale).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = theme.getFontFamily()
                    )
                    BasicTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .heightIn(min = 60.dp),
                        textStyle = LocalTextStyle.current.copy(
                            color = theme.getMainTextColor(),
                            fontSize = (14 * theme.globalTextScale).sp,
                            fontFamily = theme.getFontFamily()
                        ),
                        cursorBrush = SolidColor(animatedAccentColor),
                        decorationBox = { innerTextField ->
                            if (noteInput.isEmpty()) {
                                Text(
                                    text = if (lang.contains("Español")) "Toca para añadir detalles..." else "Tap to add details...",
                                    color = theme.getLabelColor(),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    Text(
                        text = if (lang.contains("Español")) "Cambios guardados automáticamente" else "Changes saved automatically",
                        color = Color.Gray.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
