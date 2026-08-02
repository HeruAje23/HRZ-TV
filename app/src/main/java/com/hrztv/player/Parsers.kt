package com.hrztv.player

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class Channel(val name: String, val group: String, val url: String, val logo: String = "", val isHiddenPlaylist: Boolean = false)

object Parsers {
    suspend fun parseM3U(url: String, isHidden: Boolean = false): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        try {
            val request = Request.Builder().url(url).build()
            val response = NetworkManager.okHttpClient.newCall(request).execute()
            val content = response.body?.string() ?: return@withContext emptyList()
            val lines = content.split("\n")
            var currentName = "Unknown"
            var currentGroup = "Uncategorized"
            var currentLogo = ""
            for (line in lines) {
                val tLine = line.trim()
                if (tLine.startsWith("#EXTINF")) {
                    currentName = tLine.substringAfterLast(",", "Unknown").trim()
                    currentGroup = Regex("group-title=\"(.*?)\"").find(tLine)?.groups?.get(1)?.value ?: "Uncategorized"
                    currentLogo = Regex("tvg-logo=\"(.*?)\"").find(tLine)?.groups?.get(1)?.value ?: ""
                } else if (tLine.startsWith("http")) {
                    channels.add(Channel(currentName, currentGroup, tLine, currentLogo, isHidden))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext channels
    }

    suspend fun parseXtream(server: String, user: String, pass: String): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        try {
            val apiUrl = "$server/player_api.php?username=$user&password=$pass&action=get_live_streams"
            val request = Request.Builder().url(apiUrl).build()
            val response = NetworkManager.okHttpClient.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext emptyList()
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val streams: List<Map<String, Any>> = Gson().fromJson(json, type)
            streams.forEach { stream ->
                val streamId = stream["stream_id"].toString()
                val ext = stream["stream_type"]?.toString() ?: "ts"
                val playUrl = "$server/live/$user/$pass/$streamId.$ext"
                channels.add(Channel(stream["name"]?.toString() ?: "Unknown", stream["category_id"]?.toString() ?: "Live", playUrl, stream["stream_icon"]?.toString() ?: "", false))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext channels
    }
}