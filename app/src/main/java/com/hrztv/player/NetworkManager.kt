package com.hrztv.player

import androidx.media3.datasource.DefaultHttpDataSource
import okhttp3.Interceptor
import okhttp3.OkHttpClient

object NetworkManager {
    val USER_AGENT = "HRZ-MEDIA/${BuildConfig.VERSION_NAME}"
    
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            })
            .build()
    }
    
    fun getDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
    }
}