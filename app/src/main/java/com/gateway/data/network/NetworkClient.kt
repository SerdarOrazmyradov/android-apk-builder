package com.gateway.data.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {
    private var tlsClient: OkHttpClient? = null

    fun getTls12Client(): OkHttpClient {
        if (tlsClient == null) {
            tlsClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        }
        return tlsClient!!
    }
}
