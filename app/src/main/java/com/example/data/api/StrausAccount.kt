package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class StrausAccount(
    var key: String? = null,
    var cabinetNo: String? = null,
    var country: String? = null,
    var subUrl: String? = null,
    var expiresAt: String? = null,
    var panelUser: String? = null,
    var error: String? = null,
    var baseUrl: String = DEFAULT_BASE
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val DEFAULT_BASE = "https://straustopp.online"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun register(): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = "{}".toRequestBody(JSON)
            val request = Request.Builder()
                .url("$baseUrl/api/register.php")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val rspBody = response.body?.string() ?: ""
                if (response.isSuccessful && rspBody.isNotEmpty()) {
                    val json = JSONObject(rspBody)
                    if (json.optBoolean("ok")) {
                        key = json.optString("account_key")
                        cabinetNo = json.optString("cabinet_no")
                        country = json.optString("country_code")
                        return@withContext true
                    } else {
                        error = json.optString("error", "unknown")
                    }
                } else {
                    error = "HTTP error: ${response.code}"
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        }
        false
    }

    suspend fun login(providedKey: String = key ?: ""): Boolean = withContext(Dispatchers.IO) {
        try {
            this@StrausAccount.key = providedKey
            val jsonReq = JSONObject().apply { put("key", providedKey) }.toString()
            val request = Request.Builder()
                .url("$baseUrl/api/login.php")
                .post(jsonReq.toRequestBody(JSON))
                .build()

            client.newCall(request).execute().use { response ->
                val rspBody = response.body?.string() ?: ""
                if (response.isSuccessful && rspBody.isNotEmpty()) {
                     val json = JSONObject(rspBody)
                     if (json.optBoolean("ok")) {
                         return@withContext true
                     } else {
                         error = json.optString("error", "unknown")
                     }
                } else {
                     error = "HTTP error: ${response.code}"
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        }
        false
    }

    suspend fun startTrial(providedKey: String = key ?: ""): Boolean = withContext(Dispatchers.IO) {
         try {
             this@StrausAccount.key = providedKey
             val jsonReq = JSONObject().apply { put("key", providedKey) }.toString()
             val request = Request.Builder()
                 .url("$baseUrl/api/start_trial.php")
                 .post(jsonReq.toRequestBody(JSON))
                 .build()

             client.newCall(request).execute().use { response ->
                 val rspBody = response.body?.string() ?: ""
                 if (response.isSuccessful && rspBody.isNotEmpty()) {
                      val json = JSONObject(rspBody)
                      if (json.optBoolean("ok")) {
                          subUrl = json.optString("subscription_url")
                          expiresAt = json.optString("expires_at")
                          panelUser = json.optString("panel_username")
                          return@withContext true
                      } else {
                          error = json.optString("error", "unknown")
                      }
                 } else {
                      error = "HTTP error: ${response.code}"
                 }
             }
         } catch(e: Exception) {
             error = e.message ?: "Unknown error"
         }
         false
    }

    suspend fun fetchConfig(url: String = subUrl ?: "", format: String = "clash"): String? = withContext(Dispatchers.IO) {
         if (url.isBlank()) return@withContext null
         
         val userAgent = when(format.lowercase()) {
             "clash" -> "clash.meta"
             "v2ray" -> "v2ray"
             "singbox" -> "SFA/3.0"
             "base64" -> "okhttp/4.12.0"
             else -> "clash.meta"
         }

         try {
              val request = Request.Builder()
                  .url(url)
                  .header("User-Agent", userAgent)
                  .get()
                  .build()

              client.newCall(request).execute().use { response ->
                   if (response.isSuccessful) {
                        return@withContext response.body?.string()
                   }
              }
         } catch(e: Exception) {
              e.printStackTrace()
         }
         null
    }
    
    suspend fun fullSetup(): Boolean {
        if (!register()) return false
        if (!login()) return false
        return startTrial()
    }
}
