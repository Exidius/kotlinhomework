/*
 * Copyright 2019 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.media.library

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.example.android.uamp.media.MusicService
import com.example.android.uamp.media.R
import com.example.android.uamp.media.extensions.*
import com.google.gson.Gson
import java.io.*
import java.util.function.UnaryOperator

/**
 * Represents a tree of media that's used by [MusicService.onLoadChildren].
 *
 * [BrowseTree] maps a media id (see: [MediaMetadataCompat.METADATA_KEY_MEDIA_ID]) to one (or
 * more) [MediaMetadataCompat] objects, which are children of that media id.
 *
 * For example, given the following conceptual tree:
 * root
 *  +-- Albums
 *  |    +-- Album_A
 *  |    |    +-- Song_1
 *  |    |    +-- Song_2
 *  ...
 *  +-- Artists
 *  ...
 *
 *  Requesting `browseTree["root"]` would return a list that included "Albums", "Artists", and
 *  any other direct children. Taking the media ID of "Albums" ("Albums" in this example),
 *  `browseTree["Albums"]` would return a single item list "Album_A", and, finally,
 *  `browseTree["Album_A"]` would return "Song_1" and "Song_2". Since those are leaf nodes,
 *  requesting `browseTree["Song_1"]` would return null (there aren't any children of it).
 */
class BrowseTree(context: Context, musicSource: MusicSource) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>()

    private var json = File(context.filesDir, "playlistJson")

    /**
     * Whether to allow clients which are unknown (non-whitelisted) to use search on this
     * [BrowseTree].
     */
    val searchableByUnknownCaller = true

    /**
     * In this example, there's a single root node (identified by the constant
     * [UAMP_BROWSABLE_ROOT]). The root's children are each album included in the
     * [MusicSource], and the children of each album are the songs on that album.
     * (See [BrowseTree.buildAlbumRoot] for more details.)
     */
    init {
        val rootList = mediaIdToChildren[UAMP_BROWSABLE_ROOT] ?: mutableListOf()

        val recommendedMetadata = MediaMetadataCompat.Builder().apply {
            id = UAMP_RECOMMENDED_ROOT
            title = context.getString(R.string.recommended_title)
            albumArtUri = RESOURCE_ROOT_URI +
                    context.resources.getResourceEntryName(R.drawable.ic_recommended)
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()

        val albumsMetadata = MediaMetadataCompat.Builder().apply {
            id = UAMP_ALBUMS_ROOT
            title = context.getString(R.string.albums_title)
            albumArtUri = RESOURCE_ROOT_URI +
              context.resources.getResourceEntryName(R.drawable.ic_album)
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()

        val artistsMetadata = MediaMetadataCompat.Builder().apply {
            id = UAMP_ARTISTS_ROOT
            title = context.getString(R.string.artists_title)
            albumArtUri = RESOURCE_ROOT_URI +
                    context.resources.getResourceEntryName(R.drawable.ic_album)
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()

        val playlistsMetadata = MediaMetadataCompat.Builder().apply {
            id = UAMP_PLAYLISTS_ROOT
            title = context.getString(R.string.playlists_title)
            albumArtUri = RESOURCE_ROOT_URI +
                    context.resources.getResourceEntryName(R.drawable.ic_album)
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()

        rootList += recommendedMetadata
        rootList += albumsMetadata
        rootList += artistsMetadata
        rootList += playlistsMetadata
        mediaIdToChildren[UAMP_BROWSABLE_ROOT] = rootList
        mediaIdToChildren[UAMP_PLAYLISTS_ROOT] = buildPlaylistsRoot(musicSource)

        musicSource.forEach { mediaItem ->
            val albumMediaId = mediaItem.album.urlEncoded
            val albumChildren = mediaIdToChildren[albumMediaId] ?: buildAlbumRoot(mediaItem)
            albumChildren += mediaItem
            val artistMediaId = mediaItem.artist.urlEncoded
            val artistChildren = mediaIdToChildren[artistMediaId] ?: buildArtistRoot(mediaItem)
            artistChildren += mediaItem

            val reader = BufferedReader(FileReader(json))
            val playlists = Gson().fromJson<PlaylistsData>(reader,PlaylistsData::class.java)
            for (playlist in playlists.playlists) {
                if(mediaIdToChildren[playlist.name] == null) {
                    mediaIdToChildren[playlist.name] = mutableListOf<MediaMetadataCompat>()
                }
                for (song in playlist.songs) {
                    if (mediaItem.mediaUri.toString() == song.route) {
                        mediaIdToChildren[playlist.name]?.plusAssign(mediaItem)
                    }
                }
            }

            // Add the first track of each album to the 'Recommended' category
            if (mediaItem.trackNumber == 1L){
                val recommendedChildren = mediaIdToChildren[UAMP_RECOMMENDED_ROOT]
                                        ?: mutableListOf()
                recommendedChildren += mediaItem
                mediaIdToChildren[UAMP_RECOMMENDED_ROOT] = recommendedChildren
            }
        }
    }

    /**
     * Provide access to the list of children with the `get` operator.
     * i.e.: `browseTree\[UAMP_BROWSABLE_ROOT\]`
     */
    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    /**
     * Builds a node, under the root, that represents an album, given
     * a [MediaMetadataCompat] object that's one of the songs on that album,
     * marking the item as [MediaItem.FLAG_BROWSABLE], since it will have child
     * node(s) AKA at least 1 song.
     */
    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat) : MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = mediaItem.album.urlEncoded
            title = mediaItem.album
            artist = mediaItem.artist
            albumArt = mediaItem.albumArt
            albumArtUri = mediaItem.albumArtUri.toString()
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        // Adds this album to the 'Albums' category.
        val rootList = mediaIdToChildren[UAMP_ALBUMS_ROOT] ?: mutableListOf()
        rootList += albumMetadata
        mediaIdToChildren[UAMP_ALBUMS_ROOT] = rootList

        // Insert the album's root with an empty list for its children, and return the list.
        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[albumMetadata.id] = it
        }
    }

    private fun buildArtistRoot(mediaItem: MediaMetadataCompat) : MutableList<MediaMetadataCompat> {
        val artistsMetadata = MediaMetadataCompat.Builder().apply {
            id = mediaItem.artist.urlEncoded
            title = mediaItem.artist
            albumArt = mediaItem.albumArt
            albumArtUri = mediaItem.albumArtUri.toString()
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        // Adds this artist to the 'Artists' category.
        val rootList = mediaIdToChildren[UAMP_ARTISTS_ROOT] ?: mutableListOf()
        rootList += artistsMetadata
        mediaIdToChildren[UAMP_ARTISTS_ROOT] = rootList

        // Insert the artists's root with an empty list for its children, and return the list.
        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[artistsMetadata.id] = it
        }
    }

    private fun buildPlaylistsRoot(musicSource: MusicSource) : MutableList<MediaMetadataCompat> {
        val rootList = mediaIdToChildren[UAMP_PLAYLISTS_ROOT] ?: mutableListOf()

        val reader = BufferedReader(FileReader(json))
        val playlists = Gson().fromJson<PlaylistsData>(reader,PlaylistsData::class.java)

        for (playlist in playlists.playlists) {
            val playlistMetadata = MediaMetadataCompat.Builder().apply {
                id = playlist.name
                title = playlist.name
                flag = MediaItem.FLAG_BROWSABLE
            }.build()
            rootList += playlistMetadata
        }
        mediaIdToChildren[UAMP_PLAYLISTS_ROOT] = rootList
        return rootList
    }
}


const val UAMP_BROWSABLE_ROOT = "/"
const val UAMP_EMPTY_ROOT = "@empty@"
const val UAMP_RECOMMENDED_ROOT = "__RECOMMENDED__"
const val UAMP_ALBUMS_ROOT = "__ALBUMS__"
const val UAMP_ARTISTS_ROOT = "__ARTISTS__"
const val UAMP_PLAYLISTS_ROOT = "__PLAYLISTS__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_ROOT_URI = "android.resource://com.example.android.uamp.next/drawable/"
