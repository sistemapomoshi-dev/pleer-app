package com.hiresplayer.cloud.yandex

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hiresplayer.cloud.BuildConfig
import com.hiresplayer.cloud.CloudAuthState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class YandexOAuthManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = encryptedPreferences(context.applicationContext)
    private val _authState = MutableStateFlow(
        CloudAuthState(
            isAuthorized = accessToken() != null,
            message = if (BuildConfig.YANDEX_CLIENT_ID == PLACEHOLDER_CLIENT_ID) {
                "Укажите yandexClientId в gradle.properties"
            } else {
                null
            }
        )
    )

    val authState: StateFlow<CloudAuthState> = _authState.asStateFlow()

    fun buildAuthUrl(): String {
        // Для встроенного WebView используем стандартный implicit-flow Яндекса:
        // access_token вернётся во фрагменте URL /verification_code после символа #.
        return "https://oauth.yandex.ru/authorize" +
            "?response_type=token" +
            "&client_id=${BuildConfig.YANDEX_CLIENT_ID}" +
            "&force_confirm=yes"
    }

    fun handleRedirect(uri: Uri?) {
        if (uri == null ||
            uri.scheme != REDIRECT_SCHEME ||
            uri.host != REDIRECT_HOST ||
            uri.path != REDIRECT_PATH ||
            uri.port != -1
        ) return
        val token = uri.getQueryParameter("access_token")
            ?: uri.fragment
                ?.split("&")
                ?.firstOrNull { it.startsWith("access_token=") }
                ?.substringAfter("=")
        if (token != null) {
            saveAccessToken(token)
        }
    }

    fun saveAccessToken(token: String) {
        // Токен OAuth храним через Android Keystore, а не в открытом XML-файле.
        preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
        _authState.value = CloudAuthState(isAuthorized = true)
    }

    fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun clear() {
        // commit() гарантирует удаление токена до завершения операции выхода.
        preferences.edit().remove(KEY_ACCESS_TOKEN).commit()
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        WebStorage.getInstance().deleteAllData()
        _authState.value = CloudAuthState(isAuthorized = false)
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val REDIRECT_SCHEME = "hiresplayer"
        const val REDIRECT_HOST = "oauth"
        const val REDIRECT_PATH = "/yandex"
        const val PLACEHOLDER_CLIENT_ID = "YANDEX_CLIENT_ID_NOT_SET"

        fun encryptedPreferences(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                "yandex_oauth",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
