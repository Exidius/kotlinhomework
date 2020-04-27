package com.example.android.uamp.common

import android.content.Context
import android.util.Log
import com.example.android.uamp.media.library.PlaylistsData
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class PlaylistHandler(context: Context) {
    private var json = File(context.filesDir, "playlistJson")
    val reader = BufferedReader(FileReader(json))
    val playlists = Gson().fromJson<PlaylistsData>(reader,PlaylistsData::class.java)


    // Only write fir first time starting the app
    //writeToJson(context)

    //        val reader = BufferedReader(FileReader(json))
//        val playlists = Gson().fromJson<PlaylistsData>(reader,PlaylistsData::class.java)
//        val songs = mutableListOf<Song>(Song("111"), Song("222"))
//        val newPlaylist: PlaylistData = PlaylistData("asdasd", songs)
//        playlists.playlists[1].songs.removeAt(0)
//        val jsonToWrite = Gson().toJson(playlists,PlaylistsData::class.java)
//        writeToJson(context,jsonToWrite.toString())
    private fun writeToJson(context: Context, string: String){
        val filename = "playlistJson"
        context.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(string.toByteArray())
        }

        val reader = BufferedReader(FileReader(json))
        val asd = Gson().fromJson<PlaylistsData>(reader, PlaylistsData::class.java)
        Log.i("asdasd",asd.playlists[1].songs[0].route)
    }

    private fun writeToJson(context: Context){
        val filename = "playlistJson"
        val fileContents = "{\"playlists\": [{\"name\": \"playlist-1\",\"songs\": [{\"route\": \"/storage/emulated/0/Download/09-imagine_dragons-thunder.mp3\"},{\"route\": \"route2\"}]}]}"
        context.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(fileContents.toByteArray())
        }
}
}