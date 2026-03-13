package org.dokiteam.doki.ai.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.dokiteam.doki.ai.domain.AiService
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
)

@HiltViewModel
class AiAssistantViewModel @Inject constructor(
    private val aiService: AiService,
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val isApiKeySet: Boolean get() = aiService.isApiKeySet()
    fun getApiKey(): String = aiService.getApiKey()

    fun saveApiKey(key: String) {
        aiService.saveApiKey(key)
        if (key.isNotEmpty()) {
            appendMessage(ChatMessage("✅ API key saved! You can now use AI features.", isUser = false))
        }
    }

    fun sendMessage(input: String) {
        if (input.isBlank()) return
        appendMessage(ChatMessage(input, isUser = true))
        _isLoading.value = true

        viewModelScope.launch {
            val result = aiService.processCommand(input)
            _isLoading.value = false
            appendMessage(
                ChatMessage(
                    text = result.message,
                    isUser = false,
                    isError = !result.success,
                )
            )
        }
    }

    private fun appendMessage(msg: ChatMessage) {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(msg)
        _messages.value = current
    }

    fun clearChat() {
        _messages.value = emptyList()
        aiService.clearCache()
    }
}
