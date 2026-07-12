package com.hiresplayer.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlayerAction(val title: String, val symbol: String) {
    RepeatShuffle("Режим повтора и shuffle", "↻"), Next("Следующий трек", "▶▶"),
    Previous("Предыдущий трек", "◀◀"), Seek("Пропустить время вперёд/назад", "±15"),
    SleepTimer("Таймер сна", "◷"), Equalizer("Аудио эквалайзер", "≋"),
    Search("Поиск", "⌕"), Bookmarks("Закладки", "◆")
}

class ActionBarPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("player_personalization", Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(read())
    val enabled: StateFlow<Set<PlayerAction>> = _enabled.asStateFlow()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _enabled.value = read() }
    init { prefs.registerOnSharedPreferenceChangeListener(listener) }
    fun setEnabled(action: PlayerAction, enabled: Boolean) {
        prefs.edit().putBoolean(action.name, enabled).apply()
    }
    private fun read() = PlayerAction.entries.filterTo(linkedSetOf()) { prefs.getBoolean(it.name, true) }
}
