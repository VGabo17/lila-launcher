package com.lilac.launcher.ui.drawer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.lilac.launcher.data.AppInfo
import com.lilac.launcher.ui.components.AppIcon
import com.lilac.launcher.ui.components.bounceClick
import com.lilac.launcher.ui.theme.Lilac
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Resultados: los que empiezan con el texto primero, luego los que lo contienen
    val results = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            apps
        } else {
            apps.filter { it.label.contains(q, ignoreCase = true) }
                .sortedBy { if (it.label.startsWith(q, ignoreCase = true)) 0 else 1 }
        }
    }
    val grouped = remember(results, query) {
        if (query.isBlank()) results.groupBy { it.initial } else emptyMap()
    }

    // Al hacer scroll se oculta el teclado
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { focusManager.clearFocus() }
    }

    Column(
        modifier = modifier
            .background(Lilac.Background)
            // Bloquea los toques hacia el Home que queda debajo
            .pointerInput(Unit) { detectTapGestures { } }
            .nestedScroll(nestedScrollConnection)
            .systemBarsPadding()
            .imePadding(),
    ) {
        // Tirador
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Lilac.Border),
            )
        }

        SearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = { results.firstOrNull()?.let(onAppClick) },
        )

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (apps.isEmpty()) "Cargando…" else "Sin resultados",
                    color = Lilac.TextDisabled,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (query.isBlank()) {
                    grouped.forEach { (letter, items) ->
                        stickyHeader(key = "header_$letter") { LetterHeader(letter) }
                        items(items, key = { it.id }) { app -> AppRow(app, onAppClick) }
                    }
                } else {
                    items(results, key = { it.id }) { app -> AppRow(app, onAppClick) }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) Lilac.Neon else Lilac.Border,
        animationSpec = tween(150),
        label = "searchBorder",
    )
    val shape = RoundedCornerShape(20.dp)

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Lilac.TextPrimary),
        cursorBrush = SolidColor(Lilac.Neon),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Lilac.Surface)
                    .border(1.dp, borderColor, shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchIcon(tint = if (focused) Lilac.Neon else Lilac.TextSecondary)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar apps",
                            color = Lilac.TextDisabled,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Text(
                        text = "×",
                        color = Lilac.TextSecondary,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .bounceClick(pressedScale = 0.8f) { onQueryChange("") }
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchIcon(tint: Color) {
    Canvas(Modifier.size(18.dp)) {
        val stroke = 2.dp.toPx()
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.42f, size.height * 0.42f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.66f, size.height * 0.66f),
            end = Offset(size.width * 0.95f, size.height * 0.95f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Text(
        text = letter,
        color = Lilac.Neon,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(Lilac.Background)
            .padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun AppRow(app: AppInfo, onClick: (AppInfo) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(pressedScale = 0.97f) { onClick(app) }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app = app, modifier = Modifier.size(44.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = Lilac.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
