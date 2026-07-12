package com.hiresplayer.core.database

import android.net.Uri
import com.hiresplayer.core.database.entity.TrackEntity
import com.hiresplayer.core.database.relation.BookmarkWithTrack
import com.hiresplayer.core.database.relation.PlaylistTrackWithTrack
import com.hiresplayer.core.database.relation.PlaylistWithCount
import com.hiresplayer.core.model.AudioTrack
import com.hiresplayer.core.model.Bookmark
import com.hiresplayer.core.model.Playlist
import com.hiresplayer.core.model.PlaylistTrack
import com.hiresplayer.core.model.TrackSource

fun AudioTrack.toEntity(updatedAtMs: Long = System.currentTimeMillis()): TrackEntity =
    TrackEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = uri.toString(),
        artworkUri = artworkUri?.toString(),
        source = source.name,
        updatedAtMs = updatedAtMs
    )

fun TrackEntity.toModel(): AudioTrack =
    AudioTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = Uri.parse(uri),
        artworkUri = artworkUri?.let(Uri::parse),
        source = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.Local)
    )

fun PlaylistWithCount.toModel(): Playlist =
    Playlist(id = id, name = name, trackCount = trackCount, totalDurationMs = totalDurationMs, createdAtMs = createdAtMs)

fun PlaylistTrackWithTrack.toModel(): PlaylistTrack =
    PlaylistTrack(
        playlistId = playlistId,
        track = TrackEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = uri,
            artworkUri = artworkUri,
            source = source,
            updatedAtMs = updatedAtMs
        ).toModel(),
        sortOrder = sortOrder
    )

fun BookmarkWithTrack.toModel(): Bookmark =
    Bookmark(
        id = id,
        trackId = trackId,
        trackTitle = trackTitle,
        title = title,
        positionMs = positionMs,
        createdAtMs = createdAtMs
    )
