package com.hiresplayer.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.hiresplayer.core.model.AudioTrack
import com.hiresplayer.core.model.Bookmark
import com.hiresplayer.library.LibraryViewModel
import com.hiresplayer.player.PlayerViewModel
import com.hiresplayer.player.EqualizerState
import com.hiresplayer.player.SleepTimerState
import com.hiresplayer.settings.ActionBarPreferences
import com.hiresplayer.settings.PlayerAction
import com.hiresplayer.settings.AppLockPreferences
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat.getMainExecutor
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.util.Locale
import java.text.Collator
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private val Black = Color(0xFF000000)
private val Panel = Color(0xFF181818)
private val Panel2 = Color(0xFF242424)
private val Line = Color(0xFF303030)
private val White = Color(0xFFF5F5F7)
private val Muted = Color(0xFF929297)
private val Orange = Color(0xFFFF9F0A)

private enum class Tab(val title: String, val icon: String) {
    Lists("Списки", "≡♫"), Library("Библиотека", "▤"), Connect("Подключить", "◎"),
    Files("Файлы", "▰"), Settings("Настройки", "⚙")
}

@Composable
fun HiResPlayerRoot(
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val library by libraryViewModel.state.collectAsState()
    val playback by playerViewModel.playbackState.collectAsState()
    var tab by remember { mutableStateOf(Tab.Library) }
    var fullPlayer by remember { mutableStateOf(false) }
    var personalization by remember { mutableStateOf(false) }
    var equalizer by remember { mutableStateOf(false) }
    var sleepTimer by remember { mutableStateOf(false) }
    var speedPicker by remember { mutableStateOf(false) }
    var bookmarksScreen by remember { mutableStateOf(false) }
    var passwordScreen by remember { mutableStateOf(false) }
    var menuTrack by remember { mutableStateOf<AudioTrack?>(null) }
    val context = LocalContext.current
    val actionPreferences = remember { ActionBarPreferences(context) }
    val lockPreferences = remember { AppLockPreferences(context) }
    var unlocked by remember { mutableStateOf(!lockPreferences.isEnabled) }
    val enabledActions by actionPreferences.enabled.collectAsState()
    val equalizerState by playerViewModel.equalizerState.collectAsState()
    val sleepTimerState by playerViewModel.sleepTimerState.collectAsState()
    val playbackSpeed by playerViewModel.playbackSpeed.collectAsState()
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = it
        if (it) libraryViewModel.refreshFromDevice()
    }
    LaunchedEffect(allowed) { if (allowed && library.tracks.isEmpty()) libraryViewModel.refreshFromDevice() }

    Box(Modifier.fillMaxSize().background(Black)) {
        when (tab) {
            Tab.Lists -> PlaylistsScreen(library.playlists)
            Tab.Library -> LibraryScreen(library.tracks, allowed, { launcher.launch(permission) }, { menuTrack = it }) { track ->
                playerViewModel.play(track); fullPlayer = true
            }
            Tab.Connect -> ConnectScreen()
            Tab.Files -> FilesScreen()
            Tab.Settings -> SettingsScreen({ personalization = true }, { passwordScreen = true }, { tab = Tab.Files })
        }
        Column(Modifier.align(Alignment.BottomCenter)) {
            AnimatedVisibility(playback.currentTrack != null) {
                MiniPlayer(playback.currentTrack, playback.isPlaying, playback.positionMs, playback.durationMs,
                    { fullPlayer = true }, playerViewModel::togglePlayPause, playerViewModel::seekForward)
            }
            TabBar(tab) { tab = it }
        }
    }

    if (fullPlayer && playback.currentTrack != null) {
        PlayerScreen(playback.currentTrack!!, playback.isPlaying, playback.positionMs, playback.durationMs, enabledActions,
            { fullPlayer = false }, playerViewModel::togglePlayPause, playerViewModel::seekBackward,
            playerViewModel::seekForward, playerViewModel::seekTo, playerViewModel::skipToPrevious,
            playerViewModel::skipToNext, playbackSpeed, { equalizer = true }, { sleepTimer = true }, { bookmarksScreen = true }) { speedPicker = true }
    }
    if (personalization) PersonalizationScreen(enabledActions, actionPreferences::setEnabled) { personalization = false }
    if (equalizer) EqualizerScreen(equalizerState, playerViewModel::setEqualizerEnabled,
        { band, level -> playerViewModel.setEqualizerBandLevel(band.toShort(), level) },
        { playerViewModel.setBassBoost((it * 100).toInt().toShort()) }) { equalizer = false }
    if (sleepTimer) SleepTimerScreen(sleepTimerState, playerViewModel::startSleepTimer, playerViewModel::stopAtEndOfTrack, playerViewModel::cancelSleepTimer) { sleepTimer = false }
    if (speedPicker) PlaybackSpeedSheet(playbackSpeed, playerViewModel::setPlaybackSpeed) { speedPicker = false }
    if (bookmarksScreen) BookmarksScreen(library.bookmarks, playback.currentTrack, { playerViewModel.seekTo(it.positionMs); fullPlayer = true; bookmarksScreen = false }) { bookmarksScreen = false }
    if (passwordScreen) PasswordSettingsScreen(lockPreferences) { passwordScreen = false }
    menuTrack?.let { TrackContextMenu(it, playback.positionMs, libraryViewModel, playerViewModel) { menuTrack = null } }
    if (!unlocked) AppUnlockScreen(lockPreferences) { unlocked = true }
}

@Composable private fun Header(title: String, left: String = "+", right: String = "•••") {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleButton(left)
        Surface(color = Panel, shape = RoundedCornerShape(34.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C4C4F)), modifier = Modifier.weight(1f).padding(horizontal = 18.dp)) {
            Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 25.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 15.dp))
        }
        CircleButton(right)
    }
}

@Composable private fun CircleButton(text: String, onClick: () -> Unit = {}) {
    Surface(color = Panel, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C4C4F)), modifier = Modifier.size(60.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) { Text(text, color = White, fontSize = if (text == "•••") 20.sp else 35.sp, fontWeight = FontWeight.Light) }
    }
}

@Composable private fun ActionRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        listOf("⌕", "▶", "⤨").forEach { icon ->
            Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(54.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(icon, color = White, fontSize = 28.sp) }
            }
        }
    }
}

@Composable private fun LibraryScreen(tracks: List<AudioTrack>, allowed: Boolean, onPermission: () -> Unit, onLongPress: (AudioTrack) -> Unit, onTrack: (AudioTrack) -> Unit) {
    val collator = remember { Collator.getInstance(Locale("ru")).apply { strength = Collator.PRIMARY } }
    val sortedTracks = remember(tracks) { tracks.sortedWith(compareBy(collator) { it.title }) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val alphabet = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ".toList()
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 175.dp)) {
        Header("Библиотека")
        Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Panel).padding(7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("Плейлисты", color = White, modifier = Modifier.padding(10.dp)); Text("Песни", color = Black, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(22.dp)).background(Orange).padding(horizontal = 22.dp, vertical = 10.dp)); Text("Неигранные песни", color = White, modifier = Modifier.padding(10.dp))
        }
        ActionRow()
        if (!allowed) EmptyState("Нужен доступ к аудиофайлам", "Разрешить", onPermission)
        else if (tracks.isEmpty()) EmptyState("На устройстве пока нет песен", "Обновить")
        else Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(end = 24.dp)) {
                items(sortedTracks, key = { it.id }) { TrackItem(it, { onLongPress(it) }) { onTrack(it) } }
            }
            Column(Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 3.dp), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                alphabet.forEach { letter -> Text(letter.toString(), color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp).clickable {
                    val index = sortedTracks.indexOfFirst { it.title.trim().uppercase(Locale("ru")).firstOrNull() == letter }
                    if (index >= 0) scope.launch { listState.scrollToItem(index) }
                }, textAlign = TextAlign.Center) }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable private fun TrackItem(track: AudioTrack, onLongClick: () -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(vertical = 7.dp).size(64.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF373737)), contentAlignment = Alignment.Center) { Text("♪", color = Color(0xFF242424), fontSize = 33.sp) }
        Column(Modifier.weight(1f).padding(horizontal = 16.dp).padding(vertical = 12.dp)) {
            Text(track.title, color = White, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (track.artist.isBlank()) "Неизвестный исполнитель" else track.artist, color = Muted, fontSize = 14.sp, maxLines = 1)
            Box(Modifier.padding(top = 8.dp).fillMaxWidth(.7f).height(2.dp).background(Orange))
        }
        Text("•••", color = Muted, fontSize = 17.sp)
    }
    HorizontalDivider(color = Line, modifier = Modifier.padding(start = 98.dp))
}

@Composable private fun EmptyState(title: String, action: String, onClick: () -> Unit = {}) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("♫", color = Color(0xFF353535), fontSize = 100.sp); Text(title, color = White, fontSize = 20.sp)
        TextButton(onClick = onClick) { Text(action, color = Orange) }
    }
}

@Composable private fun LegacyPlaylistsScreen() {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 175.dp)) {
        Header("Плейлисты"); ActionRow()
        Box(Modifier.padding(42.dp).size(150.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF373737)), contentAlignment = Alignment.Center) { Text("≡♫", color = Color(0xFF292929), fontSize = 55.sp) }
        Text("мои песни", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 85.dp)); Text("20 минут", color = Muted, modifier = Modifier.padding(start = 85.dp, top = 4.dp))
    }
}

@Composable private fun LegacyFilesScreen() {
    val rows = listOf("Избранное" to "♥", "Недавние" to "◷", "Документы" to "▣", "Загрузки" to "↓", "Офлайн папки" to "↻", "iCloud" to "☁")
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 175.dp)) {
        Header("Локальные файлы", "↕", "?")
        Text("БЫСТРЫЙ ДОСТУП", color = Muted, fontSize = 18.sp, modifier = Modifier.padding(22.dp))
        rows.forEach { SettingsRow(it.first, it.second) }
        Text("Открывайте файлы и папки, расположенные на вашем устройстве.", color = Muted, fontSize = 17.sp, lineHeight = 23.sp, modifier = Modifier.padding(22.dp))
    }
}

@Composable private fun ConnectScreen() {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 175.dp)) {
        Header("Подключить", "↕", "?")
        Text("ОБЛАЧНЫЕ ХРАНИЛИЩА", color = Muted, fontSize = 18.sp, modifier = Modifier.padding(22.dp))
        SettingsRow("Яндекс.Диск", "Я"); SettingsRow("Google Drive", "G"); SettingsRow("Dropbox", "◈")
    }
}

@Composable private fun LegacySettingsScreen(onPersonalization: () -> Unit) {
    val first = listOf("Премиум версия" to "★", "Покупки" to "▣", "Аудиоплеер" to "▶", "Библиотека" to "▤", "Пароль" to "●", "Файловый менеджер" to "▰", "Редактор аудио тегов" to "▧", "CarPlay" to "▱", "Виджеты" to "▥", "Wi‑Fi Drive" to "⌁", "Персонализация" to "◉", "Язык" to "A")
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 175.dp)) {
        Header("Настройки", "", "?")
        LazyColumn { item { Text("НАСТРОЙКИ ПРИЛОЖЕНИЯ", color = Muted, fontSize = 18.sp, modifier = Modifier.padding(22.dp)) }; item { SettingsRow("Персонализация", "◉", onPersonalization) }; items(first.filterNot { it.first == "Персонализация" }) { SettingsRow(it.first, it.second) } }
    }
}

@Composable private fun SettingsRow(title: String, icon: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(78.dp).then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)).padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = Orange, fontSize = 27.sp, modifier = Modifier.width(50.dp)); Text(title, color = White, fontSize = 21.sp, modifier = Modifier.weight(1f)); Text("›", color = Muted, fontSize = 34.sp)
    }
    HorizontalDivider(color = Line, modifier = Modifier.padding(start = 72.dp))
}

@Composable private fun MiniPlayer(track: AudioTrack?, playing: Boolean, position: Long, duration: Long, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    if (track == null) return
    Surface(color = Panel.copy(alpha = .97f), shape = RoundedCornerShape(38.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF555555)), modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 12.dp, vertical = 3.dp).clickable(onClick = onOpen)) {
        Column {
            Row(Modifier.weight(1f).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF363636)), contentAlignment = Alignment.Center) { Text("♪", color = Color(0xFF202020), fontSize = 28.sp) }
                Text(track.title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
                Text("↶\n15", color = White, textAlign = TextAlign.Center, lineHeight = 13.sp, modifier = Modifier.padding(horizontal = 10.dp)); Text(if (playing) "Ⅱ" else "▶", color = White, fontSize = 27.sp, modifier = Modifier.clickable(onClick = onToggle).padding(10.dp)); Text("▶▶", color = White, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onNext).padding(10.dp))
            }
            val p = if (duration > 0) position.toFloat() / duration else 0f
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF303030))) { Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).height(3.dp).background(Orange)) }
        }
    }
}

@Composable private fun TabBar(selected: Tab, onTab: (Tab) -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(42.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF555555)), modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 7.dp).navigationBarsPadding()) {
        Row(Modifier.height(80.dp), horizontalArrangement = Arrangement.SpaceAround) {
            Tab.entries.forEach { tab ->
                Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(38.dp)).background(if (tab == selected) Color(0xFF3A3A3C) else Color.Transparent).clickable { onTab(tab) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(tab.icon, color = if (tab == selected) Orange else Muted, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(tab.title, color = if (tab == selected) Orange else White, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable private fun PlayerScreen(track: AudioTrack, playing: Boolean, position: Long, duration: Long, actions: Set<PlayerAction>, onClose: () -> Unit, onToggle: () -> Unit, back: () -> Unit, next: () -> Unit, seek: (Long) -> Unit, previousTrack: () -> Unit, nextTrack: () -> Unit, speed: Float, openEqualizer: () -> Unit, openSleepTimer: () -> Unit, openBookmarks: () -> Unit, openSpeed: () -> Unit) {
    var volume by remember { mutableFloatStateOf(.42f) }
    Surface(color = Color(0xFF202020), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) { actions.forEach { action -> Text(action.symbol, color = Muted, fontSize = 22.sp, modifier = Modifier.clickable { when(action) { PlayerAction.Equalizer -> openEqualizer(); PlayerAction.SleepTimer -> openSleepTimer(); PlayerAction.Bookmarks -> openBookmarks(); PlayerAction.Next -> nextTrack(); PlayerAction.Previous -> previousTrack(); PlayerAction.Seek -> next(); else -> Unit } }.padding(6.dp)) } }
            Box(Modifier.padding(top = 10.dp).fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(Color(0xFF393939)), contentAlignment = Alignment.Center) { Text("♫", color = Color(0xFF292929), fontSize = 150.sp) }
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) { Text(track.title, color = White, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)); CircleButton("•••") }
            val progress = if (duration > 0) position.toFloat() / duration else 0f
            Slider(value = progress.coerceIn(0f, 1f), onValueChange = { seek((duration * it).roundToLong()) }, colors = SliderDefaults.colors(thumbColor = White, activeTrackColor = White, inactiveTrackColor = Muted))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(time(position), color = Muted); Text("M4A · 256 kbps · 48000", color = Muted); Text("-${time((duration-position).coerceAtLeast(0))}", color = Muted) }
            Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) { Text("↻", color = Muted, fontSize = 26.sp); Text("◀◀", color = White, fontSize = 30.sp, modifier = Modifier.clickable(onClick = previousTrack)); Text("↶\n15", color = White, textAlign = TextAlign.Center, modifier = Modifier.clickable(onClick = back)); Text(if (playing) "Ⅱ" else "▶", color = White, fontSize = 52.sp, modifier = Modifier.clickable(onClick = onToggle)); Text("↷\n15", color = White, textAlign = TextAlign.Center, modifier = Modifier.clickable(onClick = next)); Text("▶▶", color = White, fontSize = 30.sp, modifier = Modifier.clickable(onClick = nextTrack)) }
            Slider(value = volume, onValueChange = { volume = it }, colors = SliderDefaults.colors(thumbColor = White, activeTrackColor = White, inactiveTrackColor = Color(0xFF343434)))
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) { Text("◖", color = Muted, fontSize = 27.sp); Text(String.format(Locale.US, "%gx", speed), color = White, fontSize = 20.sp, modifier = Modifier.clickable(onClick = openSpeed).padding(8.dp)); Text("☷", color = Muted, fontSize = 27.sp); Text("⌄", color = White, fontSize = 35.sp, modifier = Modifier.clickable(onClick = onClose)) }
        }
    }
}

private fun time(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%02d:%02d".format(s / 60, s % 60) }

@Composable private fun PersonalizationScreen(enabled: Set<PlayerAction>, setEnabled: (PlayerAction, Boolean) -> Unit, close: () -> Unit) {
    Surface(color = Black, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Orange, fontSize = 40.sp, modifier = Modifier.clickable(onClick = close).padding(8.dp))
                Text("Персонализация", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            PlayerAction.entries.forEach { action ->
                Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(action.title, color = White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Switch(checked = action in enabled, onCheckedChange = { setEnabled(action, it) }, colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = Orange))
                }
                HorizontalDivider(color = Line)
            }
        }
    }
}

@Composable private fun EqualizerScreen(state: EqualizerState, enable: (Boolean) -> Unit, setBand: (Int, Short) -> Unit, setPreamp: (Float) -> Unit, close: () -> Unit) {
    val frequencies = listOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    Surface(color = Black, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Orange, fontSize = 40.sp, modifier = Modifier.clickable(onClick = close).padding(8.dp))
                Text("Эквалайзер", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(state.isEnabled, enable, colors = SwitchDefaults.colors(checkedTrackColor = Orange))
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                frequencies.forEachIndexed { index, label ->
                    val millibels = state.bandLevels.getOrElse(index) { 0 }
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(String.format(Locale.US, "%+.1f", millibels / 100f), color = White, fontSize = 11.sp)
                        Slider(value = millibels / 100f, onValueChange = { setBand(index, (it * 100).toInt().toShort()) }, valueRange = -12f..12f,
                            modifier = Modifier.width(210.dp).graphicsLayer { rotationZ = -90f }, colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Orange))
                        Text(label, color = Muted, fontSize = 12.sp)
                    }
                }
            }
            Text("Предусилитель", color = White, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { setPreamp(state.preampDb - .5f) }) { Text("−", color = Orange, fontSize = 28.sp) }
                Slider(state.preampDb, setPreamp, valueRange = -12f..12f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Orange))
                TextButton(onClick = { setPreamp(state.preampDb + .5f) }) { Text("+", color = Orange, fontSize = 28.sp) }
            }
            Text(String.format(Locale.US, "%+.1f dB", state.preampDb), color = White, fontSize = 18.sp)
        }
    }
}

@Composable private fun SleepTimerScreen(state: SleepTimerState, start: (Long) -> Unit, endOfTrack: () -> Unit, cancel: () -> Unit, close: () -> Unit) {
    var minutes by remember { mutableIntStateOf(30) }
    var custom by remember { mutableStateOf(false) }
    Surface(color = Black, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("‹", color = Orange, fontSize = 40.sp, modifier = Modifier.clickable(onClick = close)); Text("Таймер сна", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)) }
            if (state.remainingMs > 0) Text("Осталось ${time(state.remainingMs)}", color = Orange, fontSize = 22.sp, modifier = Modifier.padding(20.dp))
            listOf(15, 30, 45, 60, 90).forEach { preset ->
                Row(Modifier.fillMaxWidth().clickable { minutes = preset; custom = false }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(minutes == preset && !custom, { minutes = preset; custom = false }); Text("$preset минут", color = White, fontSize = 19.sp)
                }
            }
            Row(Modifier.fillMaxWidth().clickable { custom = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(custom, { custom = true }); Text("Пользовательское значение", color = White, fontSize = 19.sp) }
            if (custom) Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { minutes = (minutes - 1).coerceAtLeast(1) }) { Text("−", color = Orange, fontSize = 28.sp) }
                OutlinedTextField(minutes.toString(), { minutes = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 1440) ?: 1 }, label = { Text("Минуты") }, singleLine = true, modifier = Modifier.width(130.dp))
                IconButton(onClick = { minutes = (minutes + 1).coerceAtMost(1440) }) { Text("+", color = Orange, fontSize = 28.sp) }
            }
            TextButton(onClick = endOfTrack) { Text("В конце текущего трека", color = Orange) }
            Spacer(Modifier.weight(1f))
            Surface(color = Orange, shape = CircleShape, modifier = Modifier.size(100.dp).clickable { start(minutes * 60_000L); close() }) { Box(contentAlignment = Alignment.Center) { Text("Старт", color = Black, fontWeight = FontWeight.Bold, fontSize = 20.sp) } }
            TextButton(onClick = { cancel(); close() }) { Text("Отключить таймер", color = Muted) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PlaybackSpeedSheet(selected: Float, setSpeed: (Float) -> Unit, close: () -> Unit) {
    val speeds = listOf(.25f, .5f, .75f, 1f, 1.25f, 1.5f, 2f)
    ModalBottomSheet(onDismissRequest = close, containerColor = Panel) {
        Text("Скорость воспроизведения", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))
        speeds.forEach { speed ->
            val title = if (speed == 1f) "Нормальная скорость" else speed.toString()
            Row(Modifier.fillMaxWidth().clickable { setSpeed(speed); close() }.padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == speed, { setSpeed(speed); close() }); Text(title, color = White, fontSize = 19.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable private fun TrackContextMenu(track: AudioTrack, currentPosition: Long, library: LibraryViewModel, player: PlayerViewModel, dismiss: () -> Unit) {
    val context = LocalContext.current
    var edit by remember { mutableStateOf(false) }
    var bookmarkEditor by remember { mutableStateOf(false) }
    val actions = listOf("Воспроизвести далее", "Воспроизвести позже", "Добавить в плейлист", "Добавить в избранное", "Скачать", "Редактировать теги", "Открыть в", "Показать в папке", "Удалить из облачного хранилища", "Удалить из библиотеки")
    AlertDialog(onDismissRequest = dismiss, title = { Text(track.title) }, text = {
        LazyColumn { items(actions) { action -> Text(action, modifier = Modifier.fillMaxWidth().clickable {
            when (action) {
                "Воспроизвести далее" -> player.playNext(track)
                "Воспроизвести позже" -> player.playLater(track)
                "Добавить в плейлист" -> library.addTrackToFirstPlaylist(track)
                "Добавить в избранное" -> bookmarkEditor = true
                "Скачать" -> library.cacheOffline(track)
                "Редактировать теги" -> edit = true
                "Открыть в" -> context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "audio/*"; putExtra(Intent.EXTRA_STREAM, track.uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Открыть в"))
                "Показать в папке" -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, track.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                "Удалить из облачного хранилища" -> library.deleteFromCloud(track)
                "Удалить из библиотеки" -> library.removeFromLibrary(track)
            }
            if (action != "Редактировать теги" && action != "Добавить в избранное") dismiss()
        }.padding(vertical = 12.dp), color = White) } }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = dismiss) { Text("Отмена") } })
    if (edit) TagEditor(track, { title, artist, album -> library.updateTags(track, title, artist, album); dismiss() }) { edit = false }
    if (bookmarkEditor) BookmarkEditor(track, currentPosition, { title, position -> library.addBookmark(track, title, position); dismiss() }) { bookmarkEditor = false }
}

@Composable private fun TagEditor(track: AudioTrack, save: (String, String, String) -> Unit, dismiss: () -> Unit) {
    var title by remember { mutableStateOf(track.title) }; var artist by remember { mutableStateOf(track.artist) }; var album by remember { mutableStateOf(track.album) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Редактировать теги") }, text = { Column { OutlinedTextField(title, { title = it }, label = { Text("Название") }); OutlinedTextField(artist, { artist = it }, label = { Text("Исполнитель") }); OutlinedTextField(album, { album = it }, label = { Text("Альбом") }) } }, confirmButton = { TextButton(onClick = { save(title, artist, album) }) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Отмена") } })
}

@Composable private fun BookmarkEditor(track: AudioTrack, initialPosition: Long, save: (String, Long) -> Unit, dismiss: () -> Unit) {
    var title by remember { mutableStateOf(track.title) }
    var hours by remember { mutableIntStateOf((initialPosition / 3_600_000).toInt()) }
    var minutes by remember { mutableIntStateOf(((initialPosition / 60_000) % 60).toInt()) }
    var seconds by remember { mutableIntStateOf(((initialPosition / 1_000) % 60).toInt()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Новая закладка") }, text = { Column {
        OutlinedTextField(title, { title = it }, label = { Text("Заголовок") }, singleLine = true)
        Spacer(Modifier.height(16.dp)); Text("Точное время", color = White)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TimeWheel("часы", hours, 0..23) { hours = it }; TimeWheel("минуты", minutes, 0..59) { minutes = it }; TimeWheel("секунды", seconds, 0..59) { seconds = it }
        }
    } }, confirmButton = { TextButton(onClick = { save(title, (hours * 3_600_000L) + (minutes * 60_000L) + seconds * 1_000L) }) { Text("Сохранить") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Отмена") } })
}

@Composable private fun TimeWheel(label: String, value: Int, range: IntRange, set: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(factory = { context -> android.widget.NumberPicker(context).apply {
            minValue = range.first; maxValue = range.last; wrapSelectorWheel = true
            setFormatter { "%02d".format(it) }
            setOnValueChangedListener { _, _, newValue -> set(newValue) }
        } }, update = { picker -> if (picker.value != value) picker.value = value }, modifier = Modifier.width(82.dp).height(130.dp))
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable private fun BookmarksScreen(bookmarks: List<Bookmark>, currentTrack: AudioTrack?, select: (Bookmark) -> Unit, close: () -> Unit) {
    var currentOnly by remember { mutableStateOf(true) }
    val visible = if (currentOnly) bookmarks.filter { it.trackId == currentTrack?.id } else bookmarks
    Surface(color = Black, modifier = Modifier.fillMaxSize()) { Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("‹", color = Orange, fontSize = 40.sp, modifier = Modifier.clickable(onClick = close)); Text("Закладки", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
        Row(Modifier.padding(horizontal = 18.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Panel)) {
            listOf(true to "Текущая песня", false to "Все песни").forEach { (tab, title) -> Text(title, color = if (currentOnly == tab) Black else White, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(if (currentOnly == tab) Orange else Color.Transparent).clickable { currentOnly = tab }.padding(12.dp)) }
        }
        if (visible.isEmpty()) EmptyState(if (currentOnly) "Для текущей песни нет закладок" else "Закладок пока нет", "")
        else LazyColumn(Modifier.padding(top = 12.dp)) { items(visible, key = { it.id }) { bookmark ->
            Row(Modifier.fillMaxWidth().clickable { select(bookmark) }.padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(bookmark.title, color = White, fontSize = 19.sp); if (!currentOnly) Text(bookmark.trackTitle, color = Muted, fontSize = 14.sp) }
                Text(time(bookmark.positionMs), color = Orange, fontSize = 17.sp)
            }; HorizontalDivider(color = Line)
        } }
    } }
}

@Composable private fun FilesScreen() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("local_files", android.content.Context.MODE_PRIVATE) }
    var recent by remember { mutableStateOf(preferences.getStringSet("recent", emptySet()).orEmpty().toList()) }
    var connected by remember { mutableStateOf(preferences.getStringSet("folders", emptySet()).orEmpty().toList()) }
    fun rememberRecent(uris: List<Uri>) { recent = (uris.map(Uri::toString) + recent).distinct().take(20); preferences.edit().putStringSet("recent", recent.toSet()).apply() }
    val filesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { rememberRecent(it) }
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { rememberRecent(listOf(it)) } }
    val connectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let {
        runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        connected = (connected + it.toString()).distinct(); preferences.edit().putStringSet("folders", connected.toSet()).apply()
    } }
    val offline = remember { File(context.filesDir, "offline").listFiles()?.toList().orEmpty() }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 175.dp)) {
        Header("Локальные файлы", "↕", "?")
        LazyColumn {
            item { SectionTitle("БЫСТРЫЙ ДОСТУП") }
            item { SettingsRow("Избранное", "♥") }
            item { SettingsRow("Недавние (${recent.size})", "◷") }
            if (recent.isNotEmpty()) items(recent.take(5)) { uri -> FileAccessRow(Uri.parse(uri).lastPathSegment ?: uri) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } } }
            item { SectionTitle("ЛОКАЛЬНЫЕ ФАЙЛЫ") }
            item { SettingsRow("Загрузки", "↓") { filesLauncher.launch(arrayOf("audio/*", "application/octet-stream")) } }
            item { SettingsRow("Офлайн-папки (${offline.size})", "↻") }
            items(offline) { file -> FileAccessRow(file.name) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.fromFile(file))) } } }
            item { SettingsRow("Открыть файлы", "＋") { filesLauncher.launch(arrayOf("*/*")) } }
            item { SettingsRow("Открыть папки", "□") { folderLauncher.launch(null) } }
            item { SettingsRow("Подключить папку", "⊕") { connectLauncher.launch(null) } }
            if (connected.isNotEmpty()) { item { SectionTitle("ПОДКЛЮЧЁННЫЕ ПАПКИ") }; items(connected) { uri -> FileAccessRow(Uri.parse(uri).lastPathSegment ?: uri) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } } } }
        }
    }
}

@Composable private fun FileAccessRow(title: String, open: () -> Unit) { Text(title, color = White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().clickable(onClick = open).padding(start = 72.dp, top = 12.dp, bottom = 12.dp, end = 20.dp)) }
@Composable private fun SectionTitle(title: String) { Text(title, color = Muted, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) }

@Composable private fun PlaylistsScreen(playlists: List<com.hiresplayer.core.model.Playlist>) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 175.dp)) {
        Header("Плейлисты"); ActionRow()
        LazyColumn { items(playlists, key = { it.id }) { playlist ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF373737)), contentAlignment = Alignment.Center) { Text("≡♫", color = Color(0xFF292929), fontSize = 30.sp) }
                Column(Modifier.padding(start = 16.dp)) {
                    Text(playlist.name, color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${playlist.trackCount} треков · ${durationText(playlist.totalDurationMs)}", color = Muted, modifier = Modifier.padding(top = 4.dp))
                }
            }; HorizontalDivider(color = Line, modifier = Modifier.padding(start = 114.dp))
        } }
    }
}

private fun durationText(ms: Long): String {
    val totalMinutes = (ms / 60_000).coerceAtLeast(0)
    val hours = totalMinutes / 60; val minutes = totalMinutes % 60
    return if (hours > 0) "$hours ч $minutes мин" else "$minutes мин"
}

@Composable private fun SettingsScreen(onPersonalization: () -> Unit, onPassword: () -> Unit, onFiles: () -> Unit) {
    var legal by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = 175.dp)) {
        Header("Настройки", "", "?")
        LazyColumn {
            item { SectionTitle("НАСТРОЙКИ ПРИЛОЖЕНИЯ") }
            item { SettingsRow("Аудиоплеер", "▶") }; item { SettingsRow("Библиотека", "▤") }
            item { SettingsRow("Пароль", "●", onPassword) }; item { SettingsRow("Файловый менеджер", "□", onFiles) }
            item { SettingsRow("Редактор аудио тегов", "◇") }; item { SettingsRow("Персонализация", "◉", onPersonalization) }
            item { SectionTitle("ЮРИДИЧЕСКИЕ ВОПРОСЫ") }
            item { SettingsRow("Условия использования", "§") { legal = "Условия использования" } }
            item { SettingsRow("Политика конфиденциальности", "ⓘ") { legal = "Политика конфиденциальности" } }
        }
    }
    legal?.let { title -> AlertDialog(onDismissRequest = { legal = null }, title = { Text(title) }, text = { Text(if (title.startsWith("Условия")) "Используя приложение, вы соглашаетесь соблюдать применимые правила и права владельцев аудиоматериалов." else "Приложение хранит библиотеку, настройки и учётные данные локально. Доступ к файлам предоставляется только выбранным пользователем источникам.") }, confirmButton = { TextButton(onClick = { legal = null }) { Text("Закрыть") } }) }
}

@Composable private fun PasswordSettingsScreen(preferences: AppLockPreferences, close: () -> Unit) {
    var pin by remember { mutableStateOf("") }; var confirmation by remember { mutableStateOf("") }; var message by remember { mutableStateOf<String?>(null) }
    Surface(color = Black, modifier = Modifier.fillMaxSize()) { Column(Modifier.statusBarsPadding().navigationBarsPadding().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("‹", color = Orange, fontSize = 40.sp, modifier = Modifier.clickable(onClick = close)); Text("Пароль", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
        Text("Установите PIN-код из 4–8 цифр. После этого приложение можно разблокировать PIN-кодом или биометрией.", color = Muted, lineHeight = 22.sp, modifier = Modifier.padding(vertical = 24.dp))
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("Новый PIN-код") }, singleLine = true)
        OutlinedTextField(confirmation, { confirmation = it.filter(Char::isDigit).take(8) }, label = { Text("Повторите PIN-код") }, singleLine = true, modifier = Modifier.padding(top = 12.dp))
        message?.let { Text(it, color = Orange, modifier = Modifier.padding(12.dp)) }
        Button(onClick = { if (pin.length !in 4..8) message = "PIN-код должен содержать 4–8 цифр" else if (pin != confirmation) message = "PIN-коды не совпадают" else { preferences.setPin(pin); close() } }, colors = ButtonDefaults.buttonColors(containerColor = Orange), modifier = Modifier.padding(top = 20.dp)) { Text("Включить защиту", color = Black) }
        if (preferences.isEnabled) TextButton(onClick = { preferences.disable(); close() }) { Text("Отключить защиту", color = Orange) }
    } }
}

@Composable private fun AppUnlockScreen(preferences: AppLockPreferences, unlocked: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }; var error by remember { mutableStateOf(false) }
    fun biometric() {
        val activity = context as? FragmentActivity ?: return
        val prompt = BiometricPrompt(activity, getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { unlocked() }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Разблокировать HiResPlayer").setSubtitle("Подтвердите личность").setNegativeButtonText("Использовать PIN-код").build())
    }
    LaunchedEffect(Unit) { biometric() }
    Surface(color = Black, modifier = Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("HiResPlayer заблокирован", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8); error = false }, label = { Text("PIN-код") }, singleLine = true, isError = error, modifier = Modifier.padding(top = 28.dp))
        Button(onClick = { if (preferences.verify(pin)) unlocked() else error = true }, colors = ButtonDefaults.buttonColors(containerColor = Orange), modifier = Modifier.padding(top = 16.dp)) { Text("Разблокировать", color = Black) }
        TextButton(onClick = ::biometric) { Text("Использовать отпечаток пальца", color = Orange) }
        if (error) Text("Неверный PIN-код", color = MaterialTheme.colorScheme.error)
    } }
}
