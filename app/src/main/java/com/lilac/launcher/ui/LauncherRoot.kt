package com.lilac.launcher.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lilac.launcher.LauncherViewModel
import com.lilac.launcher.data.AppInfo
import com.lilac.launcher.ui.drawer.AppDrawer
import com.lilac.launcher.ui.home.HomeScreen
import com.lilac.launcher.ui.mods.ModsPanel
import com.lilac.launcher.ui.theme.Lilac
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Raíz del launcher. Tres "capas":
 *  - Home (centro)
 *  - Panel Mods: desliza hacia la DERECHA
 *  - Cajón de apps: desliza hacia ARRIBA
 *
 * Toda la animación es un único Float por panel (0..1) leído solo en graphicsLayer,
 * así que mientras arrastras NO se recompone nada: solo se redibuja la capa.
 */
@Composable
fun LauncherRoot(
    vm: LauncherViewModel,
    homeSignal: Int,
    onLaunch: (AppInfo) -> Unit,
    onStartIntent: (Intent) -> Unit,
) {
    val dock by vm.dock.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val drawerP = remember { Animatable(0f) }
    val modsP = remember { Animatable(0f) }
    val drawerVisible by remember { derivedStateOf { drawerP.value > 0f } }
    val modsVisible by remember { derivedStateOf { modsP.value > 0f } }

    // Resorte rápido y sin rebote: se siente instantáneo pero suave
    val settleSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f)
    }

    val launch: (AppInfo) -> Unit = { app ->
        vm.onLaunched(app)
        onLaunch(app)
    }

    // Botón Home pulsado: volver al centro
    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            launch { drawerP.animateTo(0f, settleSpec) }
            modsP.animateTo(0f, settleSpec)
        }
    }

    // Un launcher nunca se cierra con "atrás"; si hay un panel abierto, lo cierra
    BackHandler(enabled = true) { }
    BackHandler(enabled = drawerVisible) { scope.launch { drawerP.animateTo(0f, settleSpec) } }
    BackHandler(enabled = modsVisible) { scope.launch { modsP.animateTo(0f, settleSpec) } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Lilac.Background),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val panelWidth = maxWidth * 0.86f
        val panelWidthPx = widthPx * 0.86f

        fun settleDrawer(open: Boolean, velocityPx: Float = 0f) {
            scope.launch {
                drawerP.animateTo(
                    targetValue = if (open) 1f else 0f,
                    animationSpec = settleSpec,
                    initialVelocity = -velocityPx / heightPx,
                )
            }
        }

        fun settleMods(open: Boolean, velocityPx: Float = 0f) {
            scope.launch {
                modsP.animateTo(
                    targetValue = if (open) 1f else 0f,
                    animationSpec = settleSpec,
                    initialVelocity = velocityPx / panelWidthPx,
                )
            }
        }

        // Arrastre horizontal (abre/cierra Mods)
        val modsDrag = rememberDraggableState { delta ->
            if (!drawerVisible) {
                scope.launch {
                    modsP.snapTo((modsP.value + delta / panelWidthPx).coerceIn(0f, 1f))
                }
            }
        }

        // Arrastre vertical sobre el Home (abre el cajón)
        val drawerDrag = rememberDraggableState { delta ->
            if (!modsVisible) {
                scope.launch {
                    drawerP.snapTo((drawerP.value - delta / heightPx).coerceIn(0f, 1f))
                }
            }
        }

        // Dentro del cajón: al estar la lista arriba del todo, arrastrar hacia abajo lo cierra
        val drawerNested = remember(heightPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (source == NestedScrollSource.UserInput &&
                        available.y < 0f && drawerP.value < 1f
                    ) {
                        scope.launch {
                            drawerP.snapTo((drawerP.value - available.y / heightPx).coerceIn(0f, 1f))
                        }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source == NestedScrollSource.UserInput && available.y > 0f) {
                        scope.launch {
                            drawerP.snapTo((drawerP.value - available.y / heightPx).coerceIn(0f, 1f))
                        }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    val v = available.y
                    if (drawerP.value < 0.999f || v > 2500f) {
                        val open = if (v > 900f) false else drawerP.value > 0.5f
                        settleDrawer(open, v)
                    }
                    return available
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = modsDrag,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { v ->
                        if (!drawerVisible) {
                            val open = if (abs(v) > 700f) v > 0f else modsP.value > 0.5f
                            settleMods(open, v)
                        }
                    },
                ),
        ) {
            // ---------- Capa 1: Home ----------
            HomeScreen(
                dock = dock,
                onAppClick = launch,
                modifier = Modifier
                    .graphicsLayer {
                        val p = maxOf(drawerP.value, modsP.value)
                        val s = 1f - 0.05f * p
                        alpha = 1f - 0.7f * p
                        scaleX = s
                        scaleY = s
                    }
                    .draggable(
                        state = drawerDrag,
                        orientation = Orientation.Vertical,
                        onDragStopped = { v ->
                            if (!modsVisible) {
                                val open = if (abs(v) > 900f) v < 0f else drawerP.value > 0.5f
                                settleDrawer(open, v)
                            }
                        },
                    ),
            )

            // ---------- Capa 2: Panel Mods ----------
            if (modsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = modsP.value }
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { settleMods(false) },
                )
                ModsPanel(
                    onStartIntent = onStartIntent,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(panelWidth)
                        .graphicsLayer { translationX = -(1f - modsP.value) * panelWidthPx },
                )
            }

            // ---------- Capa 3: Cajón de apps ----------
            if (drawerVisible) {
                AppDrawer(
                    apps = apps,
                    onAppClick = { app ->
                        launch(app)
                        settleDrawer(false)
                    },
                    nestedScrollConnection = drawerNested,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationY = (1f - drawerP.value) * heightPx },
                )
            }
        }
    }
}
