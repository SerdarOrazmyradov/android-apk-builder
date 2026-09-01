package com.example.sampleapp

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = MediaType.parse("application/json; charset=utf-8")

    fun getAiResponse(apiKey: String, model: String, inputText: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/interactions"

        val jsonBody = JSONObject().apply {
            put("model", model)
            put("input", inputText)
        }

        val body = RequestBody.create(JSON_MEDIA, jsonBody.toString())

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body()?.string() ?: ""

            if (response.isSuccessful) {
                val jsonRes = JSONObject(responseBody)
                // Interactions API response pars etmek:
                if (jsonRes.has("output")) {
                    jsonRes.getString("output")
                } else if (jsonRes.has("candidates")) {
                    jsonRes.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                } else {
                    responseBody
                }
            } else {
                "Soraagda hümmetli säwlik boldy: ${response.code()}"
            }
        } catch (e: Exception) {
            "Rabitada näsazlyk: ${e.localizedMessage}"
        }
    }
}