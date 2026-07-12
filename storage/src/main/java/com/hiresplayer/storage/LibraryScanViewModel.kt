package com.hiresplayer.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiresplayer.core.model.AudioTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryScanState(
    val tracks: List<AudioTrack> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LibraryScanViewModel @Inject constructor(
    private val localAudioScanner: LocalAudioScanner
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryScanState())
    val state: StateFlow<LibraryScanState> = _state.asStateFlow()

    fun scan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching { localAudioScanner.scan() }
                .onSuccess { tracks ->
                    _state.value = LibraryScanState(tracks = tracks)
                }
                .onFailure { error ->
                    _state.value = LibraryScanState(errorMessage = error.localizedMessage)
                }
        }
    }
}
