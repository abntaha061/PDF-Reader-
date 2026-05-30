package com.example.pdf

import android.content.Context
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecureApiKeyStorage {
    private const val PREFS_NAME = "secure_gemini_prefs"
    private const val KEY_API_KEY = "encrypted_gemini_api_key"
    
    // A 16-byte key for AES-128 encryption/obfuscation
    private val AES_KEY = byteArrayOf(
        0x50, 0x64, 0x66, 0x53, 0x65, 0x63, 0x75, 0x72, // "PdfSecur"
        0x65, 0x47, 0x65, 0x6d, 0x69, 0x6e, 0x69, 0x4b  // "eGeminiK"
    )
    private val AES_IV = byteArrayOf(
        0x41, 0x49, 0x53, 0x74, 0x75, 0x64, 0x69, 0x6f, // "AIStudio"
        0x50, 0x64, 0x66, 0x53, 0x65, 0x63, 0x75, 0x72  // "PdfSecur"
    )

    fun saveApiKey(context: Context, apiKey: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            sharedPreferences.edit().remove(KEY_API_KEY).apply()
            return
        }
        try {
            val keySpec = SecretKeySpec(AES_KEY, "AES")
            val ivSpec = IvParameterSpec(AES_IV)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            sharedPreferences.edit().putString(KEY_API_KEY, encryptedString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to simple Base64 encoding if AES block cipher is not supported for any reason
            sharedPreferences.edit().putString(KEY_API_KEY, Base64.encodeToString(trimmed.toByteArray(Charsets.UTF_8), Base64.DEFAULT)).apply()
        }
    }

    fun getApiKey(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedString = sharedPreferences.getString(KEY_API_KEY, null) ?: return ""
        try {
            val decodedBytes = Base64.decode(encryptedString, Base64.DEFAULT)
            val keySpec = SecretKeySpec(AES_KEY, "AES")
            val ivSpec = IvParameterSpec(AES_IV)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                // Try fallback to just decode
                val decodedBytes = Base64.decode(encryptedString, Base64.DEFAULT)
                return String(decodedBytes, Charsets.UTF_8)
            } catch (ex: Exception) {
                return ""
            }
        }
    }
}
