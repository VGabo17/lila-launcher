package com.lilac.launcher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import java.text.Collator
import java.text.Normalizer

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    // PRIMARY: ignora tildes y mayúsculas al ordenar (Álvaro junto a Alvaro)
    private val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

    /** Lista de apps lanzables, ordenada alfabéticamente. Llamar fuera del hilo principal. */
    fun loadApps(): List<AppInfo> {
        val user = Process.myUserHandle()
        return launcherApps.getActivityList(null, user)
            .asSequence()
            .filter { it.componentName.packageName != context.packageName }
            .map { info ->
                val pkg = info.componentName.packageName
                val label = info.label?.toString().orEmpty().ifBlank { pkg }
                AppInfo(
                    packageName = pkg,
                    activityName = info.componentName.className,
                    label = label,
                    initial = initialOf(label),
                )
            }
            .sortedWith(Comparator { a, b -> collator.compare(a.label, b.label) })
            .toList()
    }

    private fun initialOf(label: String): String {
        val first = label.trimStart().firstOrNull() ?: return "#"
        val base = Normalizer.normalize(first.toString(), Normalizer.Form.NFD).firstOrNull() ?: first
        return if (base.isLetter()) base.uppercaseChar().toString() else "#"
    }
}
