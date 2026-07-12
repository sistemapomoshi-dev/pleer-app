package com.hiresplayer.app

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.hiresplayer.cloud.yandex.YandexDiskProvider
import com.hiresplayer.cloud.yandex.YandexOAuthManager
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import javax.inject.Inject

@AndroidEntryPoint
class YandexAuthActivity : ComponentActivity() {
    @Inject lateinit var yandexDiskProvider: YandexDiskProvider
    @Inject lateinit var yandexOAuthManager: YandexOAuthManager

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorPanel: View
    private lateinit var errorText: TextView
    private lateinit var retryButton: Button

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yandex_auth)

        webView = findViewById(R.id.yandexAuthWebView)
        progressBar = findViewById(R.id.yandexAuthProgress)
        errorPanel = findViewById(R.id.yandexAuthErrorPanel)
        errorText = findViewById(R.id.yandexAuthErrorText)
        retryButton = findViewById(R.id.yandexAuthRetryButton)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webView.settings.safeBrowsingEnabled = true
        webView.settings.setSupportMultipleWindows(false)
        webView.webViewClient = YandexAuthWebViewClient()

        retryButton.setOnClickListener {
            loadAuthPage()
        }

        loadAuthPage()
    }

    private fun loadAuthPage() {
        errorPanel.visibility = View.GONE
        webView.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        webView.loadUrl(yandexOAuthManager.buildAuthUrl())
    }

    private fun handlePossibleRedirect(url: String?): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        if (uri.scheme != HTTPS_SCHEME ||
            uri.host != YANDEX_OAUTH_HOST ||
            uri.path != YANDEX_VERIFICATION_PATH ||
            uri.port != -1
        ) return false

        val values = parseFragment(uri.fragment) + uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }
        val token = values["access_token"]
        val error = values["error"]

        if (!token.isNullOrBlank()) {
            // Сохраняем токен через существующий провайдер, чтобы CloudProvider-флоу остался прежним.
            yandexDiskProvider.authorize(token)
            setResult(Activity.RESULT_OK)
            finish()
            return true
        }

        if (error == "access_denied") {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return true
        }

        showError("Не удалось получить токен Яндекс.Диска. Попробуйте войти ещё раз.")
        return true
    }

    private fun parseFragment(fragment: String?): Map<String, String> {
        if (fragment.isNullOrBlank()) return emptyMap()
        return fragment.split("&")
            .mapNotNull { part ->
                val key = part.substringBefore("=", missingDelimiterValue = "").takeIf { it.isNotBlank() }
                val value = part.substringAfter("=", missingDelimiterValue = "")
                key?.let { decode(it) to decode(value) }
            }
            .toMap()
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, Charsets.UTF_8.name())

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        webView.visibility = View.GONE
        errorText.text = message
        errorPanel.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.clearCache(true)
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }

    private inner class YandexAuthWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            handlePossibleRedirect(request.url.toString())

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            if (handlePossibleRedirect(url)) return
            progressBar.visibility = View.VISIBLE
            errorPanel.visibility = View.GONE
            webView.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (handlePossibleRedirect(url)) return
            progressBar.visibility = View.GONE
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                showError("Страница Яндекса не загрузилась. Проверьте интернет и попробуйте снова.")
            }
        }
    }

    companion object {
        const val ACTION_YANDEX_AUTH = "com.hiresplayer.app.action.YANDEX_AUTH"
        private const val HTTPS_SCHEME = "https"
        private const val YANDEX_OAUTH_HOST = "oauth.yandex.ru"
        private const val YANDEX_VERIFICATION_PATH = "/verification_code"
    }
}
