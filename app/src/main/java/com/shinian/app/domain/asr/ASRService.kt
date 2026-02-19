package com.shinian.app.domain.asr

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ASRService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val appId = context.getString(R.string.volc_asr_app_id)
    private val accessToken = context.getString(R.string.volc_asr_access_token)
    
    suspend fun transcribe(audioFile: File): String = withContext(Dispatchers.IO) {
        try {
            // 读取音频文件并 Base64 编码
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            
            // 构建请求体
            val jsonBody = JSONObject().apply {
                put("appid", appId)
                put("token", accessToken)
                put("cluster", "volcengine_input_common")
                put("speech_type", 1) // 1: pcm, 2: wav, 3: mp3, 4: m4a
                put("audio_format", "m4a")
                put("sample_rate", 16000)
                put("language", "zh-CN")
                put("enable_punctuation", true)
                put("enable_itn", true)
                put("speech", base64Audio)
            }
            
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            
            // 发送请求
            val request = Request.Builder()
                .url("https://openspeech.bytedance.com/api/v1/auc")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("ASR request failed: ${response.code}")
                }
                
                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val jsonResponse = JSONObject(responseBody)
                
                // 解析响应
                val result = jsonResponse.optJSONObject("result")
                    ?.optString("text")
                    ?: throw Exception("No text in ASR response")
                
                result
            }
        } catch (e: Exception) {
            throw Exception("ASR transcription failed: ${e.message}", e)
        }
    }
}