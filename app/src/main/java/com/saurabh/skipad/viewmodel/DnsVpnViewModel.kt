package com.saurabh.skipad.viewmodel

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.skipad.model.InstalledAppGeneral
import com.saurabh.skipad.service.DnsVpnService
import com.saurabh.skipad.util.ToolBox
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class VpnUiState(
    val isVpnActive: Boolean = false,
    val activePackage: String? = null,
    val selectedApps: List<InstalledAppGeneral> = emptyList(),
    val allApps: List<InstalledAppGeneral> = emptyList(),
    val isLoadingApps: Boolean = false,
)

@HiltViewModel
class DnsVpnViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val toolBox: ToolBox
) : ViewModel() {

    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    private var pendingPackage: String = ""

    init {
        loadApps()
        observeServiceState()
    }

    // ── Service ka isRunning observe karo ──
    // Notification se stop ho ya app se — dono cases handle
    private fun observeServiceState() {
        DnsVpnService.isRunning
            .onEach { isRunning ->
                if (!isRunning) {
                    // Service band hui (chahe kahin se bhi) — UI reset karo
                    _uiState.update { it.copy(isVpnActive = false, activePackage = null) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadApps() {
        _uiState.update { it.copy(isLoadingApps = true) }
        val apps = toolBox.getInstalledApps()
        _uiState.update { it.copy(allApps = apps, isLoadingApps = false) }
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

    fun confirmSelection() {
        val selected = _uiState.value.allApps.filter { it.isSelected }
        _uiState.update { it.copy(selectedApps = selected) }
    }

    fun requestVpnFor(packageName: String): Intent? {
        val prepareIntent = VpnService.prepare(context)
        return if (prepareIntent != null) {
            pendingPackage = packageName
            prepareIntent
        } else {
            startVpn(packageName)
            null
        }
    }

    fun onVpnPermissionGranted() {
        if (pendingPackage.isNotBlank()) {
            startVpn(pendingPackage)
            pendingPackage = ""
        }
    }

    fun startVpn(packageName: String) {
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
        // isRunning flow automatically UI update karega via observeServiceState()
    }
}