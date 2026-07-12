package com.hiresplayer.cloud

interface CloudProvider {
    val id: String
    val title: String

    suspend fun list(path: String): List<CloudFile>
    suspend fun getDownloadUrl(path: String): String
    suspend fun signOut()
}
