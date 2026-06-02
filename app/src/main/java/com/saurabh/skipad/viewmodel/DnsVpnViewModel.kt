package com.saurabh.skipad.viewmodel

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.skipad.model.InstalledAppGeneral
import com.saurabh.skipad.service.DnsVpnService
import com.saurabh.skipad.util.ToolBox
import com.saurabh.skipad.data.PreferenceManager
import com.saurabh.skipad.data.db.AnalyticsDao
import com.saurabh.skipad.data.db.AppUsageStats
import com.saurabh.skipad.data.db.UsageSession
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class VpnUiState(
    val isVpnActive: Boolean = false,
    val activePackage: String? = null,
    val selectedApps: List<InstalledAppGeneral> = emptyList(),
    val allApps: List<InstalledAppGeneral> = emptyList(),
    val isLoadingApps: Boolean = false,
    val usageStats: List<AppUsageStats> = emptyList()
)

@HiltViewModel
class DnsVpnViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val toolBox: ToolBox,
    private val preferenceManager: PreferenceManager,
    private val analyticsDao: AnalyticsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    private var pendingPackage: String = ""
    private var vpnStartTime: Long = 0
    private var activeAppName: String = ""

    init {
        loadApps()
        observeServiceState()
        observePersistedApps()
        observeAnalytics()
    }

    private fun observeAnalytics() {
        analyticsDao.getAggregateUsage()
            .onEach { stats ->
                _uiState.update { it.copy(usageStats = stats) }
            }
            .launchIn(viewModelScope)
    }

    private fun observePersistedApps() {
        preferenceManager.selectedAppsFlow
            .onEach { persistedPackageNames ->
                _uiState.update { state ->
                    val updatedAllApps = state.allApps.map { app ->
                        app.copy(isSelected = persistedPackageNames.contains(app.packageName))
                    }
                    val selectedApps = updatedAllApps.filter { it.isSelected }
                    state.copy(allApps = updatedAllApps, selectedApps = selectedApps)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeServiceState() {
        DnsVpnService.isRunning
            .onEach { isRunning ->
                if (!isRunning && _uiState.value.isVpnActive) {
                    saveSession()
                    _uiState.update { it.copy(isVpnActive = false, activePackage = null) }
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun saveSession() {
        val packageName = _uiState.value.activePackage ?: return
        val duration = System.currentTimeMillis() - vpnStartTime
        if (duration > 1000) { // Sirf 1 second se zyada sessions save karo
            analyticsDao.insertSession(
                UsageSession(
                    packageName = packageName,
                    appName = activeAppName,
                    startTime = vpnStartTime,
                    durationMs = duration
                )
            )
        }
    }

    fun loadApps() {
        _uiState.update { it.copy(isLoadingApps = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val apps = toolBox.getInstalledApps()
            val persistedPackageNames = preferenceManager.selectedAppsFlow.first()
            val updatedApps = apps.map { app ->
                app.copy(isSelected = persistedPackageNames.contains(app.packageName))
            }
            _uiState.update { 
                it.copy(
                    allApps = updatedApps, 
                    isLoadingApps = false,
                    selectedApps = updatedApps.filter { it.isSelected }
                ) 
            }
        }
    }

    fun toggleAppSelection(app: InstalledAppGeneral, selected: Boolean) {
        val updated = _uiState.value.allApps.map {
            if (it.packageName == app.packageName) it.copy(isSelected = selected) else it
        }
        _uiState.update { it.copy(allApps = updated) }
    }

    fun selectAllApps() {
        val updated = _uiState.value.allApps.map { it.copy(isSelected = true) }
        _uiState.update { it.copy(allApps = updated) }
    }

    fun unselectAllApps() {
        val updated = _uiState.value.allApps.map { it.copy(isSelected = false) }
        _uiState.update { it.copy(allApps = updated) }
    }

    fun confirmSelection() {
        val selected = _uiState.value.allApps.filter { it.isSelected }
        _uiState.update { it.copy(selectedApps = selected) }
        viewModelScope.launch {
            preferenceManager.saveSelectedApps(selected.map { it.packageName }.toSet())
        }
    }

    fun requestVpnFor(app: InstalledAppGeneral): Intent? {
        val prepareIntent = VpnService.prepare(context)
        return if (prepareIntent != null) {
            pendingPackage = app.packageName
            activeAppName = app.appName
            prepareIntent
        } else {
            startVpn(app.packageName, app.appName)
            null
        }
    }

    fun onVpnPermissionGranted() {
        if (pendingPackage.isNotBlank()) {
            startVpn(pendingPackage, activeAppName)
            pendingPackage = ""
        }
    }

    fun startVpn(packageName: String, appName: String) {
        vpnStartTime = System.currentTimeMillis()
        activeAppName = appName
        val vpnIntent = Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_START
            putExtra(DnsVpnService.EXTRA_TARGET_APP, packageName)
        }
        context.startService(vpnIntent)
        toolBox.openApp(packageName)
        _uiState.update { it.copy(isVpnActive = true, activePackage = packageName) }
    }

    fun stopVpn() {
        val intent = Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_STOP
        }
        context.startService(intent)
    }
}