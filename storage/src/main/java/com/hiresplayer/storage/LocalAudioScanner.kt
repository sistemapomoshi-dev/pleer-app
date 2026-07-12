package com.hiresplayer.storage

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.hiresplayer.core.model.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAudioScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun scan(): List<AudioTrack> = withContext(Dispatchers.IO) {
        // Читаем аудиотеку через MediaStore, чтобы не работать напрямую с путями файлов.
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(collection, projection, selection, null, sort)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection, id)
                    add(
                        AudioTrack(
                            id = id.toString(),
                            title = cursor.getString(titleColumn).orEmpty().ifBlank { "Без названия" },
                            artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Неизвестный исполнитель" },
                            album = cursor.getString(albumColumn).orEmpty().ifBlank { "Неизвестный альбом" },
                            durationMs = cursor.getLong(durationColumn),
                            uri = uri
                        )
                    )
                }
            }
        } ?: emptyList()
    }
}
