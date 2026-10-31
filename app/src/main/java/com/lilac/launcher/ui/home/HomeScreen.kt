package com.lilac.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.lilac.launcher.data.AppInfo
import com.lilac.launcher.ui.theme.Lilac

@Composable
fun HomeScreen(
    dock: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // Resplandor lila estático: se dibuja una vez, costo casi nulo
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Lilac.DeepPurple.copy(alpha = 0.35f),
                            Lilac.Background.copy(alpha = 0f),
                        ),
                        center = Offset(size.width / 2f, size.height * 0.18f),
                        radius = size.width * 0.9f,
                    ),
                )
            }
            .systemBarsPadding(),
    ) {
        ClockBlock(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pista visual: desliza hacia arriba para abrir el cajón
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Lilac.Neon.copy(alpha = 0.45f)),
            )
            Spacer(Modifier.height(14.dp))
            Dock(apps = dock, onAppClick = onAppClick)
        }
    }
}
