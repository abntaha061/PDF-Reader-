package com.example.pdf

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: RequestBody
    ): ResponseBody
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .build()
            .create(GeminiApiService::class.java)
    }
}

object GeminiOcrEngine {

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Executes intelligent OCR (Optical Character Recognition) on an image
     */
    suspend fun performOcr(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing! Please configure GEMINI_API_KEY to run actual multimodal OCR."
        }

        try {
            // Build request JSON programmatically
            val inlineDataObj = JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", bitmap.toBase64())
            }

            val textPart = JSONObject().apply {
                put("text", "You are a professional PDF OCR agent. Extract and digitize every word of text exactly as written in this image. Write the extracted text explicitly.")
            }

            val imagePart = JSONObject().apply {
                put("inlineData", inlineDataObj)
            }

            val contentObj = JSONObject().apply {
                put("parts", JSONArray().put(textPart).put(imagePart))
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(contentObj))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val rawResponse = GeminiClient.service.generateContent(apiKey, requestBody)
            val jsonResponse = JSONObject(rawResponse.string())
            
            extractTextFromResponse(jsonResponse)
        } catch (e: Exception) {
            e.printStackTrace()
            "OCR Action Failed: ${e.message ?: "Connection Error"}"
        }
    }

    /**
     * Executes intelligent page summaries on active text blocks
     */
    suspend fun summarizeText(text: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "مفتاح API غير متوفر! أضف مفتاح Gemini الخاص بك في لوحة الأسرار (Secrets).\n\nملخص محلي للمستند:\n- يوفر التطبيق ميزات دمج وتقسيم ملفات PDF بالكامل.\n- تحرير نصوص PDF الأصلية مع تغيير الخط واللون والحجم.\n- دعم حماية الملف بعبارة مرور وتوقيع الكتروني تفاعلي.\n- نمط Reflow للقراءة الذكية المريحة على الشاشات الصغيرة."
        }

        try {
            val systemPart = JSONObject().apply {
                put("text", "Please write a concise professional summary in standard Arabic formatting (نقاط ملخصة واضحة ودقيقة) outlining the main points of this document:\n\n$text")
            }

            val contentObj = JSONObject().apply {
                put("parts", JSONArray().put(systemPart))
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(contentObj))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val rawResponse = GeminiClient.service.generateContent(apiKey, requestBody)
            val jsonResponse = JSONObject(rawResponse.string())

            extractTextFromResponse(jsonResponse)
        } catch (e: Exception) {
            e.printStackTrace()
            "ملخص المستند غير متوفر حالياً: ${e.localizedMessage}"
        }
    }

    /**
     * Translates deep text blocks with prompt guidelines
     */
    suspend fun translateText(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "مفتاح API غير متوفر! يرجى إضافته.\n\n[ترجمة افتراضية]:\nArabic Title: دليل البدء السريع PDF Reader\n- للتعديل على أي نص، اضغط مطولاً على النص للدخول إلى وضع التعديل وتغيير اللون والحجم.\n- لحماية الملف، اختر كلمة مرور حماية."
        }

        try {
            val instructPart = JSONObject().apply {
                put("text", "Translate the following text professionally to $targetLanguage. Keep form details intact:\n\n$text")
            }

            val contentObj = JSONObject().apply {
                put("parts", JSONArray().put(instructPart))
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(contentObj))
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val rawResponse = GeminiClient.service.generateContent(apiKey, requestBody)
            val jsonResponse = JSONObject(rawResponse.string())

            extractTextFromResponse(jsonResponse)
        } catch (e: Exception) {
            e.printStackTrace()
            "الترجمة غير متوفرة حالياً: ${e.localizedMessage}"
        }
    }

    private fun extractTextFromResponse(json: JSONObject): String {
        return try {
            val candidates = json.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val part = parts.getJSONObject(0)
            part.getString("text")
        } catch (e: Exception) {
            "Error: Parse failure. Response didn't match standard candidate format."
        }
    }
}
