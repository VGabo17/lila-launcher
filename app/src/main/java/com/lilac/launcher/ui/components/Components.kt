package com.lilac.launcher.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.lilac.launcher.data.AppInfo
import com.lilac.launcher.data.IconCache

/** Click con rebote de resorte rápido. Sin ripple: más limpio y más barato. */
fun Modifier.bounceClick(pressedScale: Float = 0.9f, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "bounce",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** Icono de app cargado en segundo plano y cacheado. */
@Composable
fun AppIcon(app: AppInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<ImageBitmap?>(initialValue = IconCache.peek(app.id), app.id) {
        if (value == null) value = IconCache.load(context, app)
    }
    val icon = bitmap
    if (icon != null) {
        Image(bitmap = icon, contentDescription = app.label, modifier = modifier)
    } else {
        Spacer(modifier)
    }
}
