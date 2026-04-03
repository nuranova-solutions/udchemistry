package com.udchemistry.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.udchemistry.mobile.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "udchemistry_theme")

class ThemeStore(private val context: Context) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        prefs[Keys.themeMode]
            ?.let { storedValue -> ThemeMode.entries.firstOrNull { it.name == storedValue } }
            ?: ThemeMode.Dark
    }

    suspend fun save(themeMode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[Keys.themeMode] = themeMode.name
        }
    }
}
