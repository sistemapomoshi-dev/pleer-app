package com.hiresplayer.app

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import com.hiresplayer.cloud.yandex.YandexOAuthManager
import com.hiresplayer.ui.HiResPlayerRoot
import com.hiresplayer.ui.theme.HiResPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var yandexOAuthManager: YandexOAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        yandexOAuthManager.handleRedirect(intent?.data)
        setContent {
            HiResPlayerTheme {
                HiResPlayerRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        yandexOAuthManager.handleRedirect(intent.data)
    }
}
