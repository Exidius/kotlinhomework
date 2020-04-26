package com.example.android.uamp.media.library

data class PlaylistsData(
        var playlists: MutableList<PlaylistData>
)

data class PlaylistData(
        var name: String,
        var songs: MutableList<Song>
)

data class Song(
        var route: String
)