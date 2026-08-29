package com.gateway.data.network

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class GeminiRepository {
    private val client: OkHttpClient = NetworkClient.getTls12Client()

    interface ApiCallback {
        fun onSuccess(responseText: String)
        fun onError(error: String)
    }

    fun askGemini(apiKey: String, prompt: String, callback: ApiCallback) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

            // System Instruction: Keep response brief for SMS (160 chars max)
            val systemInstruction = "Answer briefly, under 160 characters, no markdown styling."
            val fullPrompt = "$systemInstruction User query: $prompt"

            val textPart = JSONObject().put("text", fullPrompt)
            val partsObj = JSONObject().put("parts", JSONArray().put(textPart))
            val contentsObj = JSONObject().put("contents", JSONArray().put(partsObj))

            val body = contentsObj.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e.message ?: "Unknown error")
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful && response.body != null) {
                            val json = JSONObject(response.body!!.string())
                            val aiText = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                            callback.onSuccess(aiText)
                        } else {
                            callback.onError("API Error Code: ${response.code}")
                        }
                    } catch (e: JSONException) {
                        callback.onError("JSON Parse Error: ${e.message}")
                    } catch (e: Exception) {
                        callback.onError("Error: ${e.message}")
                    }
                }
            })
        } catch (e: Exception) {
            callback.onError("Request Error: ${e.message}")
        }
    }
}
