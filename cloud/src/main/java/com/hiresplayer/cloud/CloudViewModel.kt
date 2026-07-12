package com.hiresplayer.cloud

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiresplayer.core.model.AudioTrack
import com.hiresplayer.core.model.TrackSource
import com.hiresplayer.cloud.CloudSyncManager.Companion.isSupportedAudio
import com.hiresplayer.cloud.yandex.YandexOAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CloudUiState(
    val authState: CloudAuthState = CloudAuthState(),
    val path: String = "disk:/",
    val files: List<CloudFile> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val syncMessage: String? = null
)

@HiltViewModel
class CloudViewModel @Inject constructor(
    private val repository: CloudRepository,
    private val yandexOAuthManager: YandexOAuthManager
) : ViewModel() {
    private val path = MutableStateFlow("disk:/")
    private val files = MutableStateFlow<List<CloudFile>>(emptyList())
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val syncMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<CloudUiState> = combine(
        repository.authState,
        path,
        files,
        loading,
        error
    ) { auth, currentPath, currentFiles, isLoading, errorMessage ->
        CloudUiState(
            authState = auth,
            path = currentPath,
            files = currentFiles,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }.combine(syncMessage) { currentState, currentSyncMessage ->
        currentState.copy(syncMessage = currentSyncMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CloudUiState())

    val authUrl: String
        get() = repository.buildAuthUrl()

    fun handleRedirect(uri: Uri?) {
        yandexOAuthManager.handleRedirect(uri)
        refresh()
    }

    fun refresh() {
        openPath(path.value)
    }

    fun openPath(value: String) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            syncMessage.value = null
            val normalizedPath = normalizePath(value)
            runCatching { repository.listYandex(normalizedPath) }
                .onSuccess { loadedFiles ->
                    path.value = normalizedPath
                    files.value = loadedFiles
                        .filter { it.type == CloudFileType.Directory || it.isSupportedAudio() }
                        .sortedWith(compareBy<CloudFile> { it.type != CloudFileType.Directory }.thenBy { it.name.lowercase() })
                }
                .onFailure { error.value = "Не удалось загрузить список файлов" }
            loading.value = false
        }
    }

    fun openParent() {
        val current = path.value.removeSuffix("/")
        val parent = current.substringBeforeLast("/", missingDelimiterValue = "disk:")
        openPath(if (parent == "disk:") "disk:/" else parent)
    }

    fun enqueueDownload(file: CloudFile) {
        repository.enqueueDownload(file)
    }

    fun resolvePlayableTrack(file: CloudFile, onReady: (AudioTrack) -> Unit) {
        if (!file.isSupportedAudio()) return
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { repository.getYandexDownloadUrl(file.path) }
                .onSuccess { url ->
                    onReady(
                        AudioTrack(
                            id = file.path,
                            title = file.name.substringBeforeLast("."),
                            artist = "Яндекс.Диск",
                            album = "Облако",
                            durationMs = 0L,
                            uri = Uri.parse(url),
                            source = TrackSource.Cloud
                        )
                    )
                }
                .onFailure { throwable -> error.value = throwable.localizedMessage }
            loading.value = false
        }
    }

    fun addCurrentFolderToLibrary() {
        syncFolder(path.value)
    }

    fun syncFolder(folderPath: String) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            syncMessage.value = null
            runCatching { repository.syncFolder(folderPath) }
                .onSuccess { count ->
                    syncMessage.value = "Добавлено в библиотеку: $count"
                }
                .onFailure {
                    error.value = "Не удалось добавить папку в библиотеку"
                }
            loading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            files.value = emptyList()
        }
    }

    private fun normalizePath(value: String): String =
        when {
            value.isBlank() || value == "/" -> "disk:/"
            value.startsWith("disk:") -> value
            else -> "disk:${if (value.startsWith("/")) value else "/$value"}"
        }
}
