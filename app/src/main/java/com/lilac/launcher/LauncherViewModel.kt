package com.lilac.launcher

import android.app.Application
import android.content.pm.LauncherApps
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lilac.launcher.data.AppInfo
import com.lilac.launcher.data.AppRepository
import com.lilac.launcher.data.UsageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val usage = UsageStore(application)
    private val launcherApps = application.getSystemService(LauncherApps::class.java)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val usageVersion = MutableStateFlow(0)

    /** Las 5 apps del dock: más usadas, y si faltan, apps comunes o las primeras de la lista. */
    val dock: StateFlow<List<AppInfo>> = combine(_apps, usageVersion) { list, _ -> buildDock(list) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Se entera de instalaciones / desinstalaciones sin hacer polling
    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(
            packageNames: Array<out String>, user: UserHandle, replacing: Boolean,
        ) = refresh()
        override fun onPackagesUnavailable(
            packageNames: Array<out String>, user: UserHandle, replacing: Boolean,
        ) = refresh()
    }

    init {
        refresh()
        launcherApps.registerCallback(packageCallback)
    }

    fun onLaunched(app: AppInfo) {
        usage.increment(app.id)
        usageVersion.update { it + 1 }
    }

    private fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _apps.value = repository.loadApps()
        }
    }

    private fun buildDock(apps: List<AppInfo>): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        val byId = apps.associateBy { it.id }
        val chosen = LinkedHashSet<String>()

        usage.top(DOCK_SIZE).forEach { id -> if (id in byId) chosen += id }

        for (pkg in DEFAULT_DOCK_PACKAGES) {
            if (chosen.size >= DOCK_SIZE) break
            apps.firstOrNull { it.packageName == pkg }?.let { chosen += it.id }
        }
        for (app in apps) {
            if (chosen.size >= DOCK_SIZE) break
            chosen += app.id
        }
        return chosen.mapNotNull { byId[it] }
    }

    override fun onCleared() {
        launcherApps.unregisterCallback(packageCallback)
        super.onCleared()
    }

    private companion object {
        const val DOCK_SIZE = 5
        val DEFAULT_DOCK_PACKAGES = listOf(
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.android.chrome",
            "com.google.android.GoogleCamera",
            "com.android.camera",
            "com.google.android.youtube",
            "com.whatsapp",
            "com.android.settings",
        )
    }
}
