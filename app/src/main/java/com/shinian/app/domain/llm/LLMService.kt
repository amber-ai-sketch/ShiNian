package com.shinian.app.domain.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = BuildConfig.ARK_API_KEY // 从 BuildConfig 读取


    suspend fun structure(transcript: String, type: NoteType): StructuredNote = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = when (type) {
                NoteType.MEETING -> "你是一个专业的会议纪要整理助手..." // (省略完整prompt，与PRD一致)
                NoteType.IDEA -> "你是一个灵感提炼专家..."
                NoteType.TODO -> "你是一个待办事项提取助手..."
            }


            val jsonBody = JSONObject().apply {
                put("model", "kimi-k2-thinking-251104")
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "语音转写内容是：{$transcript}")
                    })
                })
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://ark.cn-beijing.volces.com/api/v3/chat/completions")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("LLM request failed: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val jsonResponse = JSONObject(responseBody)
                val content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                // Parse the JSON content from LLM
                val llmOutput = JSONObject(content)
                StructuredNote(
                    type = type,
                    title = when (type) {
                        NoteType.MEETING -> llmOutput.optString("title", "会议记录")
                        NoteType.IDEA -> "灵感"
                        NoteType.TODO -> "待办事项"
                    },
                    content = content, // Raw JSON for frontend parsing
                    summary = when (type) {
                        NoteType.MEETING -> llmOutput.optJSONArray("summary")?.let { arr ->
                            List(arr.length()) { arr.getString(it) }
                        } ?: emptyList()
                        else -> emptyList()
                    },
                    todos = when (type) {
                        NoteType.MEETING -> llmOutput.optJSONArray("todos")?.let { arr ->
                            List(arr.length()) { arr.getString(it) }
                        } ?: emptyList()
                        NoteType.TODO -> llmOutput.optJSONArray("todos")?.let { arr ->
                            List(arr.length()) { arr.getString(it) }
                        } ?: emptyList()
                        else -> emptyList()
                    },
                    quote = when (type) {
                        NoteType.IDEA -> llmOutput.optString("quote", "")
                        else -> null
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("LLMService", "Failed to structure note", e)
            // Fallback: create a basic note with raw transcript
            StructuredNote(
                type = type,
                title = when (type) {
                    NoteType.MEETING -> "会议记录 (处理失败)"
                    NoteType.IDEA -> "灵感笔记 (处理失败)"
                    NoteType.TODO -> "待办事项 (处理失败)"
                },
                content = "{\"error\":\"${e.message}\",\"raw\":\"$transcript\"}",
                summary = if (type == NoteType.MEETING) listOf("处理时出错: ${e.message}") else emptyList(),
                todos = if (type == NoteType.TODO) listOf("请检查原始录音") else emptyList(),
                quote = if (type == NoteType.IDEA) "处理失败: ${e.message}" else null
            )
        }
    }
}

data class StructuredNote(
    val type: NoteType,
    val title: String,
    val content: String, // JSON string
    val summary: List<String> = emptyList(),
    val todos: List<String> = emptyList(),
    val quote: String? = null
)