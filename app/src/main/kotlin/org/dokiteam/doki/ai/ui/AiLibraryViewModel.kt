package org.dokiteam.doki.ai.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.dokiteam.doki.ai.domain.AiService
import org.dokiteam.doki.ai.domain.DuplicatePair
import javax.inject.Inject

@HiltViewModel
class AiLibraryViewModel @Inject constructor(
    private val aiService: AiService,
) : ViewModel() {

    private val _duplicates = MutableLiveData<List<DuplicatePair>>(emptyList())
    val duplicates: LiveData<List<DuplicatePair>> = _duplicates

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun scanForDuplicates() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = aiService.scanLibraryForDuplicates()
            _isLoading.value = false
            _statusMessage.value = result.message
            @Suppress("UNCHECKED_CAST")
            _duplicates.value = (result.data as? List<DuplicatePair>) ?: emptyList()
        }
    }

    fun removeEntry(duplicate: DuplicatePair) {
        viewModelScope.launch {
            val result = aiService.removeMangaFromLibrary(duplicate.mangaId, duplicate.source)
            if (result.success) {
                val updated = _duplicates.value.orEmpty().toMutableList()
                updated.remove(duplicate)
                _duplicates.value = updated
                _statusMessage.value = "Removed '${duplicate.title}' from ${duplicate.source}."
            } else {
                _statusMessage.value = result.message
            }
        }
    }

    fun loadSummary() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = aiService.getLibrarySummary()
            _isLoading.value = false
            _statusMessage.value = result.message
        }
    }
}
