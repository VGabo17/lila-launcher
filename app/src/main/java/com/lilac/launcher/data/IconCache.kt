package com.lilac.launcher.data

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Iconos bajo demanda con tope de memoria (~120 iconos de 160px ≈ 12 MB como máximo).
 * Evita cargar cientos de Drawables al arrancar.
 */
object IconCache {
    private const val ICON_SIZE_PX = 160
    private val cache = LruCache<String, ImageBitmap>(120)

    fun peek(appId: String): ImageBitmap? = cache.get(appId)

    suspend fun load(context: Context, app: AppInfo): ImageBitmap? {
        cache.get(app.id)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                context.packageManager
                    .getActivityIcon(app.componentName)
                    .toBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
                    .asImageBitmap()
                    .also { cache.put(app.id, it) }
            } catch (e: Exception) {
                null
            }
        }
    }
}
