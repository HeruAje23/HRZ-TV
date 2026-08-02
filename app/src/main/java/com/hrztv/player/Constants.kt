package com.hrztv.player

import android.util.Base64

object Constants {
    private const val BUILT_IN_1_B64 = "aHR0cHM6Ly9pcHR2LW9yZy5naXRodWIuaW8vaXB0di9pbmRleC5tM3U="
    private const val BUILT_IN_2_B64 = "aHR0cHM6Ly9pcHR2LW9yZy5naXRodWIuaW8vaXB0di9pbmRleC5tM3U="
    
    fun getBuiltInPlaylists(): List<String> {
        return listOf(
            String(Base64.decode(BUILT_IN_1_B64, Base64.DEFAULT)),
            String(Base64.decode(BUILT_IN_2_B64, Base64.DEFAULT))
        )
    }
}