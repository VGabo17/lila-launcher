package com.lilac.launcher.data

import android.content.ComponentName
import androidx.compose.runtime.Immutable

/** Datos livianos de una app. El icono NO vive aquí: se carga bajo demanda (ver IconCache). */
@Immutable
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    /** Letra inicial para la agrupación alfabética ("#" si no es una letra). */
    val initial: String,
) {
    val id: String = "$packageName/$activityName"
    val componentName: ComponentName get() = ComponentName(packageName, activityName)
}
