package com.lilac.launcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lilac.launcher.data.AppInfo
import com.lilac.launcher.ui.LauncherRoot
import com.lilac.launcher.ui.theme.LilacTheme

class MainActivity : ComponentActivity() {

    // Se incrementa cada vez que se pulsa el botón Home estando ya en el launcher
    private val homeSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            LilacTheme {
                val vm: LauncherViewModel = viewModel()
                LauncherRoot(
                    vm = vm,
                    homeSignal = homeSignal.intValue,
                    onLaunch = ::startApp,
                    onStartIntent = ::startSafely,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeSignal.intValue++
        }
    }

    private fun startApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.componentName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        startSafely(intent)
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            // La app pudo desinstalarse justo antes de abrirla
        } catch (e: SecurityException) {
            // Ajuste no accesible en este dispositivo
        }
    }
}
