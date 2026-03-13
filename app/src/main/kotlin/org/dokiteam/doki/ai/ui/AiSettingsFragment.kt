package org.dokiteam.doki.ai.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import org.dokiteam.doki.R
import org.dokiteam.doki.core.ui.BasePreferenceFragment
import org.dokiteam.doki.settings.SettingsActivity

@AndroidEntryPoint
class AiSettingsFragment : BasePreferenceFragment(R.xml.pref_ai_settings) {

    private val viewModel: AiAssistantViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(getString(R.string.ai_assistant))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_ai_settings)

        findPreference<Preference>("ai_api_key_setup")?.apply {
            summary = if (viewModel.isApiKeySet) {
                getString(R.string.ai_api_key_set)
            } else {
                getString(R.string.ai_api_key_not_set)
            }
            setOnPreferenceClickListener {
                showApiKeySetup()
                true
            }
        }

        findPreference<Preference>("ai_chat")?.setOnPreferenceClickListener {
            (activity as? SettingsActivity)?.openFragment(
                AiAssistantFragment::class.java, null, false,
            )
            true
        }

        findPreference<Preference>("ai_library")?.setOnPreferenceClickListener {
            (activity as? SettingsActivity)?.openFragment(
                AiLibraryFragment::class.java, null, false,
            )
            true
        }

        findPreference<Preference>("ai_clear_cache")?.setOnPreferenceClickListener {
            viewModel.clearChat()
            it.summary = getString(R.string.ai_cache_cleared)
            true
        }
    }

    private fun showApiKeySetup() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "AIza..."
            setText(viewModel.getApiKey())
            setPadding(48, 32, 48, 32)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.ai_api_key_title))
            .setMessage(getString(R.string.ai_api_key_message))
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val key = input.text.toString().trim()
                viewModel.saveApiKey(key)
                findPreference<Preference>("ai_api_key_setup")?.summary = if (key.isNotEmpty()) {
                    getString(R.string.ai_api_key_set)
                } else {
                    getString(R.string.ai_api_key_not_set)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
