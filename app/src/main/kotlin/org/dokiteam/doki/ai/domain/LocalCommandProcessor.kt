package org.dokiteam.doki.ai.domain

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles simple predefined commands locally without any API calls.
 * Returns null if the command is not recognized locally (should go to Gemini).
 */
@Singleton
class LocalCommandProcessor @Inject constructor(
    private val settingsManager: SettingsManager,
) {

    fun process(input: String): AiCommandResult? {
        val normalized = input.lowercase().trim()
        return when {
            // Dark mode
            normalized.containsAny("dark mode on", "enable dark mode", "turn on dark mode", "switch to dark") ->
                settingsManager.applySettingCommand("set_theme", "dark")

            normalized.containsAny("dark mode off", "disable dark mode", "turn off dark mode", "light mode on", "enable light mode", "switch to light") ->
                settingsManager.applySettingCommand("set_theme", "light")

            // Incognito
            normalized.containsAny("enable incognito", "turn on incognito", "incognito on", "start incognito") ->
                settingsManager.applySettingCommand("set_incognito", "enabled")

            normalized.containsAny("disable incognito", "turn off incognito", "incognito off", "stop incognito") ->
                settingsManager.applySettingCommand("set_incognito", "disabled")

            // NSFW
            normalized.containsAny("disable nsfw", "hide nsfw", "nsfw off", "turn off nsfw", "no nsfw") ->
                settingsManager.applySettingCommand("set_nsfw", "disabled")

            normalized.containsAny("enable nsfw", "show nsfw", "nsfw on", "turn on nsfw") ->
                settingsManager.applySettingCommand("set_nsfw", "enabled")

            // Grid size
            normalized.containsAny("increase grid", "bigger grid", "larger grid", "grid size up", "zoom in grid") ->
                settingsManager.applySettingCommand("set_grid_size", "increase")

            normalized.containsAny("decrease grid", "smaller grid", "reduce grid", "grid size down", "zoom out grid") ->
                settingsManager.applySettingCommand("set_grid_size", "decrease")

            // History grouping
            normalized.containsAny("enable history group", "group history", "enable grouping") ->
                settingsManager.applySettingCommand("set_history_grouping", "enabled")

            normalized.containsAny("disable history group", "ungroup history", "disable grouping") ->
                settingsManager.applySettingCommand("set_history_grouping", "disabled")

            // Status
            normalized.containsAny("show settings", "current settings", "what are my settings", "settings status") ->
                AiCommandResult(true, settingsManager.getCurrentStatus())

            else -> null // Not handled locally — caller should use Gemini
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
