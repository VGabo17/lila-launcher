package com.lilac.launcher.system

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

data class ToggleStates(
    val wifi: Boolean = false,
    val bluetooth: Boolean = false,
    val dnd: Boolean = false,
)

/**
 * Android moderno (10+) NO permite que una app normal encienda/apague Wi-Fi o Bluetooth.
 * Por eso esos dos botones muestran el estado real y abren el panel/ajuste del sistema.
 * "No molestar" sí se puede cambiar directo, una vez concedido el permiso de acceso.
 */
object QuickToggles {

    fun read(context: Context): ToggleStates {
        val wifi = try {
            context.getSystemService(WifiManager::class.java)?.isWifiEnabled ?: false
        } catch (e: Exception) {
            false
        }
        val bluetooth = try {
            context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled ?: false
        } catch (e: Exception) {
            false
        }
        val dnd = try {
            when (context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter) {
                NotificationManager.INTERRUPTION_FILTER_NONE,
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                NotificationManager.INTERRUPTION_FILTER_ALARMS -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
        return ToggleStates(wifi, bluetooth, dnd)
    }

    fun wifiIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }

    fun bluetoothIntent(): Intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)

    fun settingsIntent(): Intent = Intent(Settings.ACTION_SETTINGS)

    /**
     * Cambia "No molestar" si ya hay permiso y devuelve null.
     * Si falta el permiso, devuelve el Intent que abre la pantalla para concederlo.
     */
    fun toggleDnd(context: Context, enable: Boolean): Intent? {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return null
        return if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(
                if (enable) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                else NotificationManager.INTERRUPTION_FILTER_ALL,
            )
            null
        } else {
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }
    }
}
