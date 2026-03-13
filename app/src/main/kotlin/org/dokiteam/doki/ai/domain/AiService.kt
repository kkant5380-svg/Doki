package org.dokiteam.doki.ai.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.dokiteam.doki.ai.data.GeminiApiClient
import org.dokiteam.doki.ai.data.ResponseCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiApiClient: GeminiApiClient,
    private val responseCache: ResponseCache,
    private val localCommandProcessor: LocalCommandProcessor,
    private val settingsManager: SettingsManager,
    private val mangaLibraryManager: MangaLibraryManager,
) {

    private val prefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getApiKey(): String = prefs.getString(PREF_GEMINI_API_KEY, "")?.trim() ?: ""

    fun isApiKeySet(): Boolean = getApiKey().isNotEmpty()

    fun saveApiKey(key: String) {
        prefs.edit().putString(PREF_GEMINI_API_KEY, key.trim()).apply()
    }

    private val systemPrompt = """
        You are a helpful assistant inside a manga reader app called Doki.
        Your job is to control app settings and manage the manga library based on user commands.
        
        For settings commands, respond ONLY with a JSON object like:
        {"action":"set_theme","value":"dark"}
        
        Available actions:
        - set_theme: values = "dark", "light", "system"
        - set_nsfw: values = "enabled", "disabled"
        - set_incognito: values = "enabled", "disabled"
        - set_grid_size: values = "increase", "decrease"
        - set_history_grouping: values = "enabled", "disabled"
        
        For library commands (scan, duplicates, summary), respond with:
        {"action":"library","value":"scan"} or {"action":"library","value":"summary"}
        
        For unrecognized commands, respond with:
        {"action":"unknown","value":"","message":"Brief friendly explanation"}
        
        Always respond with valid JSON only. No extra text.
    """.trimIndent()

    suspend fun processCommand(userInput: String): AiCommandResult {
        // 1. Try local processing first (no API needed)
        val localResult = localCommandProcessor.process(userInput)
        if (localResult != null) return localResult

        // 2. Check cache
        val cached = responseCache.get(userInput)
        if (cached != null) {
            return interpretGeminiResponse(cached)
        }

        // 3. Require API key
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return AiCommandResult(
                false,
                "⚠️ AI features require a Gemini API key.\n\nGo to Settings → AI Assistant and enter your key to get started.",
            )
        }

        // 4. Call Gemini API
        val result = geminiApiClient.generateContent(apiKey, systemPrompt, userInput)
        return result.fold(
            onSuccess = { response ->
                responseCache.put(userInput, response)
                interpretGeminiResponse(response)
            },
            onFailure = { error ->
                AiCommandResult(false, "AI error: ${error.message ?: "Unknown error occurred."}")
            }
        )
    }

    private suspend fun interpretGeminiResponse(jsonStr: String): AiCommandResult {
        return runCatching {
            val json = org.json.JSONObject(jsonStr)
            val action = json.getString("action")
            val value = json.optString("value", "")

            when (action) {
                "library" -> when (value) {
                    "scan" -> mangaLibraryManager.scanForDuplicates()
                    "summary" -> mangaLibraryManager.getLibrarySummary()
                    else -> AiCommandResult(false, "Unknown library action.")
                }
                "unknown" -> {
                    val message = json.optString("message", "I didn't understand that command.")
                    AiCommandResult(false, message)
                }
                else -> settingsManager.applySettingCommand(action, value)
            }
        }.getOrElse {
            // Gemini returned plain text instead of JSON — show it directly
            AiCommandResult(true, jsonStr)
        }
    }

    suspend fun scanLibraryForDuplicates(): AiCommandResult =
        mangaLibraryManager.scanForDuplicates()

    suspend fun getLibrarySummary(): AiCommandResult =
        mangaLibraryManager.getLibrarySummary()

    suspend fun removeMangaFromLibrary(mangaId: Long, source: String): AiCommandResult =
        mangaLibraryManager.removeFromLibrary(mangaId, source)

    fun clearCache() = responseCache.clear()

    companion object {
        const val PREF_GEMINI_API_KEY = "gemini_api_key"
    }
}
