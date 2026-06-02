package com.saurabh.skipad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.skipad.data.FocusProfile
import com.saurabh.skipad.data.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profiles: List<String> = emptyList(),
    val activeProfileName: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferenceManager.profilesFlow,
                preferenceManager.activeProfileNameFlow
            ) { profiles, active ->
                ProfileUiState(
                    profiles = profiles.map { it.name },
                    activeProfileName = active
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun addProfile(name: String) {
        viewModelScope.launch {
            val current = preferenceManager.profilesFlow.first().toMutableList()
            if (current.none { it.name == name }) {
                current.add(FocusProfile(name = name, packageNames = emptySet()))
                preferenceManager.saveProfiles(current)
            }
        }
    }

    fun setActive(name: String) {
        viewModelScope.launch {
            val current = preferenceManager.activeProfileNameFlow.first()
            // Toggle off if already active
            preferenceManager.setActiveProfile(if (current == name) null else name)
        }
    }

    fun deleteProfile(name: String) {
        viewModelScope.launch {
            val current = preferenceManager.profilesFlow.first()
                .filter { it.name != name }
            preferenceManager.saveProfiles(current)
            // Clear active if deleted
            if (preferenceManager.activeProfileNameFlow.first() == name) {
                preferenceManager.setActiveProfile(null)
            }
        }
    }
}