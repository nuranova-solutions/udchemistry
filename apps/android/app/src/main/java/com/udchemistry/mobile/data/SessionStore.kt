package com.udchemistry.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.udchemistry.mobile.model.StoredSession
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "udchemistry_session")

class SessionStore(private val context: Context) {
    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val userId = stringPreferencesKey("user_id")
        val loginStartedAt = longPreferencesKey("login_started_at")
        val expiresAt = longPreferencesKey("expires_at")
    }

    suspend fun read(): StoredSession? {
        val prefs = context.sessionDataStore.data.first()
        val accessToken = prefs[Keys.accessToken] ?: return null
        val refreshToken = prefs[Keys.refreshToken] ?: return null
        val userId = prefs[Keys.userId] ?: return null
        val loginStartedAt = prefs[Keys.loginStartedAt] ?: return null
        val expiresAt = prefs[Keys.expiresAt] ?: return null

        return StoredSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            loginStartedAt = loginStartedAt,
            expiresAt = expiresAt,
        )
    }

    suspend fun save(session: StoredSession) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.accessToken] = session.accessToken
            prefs[Keys.refreshToken] = session.refreshToken
            prefs[Keys.userId] = session.userId
            prefs[Keys.loginStartedAt] = session.loginStartedAt
            prefs[Keys.expiresAt] = session.expiresAt
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
