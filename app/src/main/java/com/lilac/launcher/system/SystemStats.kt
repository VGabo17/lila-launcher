package com.lilac.launcher.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Immutable

@Immutable
data class SystemSnapshot(
    val ramUsedMb: Int,
    val ramTotalMb: Int,
    val batteryPercent: Int,
    val charging: Boolean,
    /** Temperatura de la batería en °C (es la que Android expone a las apps). */
    val tempCelsius: Float,
) {
    val ramFraction: Float
        get() = if (ramTotalMb == 0) 0f else ramUsedMb / ramTotalMb.toFloat()
}

object SystemStats {
    private const val MB = 1024L * 1024L

    /** Lectura puntual y barata. Solo se llama mientras el panel Mods está visible. */
    fun read(context: Context): SystemSnapshot {
        val am = context.getSystemService(ActivityManager::class.java)
        val mem = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mem)
        val total = (mem.totalMem / MB).toInt()
        val used = ((mem.totalMem - mem.availMem) / MB).toInt()

        // Intent "sticky": no registra receptor, devuelve el último estado conocido
        val battery: Intent? =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val temp = (battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        return SystemSnapshot(used, total, percent, charging, temp)
    }
}
