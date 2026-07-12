package com.hiresplayer.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class CloudConnectActivity : ComponentActivity() {
    private val yandexAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_connect)

        findViewById<Button>(R.id.connectYandexButton).setOnClickListener {
            yandexAuthLauncher.launch(Intent(this, YandexAuthActivity::class.java))
        }
    }

    companion object {
        const val ACTION_CONNECT_CLOUD = "com.hiresplayer.app.action.CONNECT_CLOUD"
    }
}
