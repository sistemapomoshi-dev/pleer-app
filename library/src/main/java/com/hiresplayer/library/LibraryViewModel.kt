package com.hiresplayer.library

import androidx.lifecycle.ViewModel
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.hiresplayer.core.model.AudioTrack
import com.hiresplayer.core.model.TrackSource
import com.hiresplayer.cloud.CloudRepository
import com.hiresplayer.core.model.Bookmark
import com.hiresplayer.core.model.Playlist
import com.hiresplayer.storage.LocalAudioScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val searchResults: List<AudioTrack> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val localAudioScanner: LocalAudioScanner,
    private val cloudRepository: CloudRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val searchResults = query.flatMapLatest { repository.searchTracks(it) }
    private val _events = MutableStateFlow<String?>(null)

    val events = _events.asStateFlow()

    init { viewModelScope.launch { repository.ensureMySongsPlaylist() } }

    val state: StateFlow<LibraryUiState> = combine(
        repository.tracks,
        searchResults,
        repository.playlists,
        repository.bookmarks,
        query,
        loading,
        error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        LibraryUiState(
            tracks = values[0] as List<AudioTrack>,
            searchResults = values[1] as List<AudioTrack>,
            playlists = values[2] as List<Playlist>,
            bookmarks = values[3] as List<Bookmark>,
            query = values[4] as String,
            isLoading = values[5] as Boolean,
            errorMessage = values[6] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun refreshFromDevice() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { localAudioScanner.scan() }
                .onSuccess { tracks -> repository.importTracks(tracks) }
                .onFailure { throwable -> error.value = throwable.localizedMessage }
            loading.value = false
        }
    }

    fun updateQuery(value: String) {
        query.value = value
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun addTrackToFirstPlaylist(track: AudioTrack) {
        viewModelScope.launch {
            val firstPlaylist = state.value.playlists.firstOrNull()
            val playlistId = firstPlaylist?.id ?: repository.createPlaylist("Избранное")
            repository.addTrackToPlaylist(playlistId, track.id)
            _events.value = "Трек добавлен в плейлист"
        }
    }

    fun addBookmark(track: AudioTrack, title: String, positionMs: Long) {
        viewModelScope.launch {
            repository.addBookmark(track.id, title, positionMs.coerceIn(0L, track.durationMs.coerceAtLeast(positionMs)))
            _events.value = "Закладка сохранена"
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark.id)
        }
    }

    fun clearEvent() {
        _events.value = null
    }

    fun updateTags(track: AudioTrack, title: String, artist: String, album: String) {
        viewModelScope.launch {
            repository.updateTrack(track.copy(title = title.trim(), artist = artist.trim(), album = album.trim()))
            _events.value = "Теги обновлены"
        }
    }

    fun removeFromLibrary(track: AudioTrack) {
        viewModelScope.launch { repository.deleteTrack(track.id); _events.value = "Трек удалён из библиотеки" }
    }

    fun deleteFromCloud(track: AudioTrack) {
        viewModelScope.launch {
            if (track.source != TrackSource.Cloud) { _events.value = "Трек не находится в облаке"; return@launch }
            runCatching { cloudRepository.deleteYandex(track.id) }
                .onSuccess { repository.deleteTrack(track.id); _events.value = "Трек удалён из облачного хранилища" }
                .onFailure { _events.value = it.localizedMessage ?: "Не удалось удалить облачный файл" }
        }
    }

    fun cacheOffline(track: AudioTrack) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val target = File(context.filesDir, "offline/${track.id.hashCode()}.audio")
                target.parentFile?.mkdirs()
                context.contentResolver.openInputStream(track.uri).use { input ->
                    requireNotNull(input) { "Файл недоступен" }
                    target.outputStream().use(input::copyTo)
                }
            }.onSuccess { _events.value = "Трек сохранён офлайн" }
                .onFailure { _events.value = it.localizedMessage ?: "Не удалось скачать трек" }
        }
    }
}
