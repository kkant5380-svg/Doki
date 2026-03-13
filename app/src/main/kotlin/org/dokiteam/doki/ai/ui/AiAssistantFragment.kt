package org.dokiteam.doki.ai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.dokiteam.doki.R

@AndroidEntryPoint
class AiAssistantFragment : Fragment() {

    private val viewModel: AiAssistantViewModel by viewModels()

    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var apiKeyBanner: View
    private lateinit var apiKeyBannerButton: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.fragment_ai_assistant, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatContainer = view.findViewById(R.id.ai_chat_container)
        scrollView = view.findViewById(R.id.ai_scroll_view)
        inputField = view.findViewById(R.id.ai_input_field)
        sendButton = view.findViewById(R.id.ai_send_button)
        progressBar = view.findViewById(R.id.ai_progress_bar)
        apiKeyBanner = view.findViewById(R.id.ai_api_key_banner)
        apiKeyBannerButton = view.findViewById(R.id.ai_api_key_button)

        updateApiKeyBanner()

        sendButton.setOnClickListener { sendMessage() }
        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        apiKeyBannerButton.setOnClickListener { showApiKeyDialog() }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatContainer.removeAllViews()
            messages.forEach { msg -> addMessageView(msg) }
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.isVisible = loading
            sendButton.isEnabled = !loading
            inputField.isEnabled = !loading
        }

        if (viewModel.isApiKeySet && chatContainer.childCount == 0) {
            addWelcomeMessage()
        }
    }

    private fun updateApiKeyBanner() {
        apiKeyBanner.isVisible = !viewModel.isApiKeySet
    }

    private fun addWelcomeMessage() {
        val welcome = ChatMessage(
            "👋 Hi! I'm your Doki AI Assistant. I can help you:\n\n" +
                "• Control settings (\"turn on dark mode\")\n" +
                "• Manage incognito & NSFW filters\n" +
                "• Adjust grid size\n" +
                "• Check library status\n\n" +
                "What would you like to do?",
            isUser = false,
        )
        addMessageView(welcome)
    }

    private fun sendMessage() {
        val text = inputField.text.toString().trim()
        if (text.isEmpty()) return
        inputField.setText("")
        viewModel.sendMessage(text)
    }

    private fun addMessageView(msg: ChatMessage) {
        val textView = TextView(requireContext()).apply {
            text = msg.text
            setPadding(32, 20, 32, 20)
            textSize = 14f

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(16, 8, 16, 8)
                gravity = if (msg.isUser) android.view.Gravity.END else android.view.Gravity.START
            }
            layoutParams = params

            val bgColor = when {
                msg.isUser -> 0xFF6200EE.toInt()
                msg.isError -> 0xFFB00020.toInt()
                else -> 0xFF2C2C2C.toInt()
            }
            setBackgroundColor(bgColor)
            setTextColor(0xFFFFFFFF.toInt())
        }
        chatContainer.addView(textView)
    }

    private fun showApiKeyDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Enter Gemini API Key"
            setText(viewModel.getApiKey())
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("🔑 Gemini API Key")
            .setMessage("Get your free API key from Google AI Studio (aistudio.google.com)")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val key = input.text.toString().trim()
                viewModel.saveApiKey(key)
                updateApiKeyBanner()
                if (chatContainer.childCount == 0) addWelcomeMessage()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        fun newInstance() = AiAssistantFragment()
    }
}
