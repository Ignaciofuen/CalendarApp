package com.example.calendarapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.TextStyle
import java.util.Locale

// --- IMPORTS DE TUS COMPONENTES ---
import com.example.calendarapp.ui.components.CalendarDayComponent
import com.example.calendarapp.ui.components.EventDetailSheet
import com.example.calendarapp.ui.components.EventDialog
import com.example.calendarapp.utils.AppStrings
import com.example.calendarapp.data.model.CalendarEvent
import com.example.calendarapp.data.model.NotificationConfig
import com.example.calendarapp.data.model.NotificationType
import com.example.calendarapp.data.model.RecurrenceType
import com.example.calendarapp.ui.viewmodel.CalendarViewModel
import com.example.calendarapp.ui.viewmodel.SettingsViewModel
import com.example.calendarapp.ui.viewmodel.ThemeViewModel
import com.example.calendarapp.ui.viewmodel.AppTheme
import com.example.calendarapp.utils.HolidayProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    navController: NavController
) {
    val lang by settingsViewModel.currentLanguage.collectAsState()
    val context = LocalContext.current

    val startWeekOn by settingsViewModel.startWeekOn.collectAsState()

    key(lang, startWeekOn) {
        val currentLocale = remember(lang) {
            if (lang.contains("Español", ignoreCase = true)) java.util.Locale.forLanguageTag("es-ES") else java.util.Locale.ENGLISH
        }

        val events by calendarViewModel.events.collectAsState()
        val selectedDate by calendarViewModel.selectedDate.collectAsState()
        val theme by themeViewModel.currentTheme
        val coroutineScope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val snackbarHostState = remember { SnackbarHostState() }

        var showAddDialog by remember { mutableStateOf(false) }
        var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
        var showDetailEvent by remember { mutableStateOf<CalendarEvent?>(null) }
        var showMonthMenu by remember { mutableStateOf(false) }
        var longPressDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
        var conflictWarning by remember { mutableStateOf<String?>(null) }
        var showRecurringDeleteWarning by remember { mutableStateOf<CalendarEvent?>(null) }
        // Para edición de recurrentes: guardamos la instancia seleccionada hasta que el usuario elija
        var recurringEditTarget by remember { mutableStateOf<CalendarEvent?>(null) }

        val currentMonth = remember { YearMonth.now() }

        val state = rememberCalendarState(
            startMonth = currentMonth.minusMonths(50),
            endMonth = currentMonth.plusMonths(50),
            firstVisibleMonth = currentMonth,
            firstDayOfWeek = if (startWeekOn == "Monday") DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        )

        // Scroll al mes correcto cuando selectedDate cambia (ej. desde búsqueda de Agenda)
        LaunchedEffect(selectedDate) {
            val targetMonth = YearMonth.from(selectedDate)
            if (targetMonth != state.firstVisibleMonth.yearMonth) {
                coroutineScope.launch { state.animateScrollToMonth(targetMonth) }
            }
        }

        val visibleMonth = state.firstVisibleMonth.yearMonth

        // Feriados: recalculamos cuando cambia el año visible
        val holidays = remember(visibleMonth.year) {
            HolidayProvider.getHolidays(visibleMonth.year)
        }

        val displayEvents = remember(events, visibleMonth) {
            events.filterKeys { YearMonth.from(it) == visibleMonth }
                .values
                .flatten()
                .sortedWith(compareBy({ it.date }, { it.time }))
        }

        // Máximo de eventos en cualquier día del mes visible (para mapa de calor)
        val maxEventsInMonth = remember(events, visibleMonth) {
            events.filterKeys { YearMonth.from(it) == visibleMonth }
                .values.maxOfOrNull { it.size } ?: 1
        }

        // LÓGICA DE FONDO UNIFICADA: Prioridad Mes > Global (Reactividad Directa)
        val monthImg = theme.backgroundImages[state.firstVisibleMonth.yearMonth.monthValue]
        val backgroundUri = if (theme.showBackgroundImages) {
            if (!monthImg.isNullOrBlank()) monthImg else theme.globalBackgroundImage
        } else null
 
        // ANIMACIÓN 1: Color fluido global
        val animatedAccentColor by animateColorAsState(
            targetValue = theme.accentColor,
            animationSpec = tween(durationMillis = 600),
            label = "GlobalAccentColor"
        )
 
        Box(modifier = Modifier.fillMaxSize().background(theme.systemBackgroundColor)) {
            // USAMOS key() PARA FORZAR EL RE-RENDERIZADO CUANDO CAMBIA EL URI
            key(backgroundUri) {
                if (backgroundUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backgroundUri)
                            .crossfade(true)
                            .setParameter("allow_hardware", false)
                            .build(),
                        contentDescription = if (lang.contains("Español")) "Fondo del calendario" else "Calendar background",
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

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Selector mes/año con nombre animado al deslizar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).clickable { showMonthMenu = true }
                    ) {
                        AnimatedContent(
                            targetState = visibleMonth,
                            transitionSpec = {
                                val dir = if (targetState > initialState) 1 else -1
                                (slideInHorizontally { it * dir } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it * dir } + fadeOut())
                            },
                            label = "MonthNameAnim"
                        ) { ym ->
                            Text(
                                text = ym.month.getDisplayName(TextStyle.FULL, currentLocale).replaceFirstChar { it.uppercase() },
                                color = theme.getMainTextColor(),
                                fontSize = (28 * theme.globalTextScale).sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = theme.getFontFamily()
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, null, tint = theme.getMainTextColor(), modifier = Modifier.size(32.dp))

                        DropdownMenu(
                            expanded = showMonthMenu,
                            onDismissRequest = { showMonthMenu = false },
                            modifier = Modifier.background(theme.systemBackgroundColor.copy(alpha = 0.95f))
                        ) {
                            Month.entries.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month.getDisplayName(TextStyle.FULL, currentLocale), color = Color.White) },
                                    onClick = {
                                        coroutineScope.launch {
                                            state.scrollToMonth(state.firstVisibleMonth.yearMonth.withMonth(month.value))
                                        }
                                        showMonthMenu = false
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = visibleMonth.year.toString(),
                            color = theme.getLabelColor(),
                            fontSize = (28 * theme.globalTextScale).sp,
                            fontFamily = theme.getFontFamily()
                        )
                    }

                    // Botón "Hoy" — aparece solo cuando no estamos en el mes actual
                    AnimatedVisibility(
                        visible = visibleMonth != YearMonth.now(),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        TextButton(onClick = {
                            coroutineScope.launch { state.animateScrollToMonth(YearMonth.now()) }
                        }) {
                            Text(
                                text = if (lang.contains("Español")) "Hoy" else "Today",
                                color = animatedAccentColor,
                                fontSize = (12 * theme.globalTextScale).sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = theme.getFontFamily()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalCalendar(
                    state = state,
                    monthHeader = {
                        val firstDay = if (startWeekOn == "Monday") DayOfWeek.MONDAY else DayOfWeek.SUNDAY
                        val daysOfWeek = mutableListOf<DayOfWeek>()
                        var current = firstDay
                        repeat(7) {
                            daysOfWeek.add(current)
                            current = current.plus(1)
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            daysOfWeek.forEach { dw ->
                                val isWeekend = dw == DayOfWeek.SATURDAY || dw == DayOfWeek.SUNDAY
                                // Distinguimos Sa y Su en Inglés si quieres evitar solo S-S, pero NARROW suele dar S
                                // Usaremos una lógica manual para asegurar distinción si es inglés
                                val dayName = if (lang.contains("English") && isWeekend) {
                                    if (dw == DayOfWeek.SATURDAY) "Sa" else "Su"
                                } else {
                                    dw.getDisplayName(TextStyle.NARROW, currentLocale).uppercase()
                                }

                                Text(
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    text = dayName,
                                    color = if (isWeekend) theme.weekendColor else theme.getLabelColor(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (12 * theme.globalTextScale).sp,
                                    fontFamily = theme.getFontFamily()
                                )
                            }
                        }
                    },
                    dayContent = { day ->
                        CalendarDayComponent(
                            day = day,
                            isSelected = selectedDate == day.date,
                            isToday = day.date == LocalDate.now(),
                            hasEvent = events[day.date]?.isNotEmpty() == true,
                            eventCount = events[day.date]?.size ?: 0,
                            maxEventsInMonth = maxEventsInMonth,
                            isHoliday = holidays.containsKey(day.date),
                            theme = theme,
                            onClick = { calendarViewModel.onDateSelected(it.date) },
                            onLongClick = { d ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                calendarViewModel.onDateSelected(d.date)
                                longPressDate = d.date
                                showAddDialog = true
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pill de feriado — aparece cuando el día seleccionado es feriado
                val selectedHoliday = holidays[selectedDate]
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedHoliday != null,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                    exit  = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                ) {
                    selectedHoliday?.let { holiday ->
                        val holidayName = if (lang.contains("Español")) holiday.name else holiday.nameEn
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(theme.weekendColor.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🏛️ ", fontSize = 13.sp)
                            Text(
                                text = holidayName,
                                color = theme.weekendColor,
                                fontSize = (12 * theme.globalTextScale).sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = theme.getFontFamily()
                            )
                        }
                    }
                }

                val monthName = visibleMonth.month.getDisplayName(TextStyle.FULL, currentLocale).replaceFirstChar { it.uppercase() }
                Text(
                    text = if (displayEvents.isNotEmpty()) {
                        if (lang.contains("Español")) "Eventos de $monthName" else "$monthName Events"
                    } else {
                        AppStrings.get("no_events", lang)
                    },
                    color = theme.getLabelColor().copy(alpha = 0.8f),
                    fontSize = (12 * theme.globalTextScale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = theme.getFontFamily()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Estado vacío cuando no hay eventos en el mes visible
                    if (displayEvents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
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
                                        text = if (lang.contains("Español")) "Sin eventos este mes" else "No events this month",
                                        color = theme.getLabelColor().copy(alpha = 0.45f),
                                        fontSize = (13 * theme.globalTextScale).sp,
                                        fontFamily = theme.getFontFamily()
                                    )
                                }
                            }
                        }
                    }
                    items(
                        items = displayEvents,
                        key = { event -> "${event.id}_${event.date}" }  // Key compuesto para evitar duplicados en recurrentes
                    ) { event ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    val capturedEvent = event
                                    if (capturedEvent.recurrence != com.example.calendarapp.data.model.RecurrenceType.NONE) {
                                        // Evento recurrente: mostrar diálogo de confirmación en vez de borrar directo
                                        showRecurringDeleteWarning = capturedEvent
                                        return@rememberSwipeToDismissBoxState false  // No confirmar el swipe todavía
                                    }
                                    calendarViewModel.deleteEvent(event.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    coroutineScope.launch {
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
                                        label = "SwipeBgAlpha"
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
                                EventCard(event = event, theme = theme, lang = lang,
                                onClick = { showDetailEvent = event },
                                onLongClick = {
                                    if (event.recurrence != com.example.calendarapp.data.model.RecurrenceType.NONE)
                                        recurringEditTarget = event
                                    else
                                        eventToEdit = event
                                })
                            }
                        }
                    }
                }
            }

            // FAB con rotación animada al abrir/cerrar el diálogo
            val fabDialogOpen = showAddDialog || eventToEdit != null
            val fabRotation by animateFloatAsState(
                targetValue = if (fabDialogOpen) 45f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "FabRotation"
            )
            val fabScale by animateFloatAsState(
                targetValue = if (fabDialogOpen) 0.88f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "FabScale"
            )
            FloatingActionButton(
                onClick = {
                    if (!fabDialogOpen) showAddDialog = true
                    else { showAddDialog = false; eventToEdit = null }
                },
                containerColor = animatedAccentColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .graphicsLayer {
                        scaleX = fabScale
                        scaleY = fabScale
                    },
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (lang.contains("Español")) "Agregar evento" else "Add event",
                    tint = if (animatedAccentColor.red * 0.299f + animatedAccentColor.green * 0.587f + animatedAccentColor.blue * 0.114f > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.graphicsLayer { rotationZ = fabRotation }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = theme.systemBackgroundColor.copy(alpha = 0.95f),
                    contentColor = theme.getMainTextColor(),
                    actionColor = animatedAccentColor
                )
            }
        }

        if (showAddDialog || eventToEdit != null) {
            EventDialog(
                existingEvent = eventToEdit,
                initialDate = eventToEdit?.date ?: selectedDate,
                onDismiss = { showAddDialog = false; eventToEdit = null },
                onConfirm = { newDate, title, time, config, colorHex, endTime, recurrence, allDay, loc, eventUrl, photo, soundUri ->
                    val excludeId = eventToEdit?.id ?: -1L
                    val conflictTitle = calendarViewModel.findConflict(newDate, time, endTime, allDay, excludeId)
                    if (conflictTitle != null) {
                        conflictWarning = if (lang.contains("Español"))
                            "Conflicto con \"$conflictTitle\""
                        else
                            "Conflict with \"$conflictTitle\""
                        val warningMsg = conflictWarning // Capturar antes del coroutine para evitar NPE
                        if (warningMsg != null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(warningMsg, duration = SnackbarDuration.Short)
                            }
                        }
                    }
                    if (eventToEdit != null && eventToEdit!!.id != 0L) {
                        calendarViewModel.updateEvent(context, eventToEdit!!.id, newDate, title, time, config, colorHex, endTime, recurrence, allDay, loc, eventUrl, photo, soundUri)
                    } else {
                        // id == 0 → nueva ocurrencia ("Solo esta vez") o evento nuevo
                        calendarViewModel.addEvent(context, newDate, title, time, config, colorHex, endTime, recurrence, allDay, loc, eventUrl, photo, soundUri)
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddDialog = false; eventToEdit = null
                },
                onDelete = {
                    eventToEdit?.let { calendarViewModel.deleteEvent(it.id) }
                    eventToEdit = null
                },
                onDuplicate = {
                    eventToEdit?.let { calendarViewModel.duplicateEvent(it) }
                },
                theme = theme,
                lang = lang
            )
        }

        // Diálogo de elección al editar evento recurrente (Bug 3 fix)
        recurringEditTarget?.let { instanceEvent ->
            AlertDialog(
                onDismissRequest = { recurringEditTarget = null },
                title = {
                    Text(
                        if (lang.contains("Español")) "Editar evento recurrente"
                        else "Edit recurring event"
                    )
                },
                text = {
                    Text(
                        if (lang.contains("Español"))
                            "¿Qué deseas editar?"
                        else
                            "What would you like to edit?"
                    )
                },
                confirmButton = {
                    // "Toda la serie" → carga el evento ORIGINAL (con fecha de inicio real)
                    TextButton(onClick = {
                        val original = calendarViewModel.getOriginalEvent(instanceEvent.id)
                        eventToEdit = original ?: instanceEvent
                        showAddDialog = true
                        recurringEditTarget = null
                    }) {
                        Text(if (lang.contains("Español")) "Toda la serie" else "Entire series")
                    }
                },
                dismissButton = {
                    // "Solo esta vez" → copia con id=0 y recurrencia NONE para crear nuevo evento
                    TextButton(onClick = {
                        eventToEdit = instanceEvent.copy(
                            id = 0L,
                            recurrence = com.example.calendarapp.data.model.RecurrenceType.NONE
                        )
                        showAddDialog = true
                        recurringEditTarget = null
                    }) {
                        Text(if (lang.contains("Español")) "Solo esta vez" else "This occurrence only")
                    }
                }
            )
        }

        // Diálogo de confirmación para eliminar evento recurrente (Bug 4 fix)
        showRecurringDeleteWarning?.let { recurringEvent ->
            AlertDialog(
                onDismissRequest = { showRecurringDeleteWarning = null },
                title = {
                    Text(
                        if (lang.contains("Español")) "Eliminar serie recurrente"
                        else "Delete recurring series"
                    )
                },
                text = {
                    Text(
                        if (lang.contains("Español"))
                            "Este es un evento recurrente. Al eliminarlo se borrará toda la serie.\n¿Deseas continuar?"
                        else
                            "This is a recurring event. Deleting it will remove the entire series.\nDo you want to continue?"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        calendarViewModel.deleteEvent(recurringEvent.id)
                        showRecurringDeleteWarning = null
                    }) {
                        Text(
                            if (lang.contains("Español")) "Eliminar todo" else "Delete all",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecurringDeleteWarning = null }) {
                        Text(if (lang.contains("Español")) "Cancelar" else "Cancel")
                    }
                }
            )
        }

        // Bottom Sheet de detalle de evento (tap simple en tarjeta)
        showDetailEvent?.let { detailEvent ->
            EventDetailSheet(
                event = detailEvent,
                onDismiss = { showDetailEvent = null },
                onEdit = {
                    if (detailEvent.recurrence != com.example.calendarapp.data.model.RecurrenceType.NONE) {
                        recurringEditTarget = detailEvent
                    } else {
                        eventToEdit = detailEvent
                    }
                    showDetailEvent = null
                },
                onDelete = {
                    calendarViewModel.deleteEvent(detailEvent.id)
                    showDetailEvent = null
                },
                theme = theme,
                lang = lang
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(event: CalendarEvent, theme: AppTheme, lang: String = "English", onClick: () -> Unit = {}, onLongClick: () -> Unit) {
    // ANIMACIÓN 2: Efecto de rebote al presionar
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f, // Se encoge a 95% al presionar
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "CardBounce"
    )

    // Color: usa el color propio del evento si tiene, si no el acento del tema
    val eventBaseColor = remember(event.colorHex) {
        if (event.colorHex.isNotEmpty()) {
            try { Color(android.graphics.Color.parseColor(event.colorHex)) }
            catch (e: Exception) { null }
        } else null
    }
    val animatedAccentColor by animateColorAsState(
        targetValue = eventBaseColor ?: theme.accentColor,
        animationSpec = tween(durationMillis = 600),
        label = "CardAccentColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { // Aplica la animación de escala
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(theme.getBorderRadius()),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D1C).copy(alpha = theme.cardOpacity))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // MEJORADO: barra izquierda con gradiente vertical
            Box(
                Modifier
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
            Spacer(Modifier.width(16.dp))

            // MEJORADO: columna con día de semana encima del número
            val cardLocale = remember(lang) {
                if (lang.contains("Español", ignoreCase = true)) java.util.Locale.forLanguageTag("es-ES") else java.util.Locale.ENGLISH
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = event.date.dayOfWeek
                        .getDisplayName(java.time.format.TextStyle.SHORT, cardLocale)
                        .uppercase(),
                    color = animatedAccentColor.copy(alpha = 0.62f),
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontFamily = theme.getFontFamily()
                )
                Text(
                    text = event.date.dayOfMonth.toString().padStart(2, '0'),
                    color = animatedAccentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = theme.getFontFamily()
                )
            }
            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = theme.getMainTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontFamily = theme.getFontFamily(),
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val timeLabel = when {
                        event.isAllDay -> if (lang.contains("Español")) "Todo el día" else "All day"
                        event.endTime.isNotEmpty() -> "${event.time} – ${event.endTime}"
                        else -> event.time
                    }
                    Text(timeLabel, color = theme.getLabelColor(), fontSize = 12.sp, fontFamily = theme.getFontFamily())
                    if (event.notification.enabled) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Notifications, null, tint = animatedAccentColor, modifier = Modifier.size(12.dp))
                    }
                    if (event.recurrence != RecurrenceType.NONE) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Refresh, contentDescription = if (lang.contains("Español")) "Evento recurrente" else "Recurring event", tint = animatedAccentColor.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                    }
                }
                // Cuenta regresiva
                val today = LocalDate.now()
                val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, event.date).toInt()
                val countdownText = when {
                    daysUntil < 0  -> null
                    daysUntil == 0 -> if (lang.contains("Español")) "Hoy" else "Today"
                    daysUntil == 1 -> if (lang.contains("Español")) "Mañana" else "Tomorrow"
                    else           -> if (lang.contains("Español")) "En $daysUntil días" else "In $daysUntil days"
                }
                countdownText?.let {
                    Text(it, color = animatedAccentColor.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = theme.getFontFamily())
                }
                // Ubicación
                if (event.location.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = if (lang.contains("Español")) "Ubicación" else "Location", tint = theme.getLabelColor(), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(event.location, color = theme.getLabelColor(), fontSize = 10.sp, fontFamily = theme.getFontFamily(), maxLines = 1)
                    }
                }
            }
        }
    }
}