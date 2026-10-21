package cc.zxc.lazytranslator.services

import cc.zxc.lazytranslator.settings.TranslatorSettings
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * 调用 OpenAI 兼容的 chat/completions 接口做翻译。
 * 兼容 OpenAI / DeepSeek / Moonshot / 通义 / Ollama 等同类服务。
 *
 * 全部走 sendAsync，返回的 [CompletableFuture] 调用 cancel(true) 即可中断底层 HTTP 请求。
 */
object TranslatorService {

    private val gson = Gson()
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    /**
     * 翻译文本
     *
     * @param concise true = 变量名场景（中文 → 简短英文短语）；false = 普通翻译（英文 → 中文完整译文）
     */
    fun translateAsync(text: String, concise: Boolean): CompletableFuture<String> {
        val s = TranslatorSettings.getInstance().state
        val system = if (concise) buildConcisePrompt() else buildNormalPrompt()
        return chatAsync(system, text).thenApply { it.trim() }
    }

    /** 设置页「测试连接」用 */
    fun testConnectionAsync(): CompletableFuture<String> =
        chatAsync(null, "只回复：ok")

    /** 变量名场景：返回简短英文短语，不要句子 */
    private fun buildConcisePrompt(): String = """
        You are a professional translation engine for variable names.
        Detect the source language automatically, then translate it to English.
        Rules:
        1. Output ONLY a short English phrase or 1-3 words suitable as a variable name (e.g. "user name", "shopping cart", "is active").
        2. Use lowercase English words separated by spaces.
        3. No punctuation, no quotes, no explanations.
        4. If the source is already English, return the cleaned-up lowercase form.
        5. If the source cannot be translated, fall back to the most natural English equivalent.
    """.trimIndent()

    /** 普通场景：忠实翻译为中文 */
    private fun buildNormalPrompt(): String = """
        You are a professional translator.
        Detect the source language automatically, then translate it to Chinese faithfully.
        Output ONLY the translated text. Do not add explanations, quotes or formatting.
        Preserve code-like tokens (camelCase, snake_case, etc.) as-is when present.
    """.trimIndent()

    private fun chatAsync(system: String?, user: String): CompletableFuture<String> {
        val s = TranslatorSettings.getInstance().state
        require(s.apiKey.isNotBlank()) { "请先在 Settings → Tools → Lazy Translator 中配置 API Key" }
        require(s.baseUrl.isNotBlank()) { "请先配置 Base URL" }

        val messages = mutableListOf<Map<String, String>>()
        if (!system.isNullOrBlank()) messages += mapOf("role" to "system", "content" to system)
        messages += mapOf("role" to "user", "content" to user)

        val payload = JsonObject().apply {
            addProperty("model", s.model)
            add("messages", gson.toJsonTree(messages))
            addProperty("temperature", s.temperature)
            addProperty("stream", false)
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(s.baseUrl.trimEnd('/') + "/chat/completions"))
            .timeout(Duration.ofSeconds(s.timeoutSeconds.toLong().coerceIn(5, 300)))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${s.apiKey}")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
            .build()

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply { response ->
            if (response.statusCode() !in 200..299) {
                throw RuntimeException("AI 请求失败 (HTTP ${response.statusCode()})：${response.body().take(500)}")
            }
            gson.fromJson(response.body(), JsonObject::class.java)
                .getAsJsonArray("choices")[0].asJsonObject
                .getAsJsonObject("message")["content"].asString
        }
    }
}
