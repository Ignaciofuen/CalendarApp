package com.example.calendarapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToMain: () -> Unit) {
    // ── Animatables existentes ──────────────────────────────────────────────
    val logoScale = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    var progress by remember { mutableStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "ProgressBar"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnim")

    // Existente: pulso interior del logo
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "GlowAlpha"
    )

    // NUEVO: anillo exterior pulsante
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "RingScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "RingAlpha"
    )

    // NUEVO: barrido shimmer horizontal sobre el logo
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -160f, targetValue = 160f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "Shimmer"
    )

    // NUEVO: 3 puntos de carga con fases desfasadas
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(600, delayMillis = 200, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "Dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(600, delayMillis = 400, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "Dot3"
    )

    // ── Coreografía existente (sin cambios) ────────────────────────────────
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        contentAlpha.animateTo(targetValue = 1f, animationSpec = tween(500))
        progress = 1f
        delay(1800)
        onNavigateToMain()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF031410)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Logo central ────────────────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {

                // NUEVO: anillo exterior que desvanece al expandirse
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .graphicsLayer {
                            scaleX = ringScale
                            scaleY = ringScale
                            alpha = ringAlpha
                        }
                        .background(
                            Color(0xFF00E676).copy(alpha = 0.18f),
                            RoundedCornerShape(32.dp)
                        )
                )

                // Caja del logo (existente) + shimmer superpuesto
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A1D1C))
                        .padding(2.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E676).copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Contenido existente: línea + "24"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        HorizontalDivider(
                            color = Color(0xFF00E676),
                            thickness = 2.dp,
                            modifier = Modifier.width(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "24",
                            color = Color(0xFF00E676),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // NUEVO: barrido shimmer sobre el logo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .graphicsLayer { translationX = shimmerOffset }
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.11f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            // ── Contenido inferior con fade-in existente ────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
            ) {
                Text(
                    text = "INITIALIZING ENGINE",
                    color = Color(0xFF00E676).copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Barra de carga — ahora con gradiente en el fill
                Box(
                    modifier = Modifier
                        .width(250.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00E676).copy(alpha = 0.55f),
                                        Color(0xFF00E676)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // NUEVO: 3 puntos de carga animados (reemplazan al texto "Cargando...")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(5.dp)
                            .graphicsLayer { alpha = dot1Alpha }
                            .background(Color(0xFF00E676), CircleShape)
                    )
                    Box(
                        Modifier.size(5.dp)
                            .graphicsLayer { alpha = dot2Alpha }
                            .background(Color(0xFF00E676), CircleShape)
                    )
                    Box(
                        Modifier.size(5.dp)
                            .graphicsLayer { alpha = dot3Alpha }
                            .background(Color(0xFF00E676), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(44.dp))

                // Branding existente
                Row {
                    Text("CALENDAR ", color = Color.Gray, fontSize = 12.sp, letterSpacing = 4.sp)
                    Text("PRO", color = Color(0xFF00E676), fontSize = 12.sp, letterSpacing = 4.sp)
                }
            }
        }
    }
}
