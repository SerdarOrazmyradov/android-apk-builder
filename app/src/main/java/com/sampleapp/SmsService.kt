package com.service

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit

// Netijäni yza gaýtarmak üçin Callback interfeýsi
interface GeminiCallback {
    fun onSuccess(response: String)
    fun onError(errorMessage: String)
}

class SmsService(private val context: Any) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
        
    fun getUserApiKey(phoneNumber: String): String? { // implement this function to retrieve the apiKey from the database based on the phone number 
        // For demonstration purposes, returning null to use the default API key
        return null
    } 

    fun forwardSmsToGeminiAi(sender: String, message: String, callback: GeminiCallback) {
        try {
            val model = "gemini-3.5-flash"
            val apiKey = getUserApiKey(sender) ?: "SECRET_API_KEY" 
            val serverUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val jsonPayload = JSONObject().apply {
                put("contents", listOf(
                    mapOf("parts" to listOf(mapOf("text" to message)))
                ))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(serverUrl)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback.onError(e.message ?: "Näbelli ýalňyşlyk emele geldi")
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                            val json = JSONObject(responseBody)
                            val aiText = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")

                            callback.onSuccess(aiText)
                        } else {
                            callback.onError("API Ýalňyşlygy (Code: ${response.code}): $responseBody")
                        }
                    } catch (e: JSONException) {
                        callback.onError("JSON Pars etme ýalňyşlygy: ${e.message}")
                    } catch (e: Exception) {
                        callback.onError("Ýalňyşlyk: ${e.message}")
                    }
                }
            })
        } catch (e: Exception) {
            callback.onError("Haýyş ýalňyşlygy: ${e.message}")
        }
    }
    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
            Log.d("SmsService", "SMS üstünlikli ugradyldy: $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e("SmsService", "SMS ugratmak başartmady: ${e.localizedMessage}")
            false
        }
    }
}