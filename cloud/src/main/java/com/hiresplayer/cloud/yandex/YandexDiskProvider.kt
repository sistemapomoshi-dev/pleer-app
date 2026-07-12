package com.hiresplayer.cloud.yandex

import com.hiresplayer.cloud.CloudFile
import com.hiresplayer.cloud.CloudFileType
import com.hiresplayer.cloud.CloudProvider
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Singleton
class YandexDiskProvider @Inject constructor(
    private val oauthManager: YandexOAuthManager
) : CloudProvider {
    override val id: String = "yandex_disk"
    override val title: String = "Яндекс.Диск"

    private val httpClient = OkHttpClient.Builder()
        .certificatePinner(
            CertificatePinner.Builder()
                // Pin открытого ключа GlobalSign Root R3 из действующей цепочки API Яндекса.
                .add(YANDEX_API_HOST, GLOBALSIGN_ROOT_R3_PIN)
                .build()
        )
        .build()

    fun authorize(authCode: String) {
        // В implicit-flow Яндекса сюда приходит готовый access_token из фрагмента URL.
        oauthManager.saveAccessToken(authCode)
    }

    override suspend fun list(path: String): List<CloudFile> = withContext(Dispatchers.IO) {
        val normalizedPath = path.ifBlank { "disk:/" }
        val url = API_RESOURCES.toHttpUrl().newBuilder()
            .addQueryParameter("path", normalizedPath)
            .addQueryParameter("limit", "200")
            .addQueryParameter(
                "fields",
                "path,name,type,size,mime_type,_embedded.items.path,_embedded.items.name," +
                    "_embedded.items.type,_embedded.items.size,_embedded.items.mime_type"
            )
            .build()
        val json = requestJson(url.toString())
        val embedded = json.optJSONObject("_embedded") ?: return@withContext emptyList()
        val items = embedded.optJSONArray("items") ?: return@withContext emptyList()
        buildList {
            for (index in 0 until items.length()) {
                add(items.getJSONObject(index).toCloudFile())
            }
        }
    }

    override suspend fun getDownloadUrl(path: String): String = withContext(Dispatchers.IO) {
        val url = API_DOWNLOAD.toHttpUrl().newBuilder()
            .addQueryParameter("path", path)
            .build()
        val downloadUrl = requestJson(url.toString()).getString("href")
        val parsed = downloadUrl.toHttpUrlOrNull()
        if (parsed == null || !parsed.isHttps) {
            throw IOException("Яндекс.Диск вернул небезопасную ссылку для скачивания")
        }
        downloadUrl
    }

    suspend fun delete(path: String): Unit = withContext(Dispatchers.IO) {
        val token = oauthManager.accessToken() ?: throw YandexAuthenticationRequiredException()
        val url = API_RESOURCES.toHttpUrl().newBuilder().addQueryParameter("path", path).addQueryParameter("permanently", "false").build()
        val request = Request.Builder().url(url).header("Authorization", "OAuth $token").delete().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 202 && response.code != 204) throw IOException("Ошибка удаления из Яндекс.Диска: ${response.code}")
        }
    }

    override suspend fun signOut() {
        oauthManager.clear()
    }

    private fun requestJson(endpoint: String): JSONObject {
        val token = oauthManager.accessToken() ?: throw YandexAuthenticationRequiredException()
        val url = endpoint.toHttpUrl()
        require(url.isHttps && url.host == YANDEX_API_HOST) { "Недопустимый адрес API Яндекс.Диска" }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "OAuth $token")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) {
                // Недействительный токен удаляется, StateFlow переводит UI на экран входа.
                oauthManager.clear()
                throw YandexAuthenticationRequiredException()
            }
            if (!response.isSuccessful) {
                // Тело ответа не пробрасывается в UI: оно может содержать служебные данные.
                throw IOException("Ошибка Яндекс.Диска: ${response.code}")
            }
            val body = response.body?.string()
                ?: throw IOException("Яндекс.Диск вернул пустой ответ")
            return JSONObject(body)
        }
    }

    private fun JSONObject.toCloudFile(): CloudFile =
        CloudFile(
            id = optString("path"),
            name = optString("name"),
            path = optString("path"),
            type = if (optString("type") == "dir") CloudFileType.Directory else CloudFileType.File,
            size = if (has("size")) optLong("size") else null,
            mimeType = optString("mime_type").ifBlank { null }
        )

    private companion object {
        const val YANDEX_API_HOST = "cloud-api.yandex.net"
        const val API_RESOURCES = "https://cloud-api.yandex.net/v1/disk/resources"
        const val API_DOWNLOAD = "https://cloud-api.yandex.net/v1/disk/resources/download"
        const val GLOBALSIGN_ROOT_R3_PIN = "sha256/cGuxAXyFXFkWm61cF4HPWX8S0srS9j0aSqN0k4AP+4A="
    }
}
