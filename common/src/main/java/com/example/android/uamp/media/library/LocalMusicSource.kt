package com.example.android.uamp.media.library

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.example.android.uamp.media.extensions.*
import java.util.concurrent.TimeUnit

class LocalMusicSource(context: Context) : AbstractMusicSource() {

    private var catalog: List<MediaMetadataCompat> = emptyList()
    private val glide: RequestManager
    private val _context: Context = context

    init {
        state = STATE_INITIALIZING
        glide = Glide.with(context)
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

    override suspend fun load() {
        updateCatalog(_context)?.let { updatedCatalog ->
            catalog = updatedCatalog
            state = STATE_INITIALIZED
        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    private fun updateCatalog(context: Context): List<MediaMetadataCompat>? {
        var musicList: ArrayList<MediaMetadataCompat> = arrayListOf()

        val proj: Array<String> = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION

        )
        val audioCursor: Cursor? = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null)
        if (audioCursor != null) {
            if (audioCursor.moveToFirst()) {
                do {
                    val music = LocalMusic()
                    music.id = audioCursor.getString(0)
                    music.title = audioCursor.getString(1)
                    music.artist = audioCursor.getString(2)
                    music.album = audioCursor.getString(3)
                    music.source = audioCursor.getString(4)
                    music.trackNumber = audioCursor.getString(5).toLong()
                    music.duration = audioCursor.getString(6).toLong()

                    //TODO: Conditionally map album image if exists, otherwise use default image

                    musicList.add(MediaMetadataCompat.Builder().from(music).build())

                } while (audioCursor.moveToNext())
            }
        }

        return musicList.toList()
    }

    /**
     * Extension method for [MediaMetadataCompat.Builder] to set the fields
     */
    fun MediaMetadataCompat.Builder.from(localMusic: LocalMusic): MediaMetadataCompat.Builder {
        id = localMusic.id
        title = localMusic.title
        artist = localMusic.artist
        album = localMusic.album
        duration = localMusic.duration
        genre = localMusic.genre
        mediaUri = localMusic.source
        albumArtUri = localMusic.image
        trackNumber = localMusic.trackNumber
        trackCount = localMusic.totalTrackCount
        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

        // To make things easier for *displaying* these, set the display properties as well.
        displayTitle = localMusic.title
        displaySubtitle = localMusic.artist
        displayDescription = localMusic.album
        displayIconUri = localMusic.image

        // Add downloadStatus to force the creation of an "extras" bundle in the resulting
        // MediaMetadataCompat object. This is needed to send accurate metadata to the
        // media session during updates.
        downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED

        // Allow it to be used in the typical builder style.
        return this
    }

    @Suppress("unused")
    class LocalMusic {
        var id: String = ""
        var title: String = ""
        var album: String = ""
        var artist: String = ""
        var genre: String = ""
        var source: String = ""
        var image: String = ""
        var trackNumber: Long = 0
        var totalTrackCount: Long = 0
        var duration: Long = -1
        var site: String = ""
    }
}