package com.lilac.launcher.ui.mods

import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lilac.launcher.system.QuickToggles
import com.lilac.launcher.system.SystemStats
import com.lilac.launcher.ui.components.bounceClick
import com.lilac.launcher.ui.theme.Lilac
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Panel de "Mods" estilo Lunar Client. Solo existe en composición mientras está visible,
 * así que el muestreo de RAM / batería / temperatura se detiene al cerrarlo
 * (y también cuando la app pasa a segundo plano).
 */
@Composable
fun ModsPanel(
    onStartIntent: (Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current

    var stats by remember { mutableStateOf(SystemStats.read(context)) }
    var toggles by remember { mutableStateOf(QuickToggles.read(context)) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val (s, t) = withContext(Dispatchers.Default) {
                    SystemStats.read(context) to QuickToggles.read(context)
                }
                stats = s
                toggles = t
                delay(2_000)
            }
        }
    }

    // ---- Linterna (esta sí se puede controlar directamente, sin permisos) ----
    val torchId: String? = remember {
        val cm = context.getSystemService(CameraManager::class.java) ?: return@remember null
        runCatching {
            cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }
    var torchOn by remember { mutableStateOf(false) }
    DisposableEffect(torchId) {
        val cm = context.getSystemService(CameraManager::class.java)
        if (torchId == null || cm == null) return@DisposableEffect onDispose { }
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (cameraId == torchId) torchOn = enabled
            }
        }
        cm.registerTorchCallback(callback, null)
        onDispose { cm.unregisterTorchCallback(callback) }
    }

    Column(
        modifier = modifier
            .background(Lilac.Surface)
            .drawBehind {
                // Línea de borde a la derecha del panel
                drawLine(
                    color = Lilac.Border,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            // Bloquea los toques hacia el Home que queda debajo
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header()

        Spacer(Modifier.height(8.dp))
        SectionLabel("RENDIMIENTO")

        val ramText = "${gb(stats.ramUsedMb)} / ${gb(stats.ramTotalMb)} GB"
        StatCard(
            label = "RAM",
            value = ramText,
            fraction = stats.ramFraction,
            warn = stats.ramFraction > 0.9f,
        )
        StatCard(
            label = "BATERÍA",
            value = "${stats.batteryPercent}%" + if (stats.charging) "  ·  cargando" else "",
            fraction = stats.batteryPercent / 100f,
            warn = stats.batteryPercent <= 15 && !stats.charging,
        )
        StatCard(
            label = "TEMPERATURA",
            value = String.format(Locale.US, "%.1f °C", stats.tempCelsius),
            fraction = (stats.tempCelsius - 20f) / 40f,
            warn = stats.tempCelsius >= 42f,
        )

        Spacer(Modifier.height(8.dp))
        SectionLabel("ACCESOS RÁPIDOS")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleTile(
                label = "Wi-Fi",
                active = toggles.wifi,
                modifier = Modifier.weight(1f),
            ) { onStartIntent(QuickToggles.wifiIntent()) }
            ToggleTile(
                label = "Bluetooth",
                active = toggles.bluetooth,
                modifier = Modifier.weight(1f),
            ) { onStartIntent(QuickToggles.bluetoothIntent()) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleTile(
                label = "No molestar",
                active = toggles.dnd,
                modifier = Modifier.weight(1f),
            ) {
                val intent = QuickToggles.toggleDnd(context, enable = !toggles.dnd)
                if (intent != null) {
                    onStartIntent(intent)
                } else {
                    toggles = toggles.copy(dnd = !toggles.dnd)
                }
            }
            if (torchId != null) {
                ToggleTile(
                    label = "Linterna",
                    active = torchOn,
                    modifier = Modifier.weight(1f),
                ) {
                    val cm = context.getSystemService(CameraManager::class.java)
                    runCatching { cm?.setTorchMode(torchId, !torchOn) }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(4.dp))
        ActionButton("Abrir ajustes del sistema") { onStartIntent(QuickToggles.settingsIntent()) }

        Text(
            text = "Android no permite que un launcher encienda Wi-Fi o Bluetooth directamente: " +
                "esos botones muestran el estado real y abren el panel del sistema.",
            style = MaterialTheme.typography.labelMedium,
            color = Lilac.TextDisabled,
        )
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "MODS",
                color = Lilac.Neon,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp,
            )
            Text(
                text = "Utilidades del sistema",
                style = MaterialTheme.typography.labelMedium,
                color = Lilac.TextSecondary,
            )
        }
        val shape = RoundedCornerShape(50)
        Row(
            modifier = Modifier
                .border(1.dp, Lilac.Border, shape)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Lilac.Neon))
            Spacer(Modifier.width(6.dp))
            Text("LIVE", style = MaterialTheme.typography.labelSmall, color = Lilac.Soft)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Lilac.TextDisabled,
        letterSpacing = 2.sp,
    )
}

@Composable
private fun StatCard(label: String, value: String, fraction: Float, warn: Boolean) {
    val progress by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(450),
        label = "statProgress",
    )
    val barColors = if (warn) listOf(Lilac.Warn, Lilac.Warn) else listOf(Lilac.Violet, Lilac.Neon)
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Lilac.SurfaceElevated)
            .border(1.dp, Lilac.Border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Lilac.TextSecondary)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = if (warn) Lilac.Warn else Lilac.TextPrimary,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Lilac.Border)
                .drawBehind {
                    // `progress` se lee solo en la fase de dibujo: no recompone
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = barColors,
                            startX = 0f,
                            endX = size.width,
                        ),
                        size = Size(size.width * progress, size.height),
                    )
                },
        )
    }
}

@Composable
private fun ToggleTile(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val bg by animateColorAsState(
        targetValue = if (active) Lilac.Violet.copy(alpha = 0.28f) else Lilac.SurfaceElevated,
        animationSpec = tween(160),
        label = "tileBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) Lilac.Neon else Lilac.Border,
        animationSpec = tween(160),
        label = "tileBorder",
    )

    Column(
        modifier = modifier
            .bounceClick(pressedScale = 0.95f, onClick = onClick)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) Lilac.Neon else Lilac.TextDisabled),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Lilac.TextPrimary,
        )
        Text(
            text = if (active) "ACTIVADO" else "APAGADO",
            style = MaterialTheme.typography.labelSmall,
            color = if (active) Lilac.Soft else Lilac.TextDisabled,
        )
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.97f, onClick = onClick)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Lilac.DeepPurple, Lilac.Violet)))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Lilac.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun gb(mb: Int): String = String.format(Locale.US, "%.1f", mb / 1024f)
