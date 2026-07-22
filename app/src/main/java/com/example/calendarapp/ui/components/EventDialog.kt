package com.example.calendarapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import java.time.*
import java.util.Locale

import com.example.calendarapp.data.model.CalendarEvent
import com.example.calendarapp.data.model.NotificationConfig
import com.example.calendarapp.data.model.NotificationType
import com.example.calendarapp.data.model.RecurrenceType
import com.example.calendarapp.ui.viewmodel.AppTheme
import com.example.calendarapp.utils.AppStrings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventDialog(
    existingEvent: CalendarEvent?,
    initialDate: LocalDate,          // ← Crítico 2: fecha editable
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, String, LocalTime, NotificationConfig, String, String, RecurrenceType, Boolean, String, String, String, String) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    theme: AppTheme,
    lang: String
) {
    val isSpanish = lang.contains("Español")
    val context = LocalContext.current

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    val initialTime = remember(existingEvent) {
        val t = existingEvent?.time?.takeIf { it.isNotEmpty() } ?: "12:00"
        try { LocalTime.parse(t) } catch (e: Exception) { LocalTime.of(12, 0) }
    }
    val initialEndTime = remember(existingEvent) {
        if (!existingEvent?.endTime.isNullOrEmpty())
            try { LocalTime.parse(existingEvent!!.endTime) } catch (e: Exception) { null }
        else null
    }

    var isAllDay by remember { mutableStateOf(existingEvent?.isAllDay ?: false) }
    var hour by remember { mutableStateOf(initialTime.hour.toString()) }
    var minute by remember { mutableStateOf(initialTime.minute.toString().padStart(2, '0')) }
    var endHour by remember { mutableStateOf(initialEndTime?.hour?.toString() ?: "") }
    var endMinute by remember { mutableStateOf(initialEndTime?.minute?.toString()?.padStart(2, '0') ?: "") }
    var hasEndTime by remember { mutableStateOf(initialEndTime != null) }
    var location by remember { mutableStateOf(existingEvent?.location ?: "") }
    var url by remember { mutableStateOf(existingEvent?.url ?: "") }
    var photoUri by remember { mutableStateOf(existingEvent?.photoUri ?: "") }
    var notificationSoundUri by remember { mutableStateOf(existingEvent?.notificationSoundUri ?: "") }

    var notifyEnabled by remember { mutableStateOf(existingEvent?.notification?.enabled ?: false) }
    var notifyType by remember { mutableStateOf(existingEvent?.notification?.type ?: NotificationType.SAME_DAY) }
    var selectedColorHex by remember { mutableStateOf(existingEvent?.colorHex ?: "") }
    var selectedRecurrence by remember { mutableStateOf(existingEvent?.recurrence ?: RecurrenceType.NONE) }
    // Crítico 2: fecha seleccionada del evento (editable)
    var selectedEventDate by remember { mutableStateOf(initialDate) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Selectores con ActivityResult
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri -> photoUri = uri?.toString() ?: photoUri }

    val soundPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            notificationSoundUri = uri?.toString() ?: ""
        }
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    val animatedAccentColor by animateColorAsState(
        targetValue = theme.accentColor,
        animationSpec = tween(durationMillis = 600),
        label = "DialogAccentColor"
    )

    fun fmtPreview(h: String, m: String): String = try {
        val hh = h.toIntOrNull() ?: 0; val mm = m.toIntOrNull() ?: 0
        LocalTime.of(hh % 24, mm % 60).format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
    } catch (e: Exception) { "--:-- --" }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.systemBackgroundColor.copy(alpha = 0.95f),
        title = {
            Text(
                if (existingEvent != null) AppStrings.get("edit_event", lang) else AppStrings.get("new_event", lang),
                color = theme.getMainTextColor(), fontFamily = theme.getFontFamily()
            )
        },
        text = {
            AnimatedVisibility(
                visible = startAnimation,
                enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(animationSpec = tween(300))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {

                    // Título
                    TextField(value = title, onValueChange = { title = it }, placeholder = { Text(AppStrings.get("new_event", lang)) })

                    // Fecha — selector con DatePickerDialog (Crítico 2)
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = if (isSpanish) "Seleccionar fecha" else "Select date", tint = animatedAccentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isSpanish) "Fecha" else "Date",
                                color = theme.getLabelColor(), fontSize = 12.sp
                            )
                            val dl = if (isSpanish) java.util.Locale("es", "ES") else java.util.Locale.ENGLISH
                            val dn = selectedEventDate.dayOfWeek
                                .getDisplayName(java.time.format.TextStyle.FULL, dl)
                                .replaceFirstChar { it.uppercase() }
                            val mn = selectedEventDate.month
                                .getDisplayName(java.time.format.TextStyle.FULL, dl)
                                .replaceFirstChar { it.uppercase() }
                            val dt = if (isSpanish)
                                "$dn, ${selectedEventDate.dayOfMonth} de $mn ${selectedEventDate.year}"
                            else
                                "$dn, $mn ${selectedEventDate.dayOfMonth}, ${selectedEventDate.year}"
                            Text(dt, color = theme.getMainTextColor(), fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = if (isSpanish) "Expandir calendario" else "Expand calendar", tint = theme.getLabelColor())
                    }
                    if (showDatePickerDialog) {
                        val dpState = rememberDatePickerState(
                            initialSelectedDateMillis = selectedEventDate
                                .atStartOfDay(java.time.ZoneOffset.UTC)
                                .toInstant()
                                .toEpochMilli()
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePickerDialog = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    dpState.selectedDateMillis?.let { ms ->
                                        selectedEventDate = java.time.Instant.ofEpochMilli(ms)
                                            .atZone(java.time.ZoneOffset.UTC)
                                            .toLocalDate()
                                    }
                                    showDatePickerDialog = false
                                }) { Text("OK", color = animatedAccentColor) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePickerDialog = false }) {
                                    Text(
                                        if (isSpanish) "Cancelar" else "Cancel",
                                        color = Color.Gray
                                    )
                                }
                            }
                        ) { DatePicker(state = dpState) }
                    }

                    // Todo el día
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = if (isSpanish) "Todo el día" else "All day", tint = animatedAccentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSpanish) "Todo el día" else "All day", color = theme.getMainTextColor(), modifier = Modifier.weight(1f))
                        Switch(checked = isAllDay, onCheckedChange = { isAllDay = it }, colors = SwitchDefaults.colors(checkedThumbColor = animatedAccentColor))
                    }

                    // Hora de inicio (oculta si es todo el día)
                    AnimatedVisibility(visible = !isAllDay) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (isSpanish) "Hora de inicio" else "Start time", color = theme.getLabelColor(), fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextField(value = hour, onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) <= 23) hour = it }, modifier = Modifier.weight(1f), label = { Text("HH") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                                TextField(value = minute, onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) <= 59) minute = it }, modifier = Modifier.weight(1f), label = { Text("MM") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                            }
                            Text(
                                text = "${if (isSpanish) "Inicio" else "Start"}: ${fmtPreview(hour, minute)}",
                                color = animatedAccentColor, fontSize = 12.sp
                            )
                        }
                    }

                    // Hora de fin (opcional, oculta si es todo el día)
                    AnimatedVisibility(visible = !isAllDay) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isSpanish) "Hora de fin" else "End time", color = theme.getMainTextColor(), modifier = Modifier.weight(1f))
                                Switch(checked = hasEndTime, onCheckedChange = { hasEndTime = it }, colors = SwitchDefaults.colors(checkedThumbColor = animatedAccentColor))
                            }
                            AnimatedVisibility(visible = hasEndTime) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextField(value = endHour, onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) <= 23) endHour = it }, modifier = Modifier.weight(1f), label = { Text("HH") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                                        TextField(value = endMinute, onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() } && (it.toIntOrNull() ?: 0) <= 59) endMinute = it }, modifier = Modifier.weight(1f), label = { Text("MM") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                                    }
                                    Text(
                                        text = "${if (isSpanish) "Fin" else "End"}: ${fmtPreview(endHour, endMinute)}",
                                        color = animatedAccentColor, fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Recurrencia
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Text(if (isSpanish) "Repetir" else "Repeat", color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurrenceType.entries.forEach { rec ->
                            val label = when (rec) {
                                RecurrenceType.NONE -> if (isSpanish) "Nunca" else "Never"
                                RecurrenceType.DAILY -> if (isSpanish) "Diario" else "Daily"
                                RecurrenceType.WEEKLY -> if (isSpanish) "Semanal" else "Weekly"
                                RecurrenceType.MONTHLY -> if (isSpanish) "Mensual" else "Monthly"
                                RecurrenceType.YEARLY -> if (isSpanish) "Anual" else "Yearly"
                            }
                            val isSelected = selectedRecurrence == rec
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) animatedAccentColor else Color.White.copy(alpha = 0.08f))
                                    .clickable { selectedRecurrence = rec }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(label, color = if (isSelected) Color.Black else Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }

                    // Notificaciones
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = if (isSpanish) "Notificación" else "Notification", tint = animatedAccentColor)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSpanish) "Notificar" else "Notify", color = theme.getMainTextColor(), modifier = Modifier.weight(1f))
                        Switch(checked = notifyEnabled, onCheckedChange = { notifyEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = animatedAccentColor))
                    }
                    if (notifyEnabled) {
                        NotificationType.entries.forEach { type ->
                            val label = when (type) {
                                NotificationType.SAME_DAY -> if (isSpanish) "Mismo día" else "Same day"
                                NotificationType.ONE_DAY_BEFORE -> if (isSpanish) "1 día antes" else "1 day before"
                                NotificationType.TWO_DAYS_BEFORE -> if (isSpanish) "2 días antes" else "2 days before"
                                NotificationType.ONE_WEEK_BEFORE -> if (isSpanish) "1 semana antes" else "1 week before"
                            }
                            Row(Modifier.fillMaxWidth().clickable { notifyType = type }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = notifyType == type, onClick = { notifyType = type }, colors = RadioButtonDefaults.colors(selectedColor = animatedAccentColor))
                                Text(text = label, color = Color.LightGray, fontSize = 14.sp)
                            }
                        }
                    }

                    // Paleta de colores
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Text(if (isSpanish) "Color del evento" else "Event color", color = Color.LightGray, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf("", "#FF5252", "#FF9800", "#FFEB3B", "#4CAF50", "#2196F3", "#CE93D8", "#F48FB1").forEach { hex ->
                            val displayColor = if (hex.isEmpty()) theme.accentColor
                            else try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { theme.accentColor }
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(26.dp).clip(CircleShape).background(displayColor)
                                    .border(if (isSelected) 2.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) Icon(Icons.Default.Check, contentDescription = if (isSpanish) "Seleccionado" else "Selected", tint = Color.Black, modifier = Modifier.size(13.dp))
                            }
                        }
                    }

                    // Ubicación
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    TextField(
                        value = location,
                        onValueChange = { location = it },
                        placeholder = { Text(if (isSpanish) "Ubicación (opcional)" else "Location (optional)", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = if (isSpanish) "Ubicación" else "Location", tint = animatedAccentColor) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // URL
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("URL (opcional)", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = "URL", tint = animatedAccentColor) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Foto
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isSpanish) "Foto" else "Photo", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            border = androidx.compose.foundation.BorderStroke(1.dp, animatedAccentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = if (isSpanish) "Agregar foto" else "Add photo", tint = animatedAccentColor, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (photoUri.isNotEmpty()) (if (isSpanish) "Cambiar" else "Change") else (if (isSpanish) "Elegir" else "Pick"), color = animatedAccentColor, fontSize = 12.sp)
                        }
                    }
                    if (photoUri.isNotEmpty()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = if (isSpanish) "Foto del evento" else "Event photo",
                            modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Tono de notificación
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Notifications, contentDescription = if (isSpanish) "Tono de alerta" else "Alert sound", tint = animatedAccentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isSpanish) "Tono de alerta" else "Alert sound",
                            color = theme.getMainTextColor(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION)
                                    if (notificationSoundUri.isNotEmpty()) {
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(notificationSoundUri))
                                    }
                                }
                                soundPickerLauncher.launch(intent)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, animatedAccentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (notificationSoundUri.isNotEmpty()) (if (isSpanish) "Cambiar" else "Change") else (if (isSpanish) "Elegir" else "Choose"),
                                color = animatedAccentColor,
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (notificationSoundUri.isNotEmpty()) {
                        val soundName = try {
                            android.media.RingtoneManager.getRingtone(context, android.net.Uri.parse(notificationSoundUri))?.getTitle(context) ?: ""
                        } catch (e: Exception) { "" }
                        if (soundName.isNotEmpty()) {
                            Text(soundName, color = theme.getLabelColor(), fontSize = 10.sp, fontFamily = theme.getFontFamily())
                        }
                    }

                    // Duplicar (solo en edición)
                    if (existingEvent != null && onDuplicate != null) {
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        TextButton(
                            onClick = { onDuplicate(); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = if (isSpanish) "Duplicar" else "Duplicate", tint = animatedAccentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSpanish) "Duplicar evento" else "Duplicate event", color = animatedAccentColor)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) return@TextButton
                val h = hour.toIntOrNull() ?: 12; val m = minute.toIntOrNull() ?: 0
                val selectedTime = LocalTime.of(h % 24, m % 60)
                val endTimeStr = if (!isAllDay && hasEndTime && endHour.isNotBlank()) {
                    val eh = endHour.toIntOrNull() ?: 0; val em = endMinute.toIntOrNull() ?: 0
                    LocalTime.of(eh % 24, em % 60).toString()
                } else ""
                onConfirm(
                    selectedEventDate, title, selectedTime,
                    NotificationConfig(notifyEnabled, notifyType),
                    selectedColorHex, endTimeStr, selectedRecurrence,
                    isAllDay, location, url, photoUri, notificationSoundUri
                )
            }) {
                Text(AppStrings.get("save_changes", lang), color = if (title.isBlank()) Color.Gray else animatedAccentColor)
            }
        },
        dismissButton = {
            var showDeleteConfirm by remember { mutableStateOf(false) }
            TextButton(onClick = {
                if (existingEvent != null) showDeleteConfirm = true
                else onDismiss()
            }) {
                Text(
                    if (existingEvent != null) AppStrings.get("delete", lang) else AppStrings.get("cancel", lang),
                    color = if (existingEvent != null) Color.Red else theme.getLabelColor()
                )
            }
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    containerColor = theme.systemBackgroundColor,
                    title = {
                        Text(
                            if (isSpanish) "¿Eliminar evento?" else "Delete event?",
                            color = theme.getMainTextColor(), fontFamily = theme.getFontFamily()
                        )
                    },
                    text = {
                        Text(
                            if (isSpanish) "Esta acción no se puede deshacer." else "This action cannot be undone.",
                            color = theme.getLabelColor(), fontFamily = theme.getFontFamily()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                            Text(if (isSpanish) "Eliminar" else "Delete", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(if (isSpanish) "Cancelar" else "Cancel", color = theme.getLabelColor())
                        }
                    }
                )
            }
        }
    )
}
