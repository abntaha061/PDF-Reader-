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

    private fun getEffectiveApiKey(userKey: String): String {
        val trimmed = userKey.trim()
        if (trimmed.isNotEmpty() && trimmed != "MY_GEMINI_API_KEY") {
            return trimmed
        }
        val defaultKey = BuildConfig.GEMINI_API_KEY
        if (defaultKey.isNotEmpty() && defaultKey != "MY_GEMINI_API_KEY") {
            return defaultKey
        }
        return ""
    }

    /**
     * Executes intelligent OCR (Optical Character Recognition) on an image
     */
    suspend fun performOcr(userKey: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(userKey)
        if (apiKey.isEmpty()) {
            return@withContext "مفتاح API غير متوفر! يرجى إضافة مفتاح Gemini الخاص بك بالضغط على زر الإعدادات الموجود أعلى لوحة الكوبيلوت لتتمكن من استخدام ميزة الـ OCR الفعلية."
        }

        try {
            // Build request JSON programmatically
            val inlineDataObj = JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", bitmap.toBase64())
            }

            val textPart = JSONObject().apply {
                put("text", "You are an expert OCR and document digitizing system. Please read the text in this document image and output it exactly. Preserve formatting, paragraphs, and list numbering where possible. Do not include any meta comments or introductory phrases, just output the extracted text.")
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
            "فشل التعرف الضوئي OCR: ${e.message ?: "حدث خطأ أثناء الاتصال بخوادم ريبون لـ Gemini API."}"
        }
    }

    /**
     * Executes intelligent page summaries on active text blocks
     */
    suspend fun summarizeText(userKey: String, text: String): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(userKey)
        if (apiKey.isEmpty()) {
            return@withContext "مفتاح API غير متوفر! يرجى إضافة مفتاح Gemini الخاص بك بالضغط على زر الإعدادات في لوحة الكوبايلوت.\n\nلمحة سريعة عن الصفحة:\n- تحتوي هذه الصفحة على نصوص PDF الهامة.\n- قم بإعداد مفتاح API للحصول على تلخيص فوري دقيق ومفصل بالذكاء الاصطناعي."
        }

        try {
            val systemPart = JSONObject().apply {
                put("text", "قم بكتابة ملخص احترافي دقيق وواضح باللغة العربية الفصحى على شكل نقاط واضحة ومفهومة للمستند والنصوص التالية:\n\n$text")
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
    suspend fun translateText(userKey: String, text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(userKey)
        if (apiKey.isEmpty()) {
            return@withContext "مفتاح API غير متوفر! يرجى إضافة مفتاح Gemini الخاص بك بالضغط على زر الإعدادات في لوحة الكوبايلوت.\n\nلمحة سريعة عن الصفحة:\n- تحتوي هذه الصفحة على نصوص باللغة الأصلية.\n- قم بإعداد مفتاح API للحصول على ترجمة فورية دقيقة ومتقنة بكافة تفاصيلها."
        }

        try {
            val instructPart = JSONObject().apply {
                put("text", "Translate the following text professionally to $targetLanguage. Keep all original terms, structural details, and formatting intact:\n\n$text")
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
