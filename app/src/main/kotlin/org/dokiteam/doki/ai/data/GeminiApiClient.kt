package org.dokiteam.doki.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    suspend fun generateContent(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBody = JSONObject().apply {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userMessage) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", 300)
                    put("temperature", 0.2)
                })
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: error("Empty response from Gemini API")

            if (!response.isSuccessful) {
                val errorMsg = runCatching {
                    JSONObject(body).getJSONObject("error").getString("message")
                }.getOrDefault("HTTP ${response.code}")
                error("Gemini API error: $errorMsg")
            }

            val json = JSONObject(body)
            json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        }
    }
}
