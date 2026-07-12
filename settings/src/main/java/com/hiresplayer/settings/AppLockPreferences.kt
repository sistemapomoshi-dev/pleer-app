package com.hiresplayer.settings

import android.content.Context
import java.security.MessageDigest

class AppLockPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("app_lock", Context.MODE_PRIVATE)
    val isEnabled: Boolean get() = preferences.contains(KEY_PIN)
    fun setPin(pin: String) { require(pin.length in 4..8 && pin.all(Char::isDigit)); preferences.edit().putString(KEY_PIN, hash(pin)).apply() }
    fun verify(pin: String): Boolean = preferences.getString(KEY_PIN, null) == hash(pin)
    fun disable() { preferences.edit().remove(KEY_PIN).apply() }
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private companion object { const val KEY_PIN = "pin_hash" }
}
