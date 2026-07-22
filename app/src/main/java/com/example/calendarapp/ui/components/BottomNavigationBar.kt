package com.example.calendarapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.calendarapp.ui.viewmodel.SettingsViewModel
import com.example.calendarapp.ui.viewmodel.ThemeViewModel
import com.example.calendarapp.utils.AppStrings

private data class NavItem(
    val route: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
    val stringKey: String
)

@Composable
fun BottomNavigationBar(
    navController: NavController,
    themeViewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val theme by themeViewModel.currentTheme
    val lang by settingsViewModel.currentLanguage.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        NavItem("calendar", Icons.Filled.DateRange,  Icons.Outlined.DateRange,  "calendar"),
        NavItem("agenda",   Icons.Filled.List,       Icons.Outlined.List,       "schedule"),
        NavItem("design",   Icons.Filled.Face,       Icons.Outlined.Face,       "design"),
        NavItem("settings", Icons.Filled.Settings,   Icons.Outlined.Settings,   "settings")
    )

    // Fondo del bar con gradiente sutil
    val barBackground = Brush.verticalGradient(
        colors = listOf(
            theme.systemBackgroundColor.copy(alpha = 0.0f),
            theme.systemBackgroundColor.copy(alpha = 0.95f),
            theme.systemBackgroundColor
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // windowInsetsPadding agrega padding dinámico para los botones de navegación
            // del sistema (3 botones clásicos o barra de gestos).
            // Si el sistema usa navegación gestual, el inset es 0 y no cambia nada.
            // Si el sistema usa botones, la barra sube automáticamente.
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(barBackground)
    ) {
        // Línea separadora con gradiente suave
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            theme.accentColor.copy(alpha = 0.20f),
                            theme.accentColor.copy(alpha = 0.35f),
                            theme.accentColor.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route

                // Color del fondo del pill
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) theme.accentColor.copy(alpha = 0.14f)
                                  else Color.Transparent,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    label = "NavBg_${item.route}"
                )

                // Color del icono
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) theme.accentColor else Color.Gray.copy(alpha = 0.7f),
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    label = "NavIcon_${item.route}"
                )

                // Escala con spring al seleccionar
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.18f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "NavScale_${item.route}"
                )

                // Alpha del glow detrás del icono
                val glowAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 0.22f else 0f,
                    animationSpec = tween(durationMillis = 350),
                    label = "NavGlow_${item.route}"
                )

                // Alpha del label — evitamos AnimatedVisibility dentro del Row para no capturar RowScope
                val labelAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "NavLabel_${item.route}"
                )
                val labelHeight by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 180),
                    label = "NavLabelH_${item.route}"
                )

                // Alpha del indicador inferior
                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(durationMillis = 250),
                    label = "NavIndicator_${item.route}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Icono con glow radial detrás
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(28.dp)
                        ) {
                            // Glow radial animado
                            if (glowAlpha > 0f) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    theme.accentColor.copy(alpha = glowAlpha),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        )
                                )
                            }

                            Icon(
                                imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                                contentDescription = AppStrings.get(item.stringKey, lang),
                                tint = iconColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                        }

                        // Label animado con alpha — evita conflicto con RowScope.AnimatedVisibility
                        if (labelHeight > 0f) {
                            Text(
                                text = AppStrings.get(item.stringKey, lang),
                                color = theme.accentColor.copy(alpha = labelAlpha),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = theme.getFontFamily(),
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .graphicsLayer { alpha = labelAlpha }
                            )
                        }
                    }

                    // Indicador de línea brillante — con alpha animado para evitar RowScope conflict
                    if (indicatorAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 3.dp)
                                .width(20.dp)
                                .height(2.dp)
                                .graphicsLayer { alpha = indicatorAlpha }
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            theme.accentColor.copy(alpha = 0.8f),
                                            Color.Transparent
                                        )
                                    ),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
