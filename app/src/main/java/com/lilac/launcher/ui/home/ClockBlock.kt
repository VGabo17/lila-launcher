package com.lilac.launcher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lilac.launcher.ui.theme.Lilac
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Hora actual (ms). Se actualiza con el broadcast del sistema TIME_TICK (1 vez por minuto):
 * cero polling, cero timers propios.
 */
@Composable
private fun rememberNowMillis(): State<Long> {
    val context = LocalContext.current
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                now.longValue = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        now.longValue = System.currentTimeMillis()
        onDispose { context.unregisterReceiver(receiver) }
    }
    return now
}

@Composable
fun ClockBlock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val nowMillis by rememberNowMillis()

    val locale = Locale.getDefault()
    val is24h = DateFormat.is24HourFormat(context)
    val zoneId = TimeZone.getDefault().id

    val timeFormat = remember(is24h, locale, zoneId) {
        SimpleDateFormat(if (is24h) "HH:mm" else "h:mm", locale)
    }
    val dateFormat = remember(locale, zoneId) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale)
    }
    val date = Date(nowMillis)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timeFormat.format(date),
            style = MaterialTheme.typography.displayLarge.copy(
                shadow = Shadow(
                    color = Lilac.Violet.copy(alpha = 0.55f),
                    blurRadius = 42f,
                ),
            ),
            color = Lilac.TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = dateFormat.format(date).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.titleMedium,
            color = Lilac.Soft,
        )
    }
}
