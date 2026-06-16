package com.github.`792532971`.ideaplugin.services

import com.google.gson.JsonObject
import com.google.gson.JsonArray
import org.junit.Assert.*
import org.junit.Test

class AiNamingServiceTest {

    @Test
    fun `parseWords extracts space-separated words`() {
        val service = AiNamingService("http://localhost", "key", "model", 0.2)
        assertEquals(listOf("hello", "world"), service.parseWords("hello world"))
    }

    @Test
    fun `parseWords cleans punctuation and extra whitespace`() {
        val service = AiNamingService("http://localhost", "key", "model", 0.2)
        assertEquals(listOf("hello", "world"), service.parseWords("  hello,  world!  "))
    }

    @Test
    fun `parseWords filters empty tokens`() {
        val service = AiNamingService("http://localhost", "key", "model", 0.2)
        assertEquals(listOf("a", "b"), service.parseWords("a  b"))
    }

    @Test
    fun `extractContent returns text from a sample Claude response JSON`() {
        val service = AiNamingService("http://localhost", "key", "model", 0.2)
        val json = """
            {
              "content": [
                { "type": "text", "text": "hello world" }
              ]
            }
        """.trimIndent()
        assertEquals("hello world", service.extractContent(json))
    }

    @Test
    fun `buildRequestBody produces valid JSON containing expected fields`() {
        val service = AiNamingService("http://localhost", "key", "model", 0.2)
        val body = service.buildRequestBody("中文描述")
        val json = com.google.gson.JsonParser.parseString(body).asJsonObject

        assertEquals("model", json.get("model").asString)
        assertEquals(64, json.get("max_tokens").asInt)
        assertEquals(0.2, json.get("temperature").asDouble, 0.0001)
        assertTrue(json.has("system"))
        assertTrue(json.has("messages"))

        val messages = json.getAsJsonArray("messages")
        assertEquals(1, messages.size())
        val msg = messages[0].asJsonObject
        assertEquals("user", msg.get("role").asString)
        assertEquals("中文描述", msg.get("content").asString)
    }
}
