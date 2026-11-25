package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AIProviderType
import com.example.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.konozDataStore by preferencesDataStore(name = "konoz_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val PROVIDER_TYPE = stringPreferencesKey("provider_type")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val CUSTOM_PROXY_URL = stringPreferencesKey("custom_proxy_url")
        val CUSTOM_PROXY_TOKEN = stringPreferencesKey("custom_proxy_token")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
    }

    val settingsFlow: Flow<AppSettings> = context.konozDataStore.data.map { prefs ->
        val provider = try {
            AIProviderType.valueOf(prefs[Keys.PROVIDER_TYPE] ?: AIProviderType.FIREBASE_GEMINI.name)
        } catch (_: Exception) {
            AIProviderType.FIREBASE_GEMINI
        }
        AppSettings(
            providerType = provider,
            geminiModel = prefs[Keys.GEMINI_MODEL] ?: "gemini-3.8-flash",
            customProxyUrl = prefs[Keys.CUSTOM_PROXY_URL] ?: "",
            customProxyToken = prefs[Keys.CUSTOM_PROXY_TOKEN] ?: "",
            timeoutSeconds = (prefs[Keys.TIMEOUT_SECONDS] ?: 45).coerceIn(30, 60),
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "ar"
        )
    }

    suspend fun updateProviderType(type: AIProviderType) {
        context.konozDataStore.edit { it[Keys.PROVIDER_TYPE] = type.name }
    }

    suspend fun updateGeminiModel(model: String) {
        context.konozDataStore.edit { it[Keys.GEMINI_MODEL] = model }
    }

    suspend fun updateCustomProxy(url: String, token: String) {
        context.konozDataStore.edit {
            it[Keys.CUSTOM_PROXY_URL] = url.trim()
            it[Keys.CUSTOM_PROXY_TOKEN] = token.trim()
        }
    }

    suspend fun updateTimeoutSeconds(seconds: Int) {
        context.konozDataStore.edit { it[Keys.TIMEOUT_SECONDS] = seconds.coerceIn(30, 60) }
    }

    suspend fun updateDarkTheme(dark: Boolean) {
        context.konozDataStore.edit { it[Keys.DARK_THEME] = dark }
    }

    suspend fun updateLanguageCode(lang: String) {
        context.konozDataStore.edit { it[Keys.LANGUAGE_CODE] = lang }
    }
}
