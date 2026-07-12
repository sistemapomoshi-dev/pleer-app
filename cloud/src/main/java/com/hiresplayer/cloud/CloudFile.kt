package com.hiresplayer.cloud

data class CloudFile(
    val id: String,
    val name: String,
    val path: String,
    val type: CloudFileType,
    val size: Long?,
    val mimeType: String?,
    val downloadUrl: String? = null
)

enum class CloudFileType {
    File,
    Directory
}

data class CloudAuthState(
    val isAuthorized: Boolean = false,
    val providerName: String = "Яндекс.Диск",
    val message: String? = null
)
