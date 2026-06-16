package com.github.`792532971`.ideaplugin.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiNamingService(
    private val baseUrl: String,
    private val apiKey: String,
    private val modelName: String,
    private val temperature: Double
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun generateEnglishWords(chineseDescription: String): List<String> {
        val requestBody = buildRequestBody(chineseDescription).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP code: ${response.code}")
            }
            val responseBody = response.body?.string()
                ?: throw IOException("Empty response body")
            val text = extractContent(responseBody)
            return parseWords(text)
        }
    }

    fun testConnection(): String {
        val requestBody = buildRequestBody("test").toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP code: ${response.code}")
            }
            return "Connection successful"
        }
    }

    fun parseWords(text: String): List<String> {
        return text
            .replace(Regex("[^\\w\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.lowercase() }
    }

    internal fun buildRequestBody(description: String): String {
        val json = JsonObject().apply {
            addProperty("model", modelName)
            addProperty("max_tokens", 64)
            addProperty("temperature", temperature)
            addProperty("system", "You are a coding assistant. Translate the Chinese description into 2-5 concise English words suitable for a variable name. Return ONLY the words, space-separated, lowercase. No explanation, no punctuation.")
            val message = JsonObject().apply {
                addProperty("role", "user")
                addProperty("content", description)
            }
            val messages = JsonArray().apply { add(message) }
            add("messages", messages)
        }
        return gson.toJson(json)
    }

    internal fun extractContent(responseJson: String): String {
        val root = JsonParser.parseString(responseJson).asJsonObject
        val contentArray = root.getAsJsonArray("content")
            ?: throw IllegalStateException("Missing 'content' array in response")
        if (contentArray.size() == 0) {
            throw IllegalStateException("Empty 'content' array in response")
        }
        val firstContent = contentArray[0].asJsonObject
        val text = firstContent.get("text")
            ?: throw IllegalStateException("Missing 'text' field in content[0]")
        return text.asString
    }
}
