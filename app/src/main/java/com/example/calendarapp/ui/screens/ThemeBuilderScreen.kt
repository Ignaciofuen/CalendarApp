package com.example.calendarapp.ui.screens

import android.content.Intent
import android.net.Uri
import java.time.LocalDate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.calendarapp.ui.components.MarkerShape
import com.example.calendarapp.ui.theme.*
import com.example.calendarapp.ui.viewmodel.*
import com.example.calendarapp.utils.AppStrings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ThemeBuilderScreen(
    viewModel: ThemeViewModel,
    settingsViewModel: SettingsViewModel,
    navController: NavController
) {
    val lang by settingsViewModel.currentLanguage.collectAsState()

    key(lang) {
        val theme by viewModel.currentTheme
        val scrollState = rememberScrollState()
        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        Scaffold(
            topBar = {
                Box {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (lang.contains("Español")) "Diseñador de Temas" else "Theme Designer",
                                color = theme.getMainTextColor(),
                                fontFamily = theme.getFontFamily(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (lang.contains("Español")) "Volver" else "Go back",
                                    tint = theme.accentColor
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = theme.systemBackgroundColor
                        )
                    )
                    // Línea inferior con gradiente de acento
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        theme.accentColor.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        ) { padding ->
            var showEditDialog by remember { mutableStateOf(false) }

            val showBg = theme.showBackgroundImages
            Box(modifier = Modifier.fillMaxSize().background(theme.systemBackgroundColor)) {
                val currentMonthVal = LocalDate.now().monthValue
                val backgroundUri = if (showBg) {
                    theme.backgroundImages[currentMonthVal] ?: theme.globalBackgroundImage
                } else null

                key(backgroundUri) {
                    if (backgroundUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(backgroundUri)
                                .crossfade(true)
                                .setParameter("allow_hardware", false)
                                .build(),
                            contentDescription = if (lang.contains("Español")) "Imagen de fondo" else "Background image",
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(theme.blurAmount.dp)
                                .graphicsLayer(
                                    rotationZ = theme.bgRotation,
                                    scaleX = theme.bgScale,
                                    scaleY = theme.bgScale,
                                    translationX = theme.bgOffsetX,
                                    translationY = theme.bgOffsetY
                                ),
                            contentScale = ContentScale.Crop,
                            alpha = theme.backgroundOpacity
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {

                    // ── 0. PREVIEW EN VIVO ───────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        theme.systemBackgroundColor,
                                        theme.accentColor.copy(alpha = 0.07f),
                                        theme.systemBackgroundColor
                                    )
                                )
                            )
                            .border(1.dp, theme.accentColor.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (lang.contains("Español")) "Vista previa en vivo" else "Live Preview",
                                    color = theme.getLabelColor(),
                                    fontSize = 10.sp,
                                    fontFamily = theme.getFontFamily(),
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(theme.accentColor.copy(alpha = 0.14f), RoundedCornerShape(5.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (lang.contains("Español")) "EN VIVO" else "LIVE",
                                        color = theme.accentColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            val dayLabels = if (lang.contains("Español"))
                                listOf("L","M","X","J","V","S","D")
                            else
                                listOf("M","T","W","T","F","S","S")
                            val nums = listOf("20","21","22","23","24","25","26")
                            Row(modifier = Modifier.fillMaxWidth()) {
                                dayLabels.forEach { d ->
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        Text(d, color = theme.getLabelColor(), fontSize = 8.sp, fontFamily = theme.getFontFamily())
                                    }
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                nums.forEachIndexed { i, n ->
                                    val isToday = n == "26"
                                    val isWeekend = i >= 5
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        if (isToday) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(theme.accentColor, RoundedCornerShape(theme.getBorderRadius()))
                                            )
                                        } else if (n == "21") {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .offset(y = (-13).dp)
                                                    .background(theme.accentColor.copy(0.6f), CircleShape)
                                            )
                                        }
                                        Text(
                                            text = n,
                                            color = if (isToday) Color.Black
                                                    else if (isWeekend) theme.weekendColor
                                                    else theme.getMainTextColor(),
                                            fontSize = (10 * theme.globalTextScale).sp,
                                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                            fontFamily = theme.getFontFamily()
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // ── 1. FONDO GLOBAL ───────────────────────────────────────
                    SectionLabel(
                        text = if (lang.contains("Español")) "FONDO GLOBAL" else "GLOBAL BACKGROUND",
                        icon = Icons.Default.Image,
                        theme = theme
                    )
                    Spacer(Modifier.height(14.dp))

                    val globalLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri: Uri? ->
                        uri?.let {
                            try {
                                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                viewModel.saveGlobalBackgroundImage(it.toString())
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = if (theme.globalBackgroundImage != null)
                                    Brush.linearGradient(listOf(Color(0xFF1A1D1C), Color(0xFF1A1D1C)))
                                else
                                    Brush.linearGradient(listOf(Color(0xFF1A1D1C), Color(0xFF0F1210)))
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        theme.accentColor.copy(alpha = 0.2f),
                                        theme.accentColor.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .combinedClickable(
                                onClick = { globalLauncher.launch(arrayOf("image/*")) },
                                onLongClick = {
                                    if (theme.globalBackgroundImage != null) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showEditDialog = true
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (theme.globalBackgroundImage != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(theme.globalBackgroundImage)
                                    .crossfade(true)
                                    .setParameter("allow_hardware", false)
                                    .build(),
                                contentDescription = if (lang.contains("Español")) "Vista previa del fondo global" else "Global background preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.55f
                            )
                            // Badge "Mantén pulsado para editar"
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (lang.contains("Español")) "Mantén para editar" else "Hold to edit",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.saveGlobalBackgroundImage(null) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = if (lang.contains("Español")) "Eliminar fondo" else "Remove background", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(theme.accentColor.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = if (lang.contains("Español")) "Establecer fondo" else "Set background image",
                                        tint = theme.accentColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = if (lang.contains("Español")) "Establecer fondo global" else "Set global background",
                                    color = theme.getLabelColor(),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    ThemeDivider(theme.accentColor)

                    // ── 2. FONDOS MENSUALES ───────────────────────────────────
                    SectionLabel(
                        text = if (lang.contains("Español")) "FONDOS MENSUALES" else "MONTHLY BACKGROUNDS",
                        icon = Icons.Default.CalendarViewMonth,
                        theme = theme
                    )
                    Spacer(Modifier.height(14.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..12).forEach { month ->
                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.OpenDocument()
                            ) { uri: Uri? ->
                                uri?.let {
                                    try {
                                        context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        viewModel.saveBackgroundImage(month, it.toString())
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                MonthBox(
                                    month = month,
                                    theme = theme,
                                    lang = lang,
                                    imageUri = theme.backgroundImages[month],
                                    onDelete = { viewModel.saveBackgroundImage(month, null) },
                                    onClick = { launcher.launch(arrayOf("image/*")) }
                                )
                            }
                        }
                    }

                    ThemeDivider(theme.accentColor)

                    // ── 3. VISIBILIDAD Y EFECTOS ──────────────────────────────
                    SectionLabel(
                        text = if (lang.contains("Español")) "VISIBILIDAD Y EFECTOS" else "VISIBILITY & EFFECTS",
                        icon = Icons.Default.Tune,
                        theme = theme
                    )
                    Spacer(Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141716))
                            .border(1.dp, theme.accentColor.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        EffectSlider(
                            label = if (lang.contains("Español")) "Presencia del Fondo" else "Background Presence",
                            value = theme.backgroundOpacity,
                            range = 0f..1f,
                            accentColor = theme.accentColor
                        ) { viewModel.updateBackgroundOpacity(it) }

                        HorizontalDivider(color = theme.accentColor.copy(alpha = 0.06f))

                        EffectSlider(
                            label = if (lang.contains("Español")) "Desenfoque (Blur)" else "Blur Amount",
                            value = theme.blurAmount,
                            range = 0f..30f,
                            accentColor = theme.accentColor,
                            isPercentage = false
                        ) { viewModel.updateBlurAmount(it) }

                        HorizontalDivider(color = theme.accentColor.copy(alpha = 0.06f))

                        EffectSlider(
                            label = if (lang.contains("Español")) "Opacidad de Tarjetas" else "Card Opacity",
                            value = theme.cardOpacity,
                            range = 0f..1f,
                            accentColor = theme.accentColor
                        ) { viewModel.updateCardOpacity(it) }

                        HorizontalDivider(color = theme.accentColor.copy(alpha = 0.06f))

                        // Interruptor mostrar imágenes
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (lang.contains("Español")) "Mostrar Imágenes de Fondo" else "Show Background Images",
                                    color = theme.getMainTextColor(),
                                    fontSize = (13 * theme.globalTextScale).sp,
                                    fontFamily = theme.getFontFamily()
                                )
                                Text(
                                    text = if (lang.contains("Español")) "Aplica globales y mensuales" else "Applies global & monthly",
                                    color = theme.getLabelColor(),
                                    fontSize = 11.sp,
                                    fontFamily = theme.getFontFamily()
                                )
                            }
                            Switch(
                                checked = theme.showBackgroundImages,
                                onCheckedChange = { viewModel.updateShowBackgroundImages(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.accentColor,
                                    checkedTrackColor = theme.accentColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Reset button
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.resetVisibilitySettings()
                        },
                        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.accentColor)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = if (lang.contains("Español")) "Resetear visibilidad" else "Reset visibility", modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (lang.contains("Español")) "Resetear Visibilidad" else "Reset Visibility",
                            fontSize = (12 * theme.globalTextScale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = theme.getFontFamily()
                        )
                    }

                    ThemeDivider(theme.accentColor)

                    // ── 4. AJUSTES DE TEXTO ───────────────────────────────────
                    SectionLabel(
                        text = if (lang.contains("Español")) "AJUSTES DE TEXTO" else "TEXT SETTINGS",
                        icon = Icons.Default.TextFields,
                        theme = theme
                    )
                    Spacer(Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141716))
                            .border(1.dp, theme.accentColor.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        EffectSlider(
                            label = if (lang.contains("Español")) "Brillo del Texto" else "Text Brightness",
                            value = theme.calendarTextBrightness,
                            range = 0.5f..1.5f,
                            accentColor = theme.accentColor,
                            isPercentage = false
                        ) { viewModel.updateCalendarTextBrightness(it) }

                        HorizontalDivider(color = theme.accentColor.copy(alpha = 0.06f))

                        // Sombra de números con preview
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            EffectSlider(
                                label = if (lang.contains("Español")) "Sombra de números" else "Number Shadow",
                                value = theme.textShadowIntensity,
                                range = 0f..100f,
                                accentColor = theme.accentColor,
                                isPercentage = false
                            ) { viewModel.updateTextShadowIntensity(it) }

                            // Preview inline
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = if (lang.contains("Español")) "Vista previa:" else "Preview:",
                                    color = theme.getLabelColor(),
                                    fontSize = 11.sp,
                                    fontFamily = theme.getFontFamily(),
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "25",
                                        style = TextStyle(
                                            color = theme.getMainTextColor(),
                                            fontFamily = theme.getFontFamily(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (18 * theme.calendarTextBrightness).sp,
                                            shadow = if (theme.textShadowIntensity > 0) Shadow(
                                                color = Color.Black.copy(alpha = (theme.textShadowIntensity / 100f).coerceIn(0.1f, 1f)),
                                                offset = Offset(theme.textShadowIntensity / 15f, theme.textShadowIntensity / 15f),
                                                blurRadius = (theme.textShadowIntensity / 8f).coerceAtLeast(0.5f)
                                            ) else null
                                        )
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.updateTextShadowIntensity(0f) },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = if (lang.contains("Español")) "Resetear sombra" else "Reset shadow", tint = theme.accentColor.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Reset", color = theme.accentColor.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Vinculación con acento
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141716))
                            .border(1.dp, theme.accentColor.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (lang.contains("Español")) "Vincular Etiquetas con Acento" else "Link Labels with Accent",
                                    color = theme.getMainTextColor(),
                                    fontSize = (13 * theme.globalTextScale).sp,
                                    fontFamily = theme.getFontFamily()
                                )
                            }
                            Switch(
                                checked = theme.syncLabelsWithAccent,
                                onCheckedChange = { viewModel.updateSyncLabelsWithAccent(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.accentColor,
                                    checkedTrackColor = theme.accentColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                        HorizontalDivider(color = theme.accentColor.copy(alpha = 0.06f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (lang.contains("Español")) "Vincular Texto con Acento" else "Link Main Text with Accent",
                                    color = theme.getMainTextColor(),
                                    fontSize = (13 * theme.globalTextScale).sp,
                                    fontFamily = theme.getFontFamily()
                                )
                            }
                            Switch(
                                checked = theme.syncMainTextWithAccent,
                                onCheckedChange = { viewModel.updateSyncMainTextWithAccent(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.accentColor,
                                    checkedTrackColor = theme.accentColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    AdvancedColorSelector(
                        label = if (lang.contains("Español")) "COLOR TEXTO PRINCIPAL" else "MAIN TEXT COLOR",
                        selectedColor = theme.getMainTextColor(),
                        isDefault = !theme.isCustomTextActive,
                        lang = lang, theme = theme,
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateCalendarTextColor(Color.White)
                        }
                    ) { if (!theme.syncMainTextWithAccent) viewModel.updateCalendarTextColor(it) }

                    Spacer(Modifier.height(14.dp))

                    AdvancedColorSelector(
                        label = if (lang.contains("Español")) "COLOR DE ETIQUETAS" else "LABEL TEXT COLOR",
                        selectedColor = theme.getLabelColor(),
                        isDefault = false,
                        lang = lang, theme = theme,
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateSecondaryTextColor(Color.Gray)
                        }
                    ) { if (!theme.syncLabelsWithAccent) viewModel.updateSecondaryTextColor(it) }

                    Spacer(Modifier.height(14.dp))

                    // Escala global de texto
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141716))
                            .border(1.dp, theme.accentColor.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (lang.contains("Español")) "Tamaño global de texto" else "Global Text Size",
                                color = theme.getMainTextColor(),
                                fontSize = (13 * theme.globalTextScale).sp,
                                fontFamily = theme.getFontFamily(),
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(theme.accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${"%.1f".format(theme.globalTextScale)}x",
                                    color = theme.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(theme.accentColor.copy(alpha = 0.10f), CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.updateGlobalTextScale(1.0f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = if (lang.contains("Español")) "Resetear tamaño de texto" else "Reset text size",
                                    tint = theme.accentColor.copy(alpha = 0.75f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("0.8x", color = theme.getLabelColor(), fontSize = 10.sp)
                            Slider(
                                value = theme.globalTextScale,
                                onValueChange = { viewModel.updateGlobalTextScale(it) },
                                valueRange = 0.8f..1.5f,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = theme.accentColor,
                                    activeTrackColor = theme.accentColor
                                )
                            )
                            Text("1.5x", color = theme.getLabelColor(), fontSize = 10.sp)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.resetCalendarTextSettings()
                        },
                        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.accentColor)
                    ) {
                        Icon(Icons.Default.FormatColorReset, contentDescription = if (lang.contains("Español")) "Resetear texto" else "Reset text", modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (lang.contains("Español")) "Resetear Texto" else "Reset Text",
                            fontSize = (12 * theme.globalTextScale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = theme.getFontFamily()
                        )
                    }

                    ThemeDivider(theme.accentColor)

                    // ── 4.5 COLORES DEL SISTEMA ───────────────────────────────
                    SectionLabel(
                        text = if (lang.contains("Español")) "COLORES DEL SISTEMA" else "SYSTEM COLORS",
                        icon = Icons.Default.Palette,
                        theme = theme
                    )
                    Spacer(Modifier.height(14.dp))

                    AdvancedColorSelector(
                        label = if (lang.contains("Español")) "COLOR DE FONDO GENERAL" else "MAIN BACKGROUND COLOR",
                        selectedColor = theme.systemBackgroundColor,
                        lang = lang, theme = theme,
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateSystemBackgroundColor(Color(0xFF0A0C0B))
                        }
                    ) { viewModel.updateSystemBackgroundColor(it) }

                    Spacer(Modifier.height(6.dp))

                    AdvancedColorSelector(
                        label = if (lang.contains("Español")) "COLOR DE FINES DE SEMANA" else "WEEKEND COLOR",
                        selectedColor = theme.weekendColor,
                        lang = lang, theme = theme,
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateWeekendColor(Color(0xFFFF5252))
                        }
                    ) { viewModel.updateWeekendColor(it) }

                    ThemeDivider(theme.accentColor)

                    // ── 5. TIPOGRAFÍA ─────────────────────────────────────────
                    TypographySelector(
                        selectedStyle = theme.typographyStyle,
                        accentColor = theme.accentColor,
                        lang = lang, theme = theme
                    ) { viewModel.updateTypography(it) }

                    ThemeDivider(theme.accentColor)

                    // ── 5.5 COLOR DE ACENTO ───────────────────────────────────
                    SectionLabel(
                        text = if (lang.contains("Español")) "COLOR DE ACENTO" else "ACCENT COLOR",
                        icon = Icons.Default.ColorLens,
                        theme = theme
                    )
                    Spacer(Modifier.height(14.dp))
                    AdvancedColorSelector(
                        label = if (lang.contains("Español")) "COLOR DE ACENTO" else "ACCENT COLOR",
                        selectedColor = theme.accentColor,
                        lang = lang, theme = theme,
                        hideLabel = true,
                        onReset = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.updateAccentColor(Color(0xFF00E676))
                        }
                    ) { viewModel.updateAccentColor(it) }

                    ThemeDivider(theme.accentColor)

                    // ── 6. MARCADOR DEL DÍA ───────────────────────────────────
                    DayMarkerSelector(
                        selectedStyle = theme.markerStyle,
                        accentColor = theme.accentColor,
                        lang = lang, theme = theme
                    ) { viewModel.updateMarkerStyle(it) }

                    ThemeDivider(theme.accentColor)

                    // ── 7. ESTILO DE BORDES ───────────────────────────────────
                    SectionLabel(
                        text = if (lang.contains("Español")) "ESTILO DE BORDES" else "BORDER STYLE",
                        icon = Icons.Default.RoundedCorner,
                        theme = theme
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        (0..4).forEach { index ->
                            val radius = when (index) { 0 -> 0.dp; 1 -> 8.dp; 2 -> 16.dp; 3 -> 28.dp; else -> 100.dp }
                            val isSelected = theme.borderStyle == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radius))
                                    .background(
                                        if (isSelected) theme.accentColor.copy(alpha = 0.12f)
                                        else Color(0xFF1A1D1C)
                                    )
                                    .border(
                                        2.dp,
                                        if (isSelected) theme.accentColor else Color.DarkGray.copy(alpha = 0.5f),
                                        RoundedCornerShape(radius)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.updateBorderStyle(index)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = if (lang.contains("Español")) "Estilo seleccionado" else "Selected style",
                                        tint = theme.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }

                // Diálogo edición de fondo
                if (showEditDialog) {
                    Dialog(onDismissRequest = { showEditDialog = false }) {
                        var localRotation by remember { mutableFloatStateOf(theme.bgRotation) }
                        var localOffsetX  by remember { mutableFloatStateOf(theme.bgOffsetX) }
                        var localOffsetY  by remember { mutableFloatStateOf(theme.bgOffsetY) }
                        var localScale    by remember { mutableFloatStateOf(theme.bgScale) }

                        // Aplica cambios en tiempo real al ViewModel
                        LaunchedEffect(localRotation, localOffsetX, localOffsetY, localScale) {
                            viewModel.updateBackgroundTransformation(localRotation, localOffsetX, localOffsetY, localScale)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp)),
                            color = Color(0xFF141716),
                            tonalElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // ── Cabecera ──────────────────────────────────────────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(theme.accentColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = if (lang.contains("Español")) "Editar imagen" else "Edit image", tint = theme.accentColor, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = if (lang.contains("Español")) "Editar Imagen" else "Edit Image",
                                            color = Color.White,
                                            fontFamily = theme.getFontFamily(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { showEditDialog = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = if (lang.contains("Español")) "Cerrar" else "Close", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                    }
                                }

                                // ── Preview arrastrable ───────────────────────────────
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF0A0C0B))
                                        .pointerInput(Unit) {
                                            detectDragGestures { _, dragAmount ->
                                                localOffsetX = (localOffsetX + dragAmount.x).coerceIn(-800f, 800f)
                                                localOffsetY = (localOffsetY + dragAmount.y).coerceIn(-800f, 800f)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Imagen con transformaciones en tiempo real
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(theme.globalBackgroundImage)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = if (lang.contains("Español")) "Vista previa de imagen" else "Image preview",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = localScale,
                                                scaleY = localScale,
                                                rotationZ = localRotation,
                                                translationX = localOffsetX,
                                                translationY = localOffsetY
                                            ),
                                        contentScale = ContentScale.Crop,
                                        alpha = theme.backgroundOpacity.coerceAtLeast(0.4f)
                                    )
                                    // Overlay mini-calendario (referencia visual)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("◀", color = theme.accentColor, fontSize = 10.sp)
                                                Text(
                                                    if (lang.contains("Español")) "ABRIL 2026" else "APRIL 2026",
                                                    color = Color.White,
                                                    fontFamily = theme.getFontFamily(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                                Text("▶", color = theme.accentColor, fontSize = 10.sp)
                                            }
                                            val dayHeaders = if (lang.contains("Español"))
                                                listOf("L","M","X","J","V","S","D")
                                            else
                                                listOf("M","T","W","T","F","S","S")
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                dayHeaders.forEachIndexed { i, d ->
                                                    Text(
                                                        d,
                                                        color = if (i >= 5) theme.weekendColor.copy(alpha = 0.8f) else theme.getLabelColor().copy(alpha = 0.7f),
                                                        fontSize = 9.sp,
                                                        fontFamily = theme.getFontFamily(),
                                                        modifier = Modifier.weight(1f),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                listOf(20, 21, 22, 23, 24, 25, 26).forEachIndexed { i, day ->
                                                    val isToday = day == 26
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .then(
                                                                if (isToday) Modifier.background(
                                                                    theme.accentColor,
                                                                    RoundedCornerShape(theme.getBorderRadius())
                                                                ) else Modifier
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            "$day",
                                                            color = if (isToday) Color.Black else if (i >= 5) theme.weekendColor else theme.getMainTextColor(),
                                                            fontFamily = theme.getFontFamily(),
                                                            fontSize = 9.sp,
                                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // Hint arrastrar
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 8.dp)
                                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            if (lang.contains("Español")) "↔ Arrastra para mover" else "↔ Drag to reposition",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                // ── Zoom ──────────────────────────────────────────────
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (lang.contains("Español")) "Zoom" else "Zoom",
                                            color = theme.getLabelColor(),
                                            fontSize = 12.sp,
                                            fontFamily = theme.getFontFamily()
                                        )
                                        Text(
                                            "${(localScale * 100).toInt()}%",
                                            color = theme.accentColor,
                                            fontSize = 12.sp,
                                            fontFamily = theme.getFontFamily(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(theme.accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                .clickable { localScale = (localScale - 0.1f).coerceIn(0.5f, 3f) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = if (lang.contains("Español")) "Reducir zoom" else "Zoom out", tint = theme.accentColor, modifier = Modifier.size(16.dp))
                                        }
                                        Slider(
                                            value = localScale,
                                            onValueChange = { localScale = it },
                                            valueRange = 0.5f..3f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = theme.accentColor,
                                                activeTrackColor = theme.accentColor,
                                                inactiveTrackColor = theme.accentColor.copy(alpha = 0.2f)
                                            )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(theme.accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                .clickable { localScale = (localScale + 0.1f).coerceIn(0.5f, 3f) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = if (lang.contains("Español")) "Aumentar zoom" else "Zoom in", tint = theme.accentColor, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // ── Rotación ──────────────────────────────────────────
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (lang.contains("Español")) "Rotación" else "Rotation",
                                            color = theme.getLabelColor(),
                                            fontSize = 12.sp,
                                            fontFamily = theme.getFontFamily()
                                        )
                                        Text(
                                            "${localRotation.toInt()}°",
                                            color = theme.accentColor,
                                            fontSize = 12.sp,
                                            fontFamily = theme.getFontFamily(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = localRotation,
                                        onValueChange = { localRotation = it },
                                        valueRange = -180f..180f,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = theme.accentColor,
                                            activeTrackColor = theme.accentColor,
                                            inactiveTrackColor = theme.accentColor.copy(alpha = 0.2f)
                                        )
                                    )
                                }

                                HorizontalDivider(color = theme.accentColor.copy(alpha = 0.08f))

                                // ── Posición horizontal ───────────────────────────────
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (lang.contains("Español")) "Horizontal" else "Horizontal",
                                            color = theme.getLabelColor(),
                                            fontSize = 12.sp,
                                            fontFamily = theme.getFontFamily()
                                        )
                                        Text(
                                            "${localOffsetX.toInt()}px",
                                            color = theme.accentColor,
                                            fontSize = 12.sp,
                                            fontFamily = theme.getFontFamily(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(-50, -10, 0, 10, 50).forEach { delta ->
                                            val label = if (delta == 0) "⊙" else if (delta > 0) "+$delta" else "$delta"
                                            OutlinedButton(
                                                onClick = {
                                                    localOffsetX = if (delta == 0) 0f
                                                    else (localOffsetX + delta).coerceIn(-800f, 800f)
                                                },
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                border = BorderStroke(1.dp, if (delta == 0) theme.accentColor.copy(0.5f) else theme.accentColor.copy(0.2f)),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = if (delta == 0) theme.accentColor else Color.White.copy(0.7f)
                                                )
                                            ) { Text(label, fontSize = 11.sp) }
                                        }
                                    }
                                }

                                // ── Posición vertical ─────────────────────────────────
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (lang.contains("Español")) "Vertical" else "Vertical",
                                            color = theme.getLabelColor(),
                                            fontSize = 12.sp,
                                            fontFamily = theme.getFontFamily()
                                        )
                                        Text(
                                            "${localOffsetY.toInt()}px",
                                            color = theme.accentColor,
                                            fontSize = 12.sp,
                                            fontFamily = theme.getFontFamily(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(-50, -10, 0, 10, 50).forEach { delta ->
                                            val label = if (delta == 0) "⊙" else if (delta > 0) "+$delta" else "$delta"
                                            OutlinedButton(
                                                onClick = {
                                                    localOffsetY = if (delta == 0) 0f
                                                    else (localOffsetY + delta).coerceIn(-800f, 800f)
                                                },
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                border = BorderStroke(1.dp, if (delta == 0) theme.accentColor.copy(0.5f) else theme.accentColor.copy(0.2f)),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = if (delta == 0) theme.accentColor else Color.White.copy(0.7f)
                                                )
                                            ) { Text(label, fontSize = 11.sp) }
                                        }
                                    }
                                }

                                // ── Botones de acción ─────────────────────────────────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            localRotation = 0f
                                            localOffsetX = 0f
                                            localOffsetY = 0f
                                            localScale = 1f
                                        },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.35f)),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.accentColor)
                                    ) {
                                        Icon(Icons.Default.CenterFocusWeak, contentDescription = if (lang.contains("Español")) "Reiniciar posición" else "Reset position", modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (lang.contains("Español")) "Reiniciar" else "Reset", fontSize = 13.sp)
                                    }
                                    Button(
                                        onClick = { showEditDialog = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                                    ) {
                                        Text(
                                            if (lang.contains("Español")) "Listo" else "Done",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── COMPONENTES AUXILIARES ────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, theme: AppTheme) {
    val pulse by rememberInfiniteTransition(label = "SectionPulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SectionDot"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        theme.accentColor.copy(alpha = 0.17f),
                        theme.accentColor.copy(alpha = 0.08f),
                        theme.accentColor.copy(alpha = 0.01f)
                    )
                )
            )
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Punto pulsante
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(theme.accentColor.copy(alpha = pulse), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        // Barra vertical de acento
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(theme.accentColor, theme.accentColor.copy(alpha = 0.15f))
                    ),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = theme.accentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = theme.accentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            fontFamily = theme.getFontFamily(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ThemeDivider(accentColor: Color) {
    val pulse by rememberInfiniteTransition(label = "DividerPulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DividerDot"
    )
    Spacer(Modifier.height(26.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, accentColor.copy(alpha = 0.3f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(5.dp)
                .background(accentColor.copy(alpha = pulse), CircleShape)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
    }
    Spacer(Modifier.height(26.dp))
}

@Composable
fun MonthBox(month: Int, theme: AppTheme, lang: String, imageUri: String?, onDelete: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    val names = if (lang.contains("Español"))
        listOf("ENE","FEB","MAR","ABR","MAY","JUN","JUL","AGO","SEP","OCT","NOV","DIC")
    else
        listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")

    val hasImage = imageUri != null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(if (hasImage) Color(0xFF1A1D1C) else Color(0xFF141716))
            .border(
                1.dp,
                if (hasImage) theme.accentColor.copy(alpha = 0.4f) else Color.DarkGray.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (hasImage) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUri)
                    .crossfade(true)
                    .setParameter("allow_hardware", false)
                    .build(),
                contentDescription = if (lang.contains("Español")) "Fondo de ${names[month - 1]}" else "${names[month - 1]} background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (theme.showBackgroundImages) 0.65f else 0.15f
            )
            // Gradiente inferior para el texto
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )
            // Botón eliminar
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = if (lang.contains("Español")) "Eliminar imagen del mes" else "Remove month image", tint = Color.White, modifier = Modifier.size(10.dp))
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (lang.contains("Español")) "Agregar imagen del mes" else "Add month image",
                    tint = theme.accentColor.copy(alpha = if (theme.showBackgroundImages) 0.5f else 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        // Etiqueta del mes
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    if (!hasImage) theme.accentColor.copy(alpha = 0.12f)
                    else Color.Transparent
                )
                .padding(bottom = 5.dp, top = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = names[month - 1],
                color = if (hasImage) Color.White else theme.accentColor.copy(alpha = 0.8f),
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun EffectSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    isPercentage: Boolean = true,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember(value) { mutableStateOf(value) }
    val displayValue = if (isPercentage) "${(sliderValue * 100).toInt()}%" else "%.1f".format(sliderValue)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = displayValue,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChange(sliderValue) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = accentColor.copy(alpha = 0.2f)
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvancedColorSelector(
    label: String,
    selectedColor: Color,
    isDefault: Boolean = false,
    lang: String,
    theme: AppTheme,
    hideLabel: Boolean = false,
    onReset: (() -> Unit)? = null,
    onColorChanged: (Color) -> Unit
) {
    val baseColors = listOf(
        Color(0xFF00E676), Color(0xFF4285F4), Color(0xFF9B51E0),
        Color(0xFFEB5757), Color(0xFFF2C94C), Color(0xFF00BCD4)
    )
    var activeBaseColor by remember { mutableStateOf<Color?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141716))
            .border(1.dp, theme.accentColor.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        if (!hideLabel) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    color = theme.getLabelColor(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = theme.getFontFamily(),
                    modifier = Modifier.weight(1f)
                )
                if (isDefault) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = if (lang.contains("Español")) "Predeterminado" else "Default", tint = theme.accentColor, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (lang.contains("Español")) "PRED" else "DEF",
                            color = theme.accentColor,
                            fontSize = 10.sp
                        )
                    }
                }
                // Muestra el color actual
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                )
                // Botón reset dentro de la tarjeta (solo cuando la etiqueta es visible)
                if (onReset != null && !hideLabel) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(theme.accentColor.copy(alpha = 0.10f), CircleShape)
                            .clickable { onReset() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = if (lang.contains("Español")) "Resetear color" else "Reset color",
                            tint = theme.accentColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            baseColors.forEach { color ->
                val isActive = activeBaseColor == color
                val displayColor = if (isActive) selectedColor else color
                val ringAlpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0f,
                    animationSpec = tween(200),
                    label = "SwatchRing"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(
                            if (isActive) Brush.radialGradient(listOf(displayColor.copy(alpha = 0.9f), color))
                            else Brush.radialGradient(listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.7f)))
                        )
                        .then(
                            if (ringAlpha > 0f) Modifier.border(2.5.dp, Color.White.copy(alpha = ringAlpha), CircleShape)
                            else Modifier
                        )
                        .combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeBaseColor = color
                                onColorChanged(color)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeBaseColor = color
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = if (lang.contains("Español")) "Color seleccionado" else "Selected color",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Reset al final de la tarjeta cuando la etiqueta está oculta
        if (hideLabel && onReset != null) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(theme.accentColor.copy(alpha = 0.10f), CircleShape)
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = if (lang.contains("Español")) "Resetear color" else "Reset color",
                        tint = theme.accentColor.copy(alpha = 0.75f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = activeBaseColor != null) {
            activeBaseColor?.let { base ->
                Column(Modifier.padding(top = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (lang.contains("Español")) "Ajuste fino" else "Fine tuning",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(selectedColor)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (lang.contains("Español")) "Cerrar ajuste fino" else "Close fine tuning",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { activeBaseColor = null },
                            tint = theme.getLabelColor()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(listOf(Color.Black, base, Color.White)))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .pointerInput(base) {
                                detectTapGestures { offset ->
                                    onColorChanged(calculateColor(base, offset.x / size.width))
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun DayMarkerSelector(
    selectedStyle: DayMarkerStyle,
    accentColor: Color,
    lang: String,
    theme: AppTheme,
    onSelect: (DayMarkerStyle) -> Unit
) {
    val options = listOf(
        DayMarkerStyle.CLASSIC to if (lang.contains("Español")) "Clásico" else "Classic",
        DayMarkerStyle.SQUARE to if (lang.contains("Español")) "Cuadrado" else "Square",
        DayMarkerStyle.MINIMAL to if (lang.contains("Español")) "Mínimo" else "Minimal"
    )

    Column {
        SectionLabel(
            text = if (lang.contains("Español")) "ESTILO DE MARCADOR" else "MARKER STYLE",
            icon = Icons.Default.RadioButtonChecked,
            theme = theme
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { (style, name) ->
                val isSelected = selectedStyle == style
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.12f)
                                else Color(0xFF141716)
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) accentColor else Color.DarkGray.copy(alpha = 0.4f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelect(style) },
                        contentAlignment = Alignment.Center
                    ) {
                        MarkerShape(accentColor, style)
                        Text(
                            text = "12",
                            color = theme.getMainTextColor(),
                            fontSize = (12 * theme.globalTextScale).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = theme.getFontFamily()
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = name,
                        color = if (isSelected) accentColor else theme.getLabelColor(),
                        fontSize = (10 * theme.globalTextScale).sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = theme.getFontFamily()
                    )
                }
            }
        }
    }
}

@Composable
fun TypographySelector(
    selectedStyle: Int,
    accentColor: Color,
    lang: String,
    theme: AppTheme,
    onSelect: (Int) -> Unit
) {
    val styles = listOf(
        0 to (if (lang.contains("Español")) "Mínimo"       else "Minimal"),
        1 to (if (lang.contains("Español")) "Elegante"     else "Elegant"),
        2 to (if (lang.contains("Español")) "Geométrico"   else "Geometric"),
        3 to (if (lang.contains("Español")) "Divertido"    else "Playful"),
        4 to (if (lang.contains("Español")) "Redondeado"   else "Rounded"),
        5 to (if (lang.contains("Español")) "Delgado"      else "Thin"),
        6 to (if (lang.contains("Español")) "Condensado"   else "Heavy"),
        7 to (if (lang.contains("Español")) "Monoespaciado" else "Mono")
    )

    Column {
        SectionLabel(
            text = if (lang.contains("Español")) "ESTILO DE TIPOGRAFÍA" else "TYPOGRAPHY STYLE",
            icon = Icons.Default.FontDownload,
            theme = theme
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            styles.forEachIndexed { index, (_, name) ->
                val isSelected = selectedStyle == index
                val fontFamily = when (index) {
                    0 -> MinimalFont
                    1 -> ElegantFont
                    2 -> GeometricFont
                    3 -> PlayfulFont
                    4 -> RoundedFont
                    5 -> ThinFont
                    6 -> HeavyFont
                    7 -> MonoFont
                    else -> MinimalFont
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected)
                                    Brush.linearGradient(
                                        listOf(
                                            accentColor.copy(alpha = 0.15f),
                                            accentColor.copy(alpha = 0.05f)
                                        )
                                    )
                                else
                                    Brush.linearGradient(listOf(Color(0xFF1A1D1C), Color(0xFF141716)))
                            )
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) accentColor else Color.DarkGray.copy(alpha = 0.35f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "26",
                                color = if (isSelected) accentColor else theme.getMainTextColor().copy(alpha = 0.85f),
                                fontSize = (28 * theme.globalTextScale).sp,
                                fontFamily = fontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lang.contains("Español")) "ABR" else "APR",
                                color = if (isSelected) accentColor.copy(alpha = 0.75f) else theme.getLabelColor(),
                                fontSize = (9 * theme.globalTextScale).sp,
                                fontFamily = fontFamily,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = name,
                        color = if (isSelected) accentColor else theme.getLabelColor(),
                        fontSize = (10 * theme.globalTextScale).sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = theme.getFontFamily()
                    )
                }
            }
        }
    }
}

// ── HELPERS ───────────────────────────────────────────────────────────────────

fun calculateColor(base: Color, t: Float): Color {
    return when {
        t < 0.5f -> {
            val factor = t * 2
            Color(
                red = base.red * factor,
                green = base.green * factor,
                blue = base.blue * factor
            )
        }
        else -> {
            val factor = (t - 0.5f) * 2
            Color(
                red = base.red + (1f - base.red) * factor,
                green = base.green + (1f - base.green) * factor,
                blue = base.blue + (1f - base.blue) * factor
            )
        }
    }
}
