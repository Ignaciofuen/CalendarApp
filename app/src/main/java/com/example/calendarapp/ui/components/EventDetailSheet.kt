package com.example.calendarapp.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.calendarapp.data.model.CalendarEvent
import com.example.calendarapp.data.model.NotificationType
import com.example.calendarapp.data.model.RecurrenceType
import com.example.calendarapp.ui.viewmodel.AppTheme
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: CalendarEvent,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    theme: AppTheme,
    lang: String
) {
    val isSpanish = lang.contains("Español")
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Color del evento (propio o acento del tema)
    val eventColor = remember(event.colorHex) {
        if (event.colorHex.isNotEmpty())
            try { Color(android.graphics.Color.parseColor(event.colorHex)) }
            catch (e: Exception) { null }
        else null
    } ?: theme.accentColor

    val locale = if (isSpanish) java.util.Locale.forLanguageTag("es-ES") else Locale.ENGLISH

    // Fecha formateada: "Lunes, 2 de Junio 2025" / "Monday, June 2, 2025"
    val dateFormatted = remember(event.date, lang) {
        val dayName = event.date.dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.uppercase() }
        val monthName = event.date.month
            .getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.uppercase() }
        if (isSpanish) "$dayName, ${event.date.dayOfMonth} de $monthName ${event.date.year}"
        else "$dayName, $monthName ${event.date.dayOfMonth}, ${event.date.year}"
    }

    val timeDisplay = when {
        event.isAllDay -> if (isSpanish) "Todo el día" else "All day"
        event.endTime.isNotEmpty() -> "${event.time} – ${event.endTime}"
        event.time.isNotEmpty() -> event.time
        else -> ""
    }

    val recurrenceLabel = when (event.recurrence) {
        RecurrenceType.NONE    -> null
        RecurrenceType.DAILY   -> if (isSpanish) "Se repite diariamente"  else "Repeats daily"
        RecurrenceType.WEEKLY  -> if (isSpanish) "Se repite semanalmente" else "Repeats weekly"
        RecurrenceType.MONTHLY -> if (isSpanish) "Se repite mensualmente" else "Repeats monthly"
        RecurrenceType.YEARLY  -> if (isSpanish) "Se repite anualmente"   else "Repeats yearly"
    }

    val notifLabel = if (event.notification.enabled) {
        when (event.notification.type) {
            NotificationType.SAME_DAY        -> if (isSpanish) "Notificación el mismo día"    else "Notification same day"
            NotificationType.ONE_DAY_BEFORE  -> if (isSpanish) "Notificación 1 día antes"     else "Notification 1 day before"
            NotificationType.TWO_DAYS_BEFORE -> if (isSpanish) "Notificación 2 días antes"    else "Notification 2 days before"
            NotificationType.ONE_WEEK_BEFORE -> if (isSpanish) "Notificación 1 semana antes"  else "Notification 1 week before"
        }
    } else null

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = theme.systemBackgroundColor,
        contentColor = theme.getMainTextColor(),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.18f))
            )
        }
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)) + slideInVertically(tween(280)) { it / 5 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // Barra de color del evento en la parte superior
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(eventColor, eventColor.copy(alpha = 0.35f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 20.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Título
                    Text(
                        text = event.title,
                        color = theme.getMainTextColor(),
                        fontSize = (22 * theme.globalTextScale).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = theme.getFontFamily()
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    // Fecha y hora
                    DetailRow(
                        icon = {
                            Icon(
                                Icons.Default.DateRange, contentDescription = if (isSpanish) "Fecha" else "Date",
                                tint = eventColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    ) {
                        Column {
                            Text(
                                text = dateFormatted,
                                color = theme.getMainTextColor(),
                                fontSize = (14 * theme.globalTextScale).sp,
                                fontFamily = theme.getFontFamily()
                            )
                            if (timeDisplay.isNotEmpty()) {
                                Text(
                                    text = timeDisplay,
                                    color = theme.getLabelColor(),
                                    fontSize = (13 * theme.globalTextScale).sp,
                                    fontFamily = theme.getFontFamily()
                                )
                            }
                        }
                    }

                    // Recurrencia
                    if (recurrenceLabel != null) {
                        DetailRow(
                            icon = {
                                Icon(
                                    Icons.Default.Refresh, contentDescription = if (isSpanish) "Repetición" else "Recurrence",
                                    tint = eventColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        ) {
                            Text(
                                text = recurrenceLabel,
                                color = theme.getLabelColor(),
                                fontSize = (13 * theme.globalTextScale).sp,
                                fontFamily = theme.getFontFamily()
                            )
                        }
                    }

                    // Notificación
                    if (notifLabel != null) {
                        DetailRow(
                            icon = {
                                Icon(
                                    Icons.Default.Notifications, contentDescription = if (isSpanish) "Notificación" else "Notification",
                                    tint = eventColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        ) {
                            Text(
                                text = notifLabel,
                                color = theme.getLabelColor(),
                                fontSize = (13 * theme.globalTextScale).sp,
                                fontFamily = theme.getFontFamily()
                            )
                        }
                    }

                    // Ubicación (clickeable → abre Google Maps)
                    if (event.location.isNotEmpty()) {
                        DetailRow(
                            icon = {
                                Icon(
                                    Icons.Default.LocationOn, contentDescription = if (isSpanish) "Ubicación" else "Location",
                                    tint = eventColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        ) {
                            Text(
                                text = event.location,
                                color = eventColor,
                                fontSize = (14 * theme.globalTextScale).sp,
                                fontFamily = theme.getFontFamily(),
                                modifier = Modifier.clickable {
                                    val uri = Uri.parse("geo:0,0?q=${Uri.encode(event.location)}")
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }.onFailure {
                                        Toast.makeText(context, if (isSpanish) "No se encontró app de mapas" else "No maps app found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    // URL (clickeable → abre navegador)
                    if (event.url.isNotEmpty()) {
                        DetailRow(
                            icon = {
                                Icon(
                                    Icons.Default.Share, contentDescription = if (isSpanish) "Enlace" else "Link",
                                    tint = eventColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        ) {
                            Text(
                                text = event.url,
                                color = eventColor,
                                fontSize = (13 * theme.globalTextScale).sp,
                                fontFamily = theme.getFontFamily(),
                                modifier = Modifier.clickable {
                                    val rawUrl = event.url
                                    val url = if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl"
                                    val parsedUri = Uri.parse(url)
                                    // A4: Solo permitir http/https para evitar ataques via ICS
                                    if (parsedUri.scheme in listOf("http", "https")) {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, parsedUri))
                                        }.onFailure {
                                            Toast.makeText(context, if (isSpanish) "No se encontró navegador" else "No browser found", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, if (isSpanish) "URL no válida" else "Invalid URL", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }

                    // Foto del evento
                    if (event.photoUri.isNotEmpty()) {
                        AsyncImage(
                            model = event.photoUri,
                            contentDescription = if (isSpanish) "Foto del evento" else "Event photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(theme.getBorderRadius())),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Nota
                    if (event.note.isNotEmpty()) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        DetailRow(
                            icon = {
                                Icon(
                                    Icons.Default.Edit, contentDescription = if (isSpanish) "Nota" else "Note",
                                    tint = theme.getLabelColor(),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        ) {
                            Text(
                                text = event.note,
                                color = theme.getLabelColor(),
                                fontSize = (13 * theme.globalTextScale).sp,
                                fontFamily = theme.getFontFamily()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Botones de acción: Eliminar / Editar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDelete(); onDismiss() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                            shape = RoundedCornerShape(theme.getBorderRadius())
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = if (isSpanish) "Eliminar" else "Delete", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSpanish) "Eliminar" else "Delete",
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = theme.getFontFamily()
                            )
                        }
                        Button(
                            onClick = { onEdit(); onDismiss() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = eventColor),
                            shape = RoundedCornerShape(theme.getBorderRadius())
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = if (isSpanish) "Editar" else "Edit", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSpanish) "Editar" else "Edit",
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = theme.getFontFamily()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(top = 2.dp)) { icon() }
        content()
    }
}
