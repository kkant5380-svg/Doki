package org.dokiteam.doki.ai.domain

import androidx.appcompat.app.AppCompatDelegate
import org.dokiteam.doki.core.prefs.AppSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    private val settings: AppSettings,
) {

    fun applySettingCommand(action: String, value: String): AiCommandResult {
        return when (action) {
            "set_theme" -> {
                val mode = when (value) {
                    "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                    "light" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
                AiCommandResult(true, "Theme set to $value mode.")
            }
            "set_nsfw" -> {
                val disable = value == "disabled"
                settings.isNsfwContentDisabled = disable
                val msg = if (disable) "NSFW content is now hidden." else "NSFW content is now visible."
                AiCommandResult(true, msg)
            }
            "set_incognito" -> {
                val enable = value == "enabled"
                settings.isIncognitoModeEnabled = enable
                val msg = if (enable) "Incognito mode enabled." else "Incognito mode disabled."
                AiCommandResult(true, msg)
            }
            "set_grid_size" -> {
                val current = settings.gridSize
                val newSize = when (value) {
                    "increase" -> (current + 10).coerceAtMost(200)
                    "decrease" -> (current - 10).coerceAtLeast(50)
                    else -> value.toIntOrNull() ?: current
                }
                settings.gridSize = newSize
                AiCommandResult(true, "Grid size set to $newSize.")
            }
            "set_history_grouping" -> {
                val enable = value == "enabled"
                settings.isHistoryGroupingEnabled = enable
                val msg = if (enable) "History grouping enabled." else "History grouping disabled."
                AiCommandResult(true, msg)
            }
            else -> AiCommandResult(false, "Unknown setting action: $action")
        }
    }

    fun getCurrentStatus(): String {
        val theme = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> "dark"
            AppCompatDelegate.MODE_NIGHT_NO -> "light"
            else -> "system"
        }
        return """
            Current settings:
            - Theme: $theme
            - NSFW content: ${if (settings.isNsfwContentDisabled) "hidden" else "visible"}
            - Incognito mode: ${if (settings.isIncognitoModeEnabled) "enabled" else "disabled"}
            - Grid size: ${settings.gridSize}
            - History grouping: ${if (settings.isHistoryGroupingEnabled) "enabled" else "disabled"}
        """.trimIndent()
    }
}
