package com.example.calendarapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.calendarapp.ui.viewmodel.AppTheme
import com.example.calendarapp.ui.viewmodel.DayMarkerStyle
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import java.time.DayOfWeek

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDayComponent(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    theme: AppTheme,
    hasEvent: Boolean = false,
    eventCount: Int = 0,
    eventColor: Color? = null,
    maxEventsInMonth: Int = 1,
    isHoliday: Boolean = false,
    onClick: (CalendarDay) -> Unit,
    onLongClick: ((CalendarDay) -> Unit)? = null
) {
    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SelectionScale"
    )

    // Pulsación infinita del anillo exterior en el día de hoy
    val infiniteTransition = rememberInfiniteTransition(label = "TodayPulse")
    val todayRingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TodayRingScale"
    )
    val todayRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TodayRingAlpha"
    )

    val isWeekend = day.date.dayOfWeek == DayOfWeek.SATURDAY || day.date.dayOfWeek == DayOfWeek.SUNDAY
    val isMonthDate = day.position == DayPosition.MonthDate

    // Los feriados se muestran con el color de fines de semana (rojo) para destacarlos
    val dayTextColor = when {
        isSelected -> Color.White
        isToday && theme.markerStyle == DayMarkerStyle.CLASSIC -> Color.Black
        isMonthDate && isHoliday && !isSelected -> theme.weekendColor
        isMonthDate -> if (isWeekend) theme.weekendColor else theme.getMainTextColor()
        else -> Color.Gray
    }

    // Mapa de calor: opacidad proporcional al número de eventos
    val heatAlpha = if (isMonthDate && eventCount > 0 && maxEventsInMonth > 0 && !isToday && !isSelected) {
        (eventCount.toFloat() / maxEventsInMonth.coerceAtLeast(1)).coerceIn(0.05f, 0.30f)
    } else 0f

    val semanticDescription = buildString {
        append(day.date.dayOfMonth.toString())
        if (isSelected) append(", Selected")
        if (isToday) append(", Today")
        if (isHoliday) append(", Holiday")
        if (eventCount > 0) append(", $eventCount event${if (eventCount > 1) "s" else ""}")
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .alpha(if (isMonthDate) 1f else 0.3f)
            .semantics { contentDescription = semanticDescription }
            .combinedClickable(
                onClick = { onClick(day) },
                onLongClick = { onLongClick?.invoke(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Fondo de mapa de calor (solo días del mes con eventos)
        if (heatAlpha > 0f) {
            val base = eventColor ?: theme.accentColor
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(1.dp)
                    .background(base.copy(alpha = heatAlpha), RoundedCornerShape(6.dp))
            )
        }

        // Fondo suave para feriados (solo días del mes, no seleccionados, no hoy)
        if (isHoliday && isMonthDate && !isToday && !isSelected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .background(theme.weekendColor.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
            )
        }

        // Anillo pulsante exterior — solo en el día de hoy
        // Usamos .scale() y .alpha() en lugar de graphicsLayer{} para evitar conflictos de import
        if (isToday) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .scale(todayRingScale)
                    .alpha(todayRingAlpha)
                    .background(
                        theme.accentColor.copy(alpha = 0.30f),
                        CircleShape
                    )
            )
        }

        // Marcador del día de hoy
        if (isToday) MarkerShape(theme.accentColor, theme.markerStyle)

        // Borde de selección cuando no es hoy
        if (isSelected && !isToday) {
            Box(Modifier.size(36.dp).border(2.dp, theme.accentColor, CircleShape))
        }

        // Número del día
        Text(
            text = day.date.dayOfMonth.toString(),
            style = TextStyle(
                color = dayTextColor,
                fontFamily = theme.getFontFamily(),
                fontWeight = if (isSelected || isToday || isHoliday || theme.calendarTextBrightness > 1.2f)
                    FontWeight.Bold else FontWeight.Normal,
                fontSize = (14 * theme.calendarTextBrightness * theme.globalTextScale).sp,
                shadow = if (theme.textShadowIntensity > 0) Shadow(
                    color = Color.Black.copy(alpha = (theme.textShadowIntensity / 100f).coerceIn(0.1f, 1f)),
                    offset = Offset(theme.textShadowIntensity / 15f, theme.textShadowIntensity / 15f),
                    blurRadius = (theme.textShadowIntensity / 8f).coerceAtLeast(0.5f)
                ) else null
            ),
            modifier = Modifier.scale(selectionScale)
        )

        // Pequeña bandera en esquina superior derecha para feriados
        if (isHoliday && isMonthDate && !isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(5.dp)
                    .background(theme.weekendColor, CircleShape)
            )
        }

        // Badge de eventos (aparece en la parte inferior)
        val effectiveCount = eventCount.coerceAtLeast(if (hasEvent) 1 else 0)
        if (effectiveCount > 0 && isMonthDate) {
            val base = eventColor ?: theme.accentColor
            val dotColor = when {
                isToday    -> Color.Black.copy(alpha = 0.55f)
                isSelected -> base.copy(alpha = 0.7f)
                else       -> base
            }
            if (effectiveCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .defaultMinSize(minWidth = 14.dp, minHeight = 10.dp)
                        .background(dotColor, RoundedCornerShape(5.dp))
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (effectiveCount > 9) "9+" else effectiveCount.toString(),
                        color = Color.Black,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .size(4.dp)
                        .background(dotColor, CircleShape)
                )
            }
        }
    }
}

@Composable
fun MarkerShape(accentColor: Color, style: DayMarkerStyle) {
    when (style) {
        DayMarkerStyle.CLASSIC ->
            Box(Modifier.size(36.dp).background(accentColor, CircleShape))
        DayMarkerStyle.SQUARE_FILLED ->
            Box(Modifier.size(34.dp).background(accentColor, RoundedCornerShape(8.dp)))
        DayMarkerStyle.SQUARE ->
            Box(Modifier.size(36.dp).border(2.dp, accentColor, RoundedCornerShape(10.dp)))
        DayMarkerStyle.SPHERE_3D ->
            Box(Modifier.size(36.dp).background(
                Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.8f), accentColor, Color.Black.copy(alpha = 0.3f)),
                    center = Offset(30f, 30f)
                ), CircleShape
            ))
        DayMarkerStyle.MINIMAL ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(26.dp))
                Box(Modifier.width(16.dp).height(3.dp).background(accentColor, RoundedCornerShape(2.dp)))
            }
        DayMarkerStyle.INSET ->
            Box(Modifier.size(36.dp)
                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)))
    }
}
