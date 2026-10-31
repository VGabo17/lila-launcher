package com.lilac.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lilac.launcher.data.AppInfo
import com.lilac.launcher.ui.components.AppIcon
import com.lilac.launcher.ui.components.bounceClick
import com.lilac.launcher.ui.theme.Lilac

@Composable
fun Dock(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .background(Lilac.Surface, shape)
            .border(1.dp, Lilac.Border, shape)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        apps.forEach { app ->
            key(app.id) {
                AppIcon(
                    app = app,
                    modifier = Modifier
                        .size(56.dp)
                        .bounceClick(pressedScale = 0.86f) { onAppClick(app) },
                )
            }
        }
    }
}
