package com.saurabh.focusapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Serializable
data class FocusProfile(
    val name: String,
    val packageNames: Set<String>
)

@Singleton
class PreferenceManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val SELECTED_APPS_KEY = stringSetPreferencesKey("selected_apps")
    private val PROFILES_KEY = stringPreferencesKey("focus_profiles")
    private val ACTIVE_PROFILE_KEY = stringPreferencesKey("active_profile_name")

    val selectedAppsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[SELECTED_APPS_KEY] ?: emptySet()
        }

    val profilesFlow: Flow<List<FocusProfile>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[PROFILES_KEY] ?: "[]"
            try {
                Json.decodeFromString<List<FocusProfile>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }

    val activeProfileNameFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[ACTIVE_PROFILE_KEY] }

    suspend fun saveSelectedApps(packageNames: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_APPS_KEY] = packageNames
        }
    }

    suspend fun saveProfiles(profiles: List<FocusProfile>) {
        context.dataStore.edit { preferences ->
            preferences[PROFILES_KEY] = Json.encodeToString(profiles)
        }
    }

    suspend fun setActiveProfile(name: String?) {
        context.dataStore.edit { preferences ->
            if (name == null) {
                preferences.remove(ACTIVE_PROFILE_KEY)
            } else {
                preferences[ACTIVE_PROFILE_KEY] = name
            }
        }
    }
}