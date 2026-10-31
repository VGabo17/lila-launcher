package com.lilac.launcher.data

import android.content.Context

/** Cuenta cuántas veces se abre cada app. SharedPreferences: sin dependencias y casi sin memoria. */
class UsageStore(context: Context) {

    private val prefs = context.getSharedPreferences("usage", Context.MODE_PRIVATE)

    fun increment(appId: String) {
        prefs.edit().putInt(appId, prefs.getInt(appId, 0) + 1).apply()
    }

    /** ids de las [n] apps más usadas, de mayor a menor. */
    fun top(n: Int): List<String> =
        prefs.all.entries
            .mapNotNull { entry -> (entry.value as? Int)?.let { entry.key to it } }
            .sortedByDescending { it.second }
            .take(n)
            .map { it.first }
}
