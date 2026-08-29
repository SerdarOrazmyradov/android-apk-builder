package com.gateway.data.network

import android.content.Context
import com.google.gson.Gson

data class ModelConfig(
    val id: String,
    val displayName: String,
    val description: String,
    val status: String
)

data class ModelsData(
    val models: List<ModelConfig>,
    val defaultModel: String
)

class ModelManager(private val context: Context) {
    private lateinit var modelsData: ModelsData
    private val gson = Gson()

    init {
        loadModels()
    }

    private fun loadModels() {
        try {
            val inputStream = context.resources.openRawResource(com.example.sampleapp.R.raw.models)
            val json = inputStream.bufferedReader().use { it.readText() }
            modelsData = gson.fromJson(json, ModelsData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to default model
            modelsData = ModelsData(
                models = listOf(
                    ModelConfig(
                        "gemini-3.5-flash",
                        "Gemini 3.5 Flash",
                        "Latest stable model",
                        "stable"
                    )
                ),
                defaultModel = "gemini-3.5-flash"
            )
        }
    }

    fun getDefaultModel(): String = modelsData.defaultModel

    fun getLatestModel(): String = modelsData.models.firstOrNull()?.id ?: modelsData.defaultModel

    fun getAllModels(): List<ModelConfig> = modelsData.models

    fun getModelById(id: String): ModelConfig? = modelsData.models.find { it.id == id }

    fun getStableModels(): List<ModelConfig> = modelsData.models.filter { it.status == "stable" }
}
