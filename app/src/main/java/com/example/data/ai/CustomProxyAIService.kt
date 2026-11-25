package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.local.KnowledgeRuleEntity
import com.example.domain.model.AnalysisReport
import com.example.domain.model.FollowUpMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

private const val PROXY_TAG = "KonozProxyAI"

/**
 * Configurable secure proxy AI provider so the user can swap AI providers in Settings
 * without ever hardcoding API keys inside the APK or public GitHub repository.
 */
class CustomProxyAIService(
    private val endpointUrlProvider: () -> String,
    private val authTokenProvider: () -> String
) : AIService {

    override val providerLabel: String = "خادم وسيط مخصص (Proxy)"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildClient(timeoutMs: Long): OkHttpClient {
        val seconds = (timeoutMs / 1000L).coerceIn(30L, 60L)
        return OkHttpClient.Builder()
            .connectTimeout(seconds, TimeUnit.SECONDS)
            .readTimeout(seconds, TimeUnit.SECONDS)
            .writeTimeout(seconds, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun analyzeImages(
        bitmaps: List<Bitmap>,
        optionalDescription: String,
        activeRules: List<KnowledgeRuleEntity>,
        timeoutMs: Long
    ): AIResult<AnalysisReport> {
        val url = endpointUrlProvider().trim()
        if (url.isBlank() || !url.startsWith("https://")) {
            return AIResult.Failure(
                "يرجى إدخال رابط HTTPS صالح للخادم الوسيط في الإعدادات، أو التبديل إلى مزود Firebase AI Logic الافتراضي."
            )
        }
        return postToProxy(url, timeoutMs) {
            val encodedImages = JSONArray()
            bitmaps.forEach { bmp ->
                val baos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                encodedImages.put(Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP))
            }
            JSONObject().apply {
                put("mode", "image_analysis")
                put("description", optionalDescription)
                put("imagesBase64", encodedImages)
                put("rulesCount", activeRules.size)
            }
        }
    }

    override suspend fun analyzeTextDescription(
        description: String,
        activeRules: List<KnowledgeRuleEntity>,
        timeoutMs: Long
    ): AIResult<AnalysisReport> {
        val url = endpointUrlProvider().trim()
        if (url.isBlank() || !url.startsWith("https://")) {
            return AIResult.Failure(
                "يرجى إدخال رابط HTTPS صالح للخادم الوسيط في الإعدادات، أو التبديل إلى مزود Firebase AI Logic الافتراضي."
            )
        }
        return postToProxy(url, timeoutMs) {
            JSONObject().apply {
                put("mode", "text_analysis")
                put("description", description)
                put("rulesCount", activeRules.size)
            }
        }
    }

    override suspend fun continueFollowUp(
        originalDescription: String,
        currentReport: AnalysisReport,
        history: List<FollowUpMessage>,
        newUserInfo: String,
        activeRules: List<KnowledgeRuleEntity>,
        timeoutMs: Long
    ): AIResult<String> {
        val url = endpointUrlProvider().trim()
        if (url.isBlank() || !url.startsWith("https://")) {
            return AIResult.Failure(
                "يرجى إدخال رابط HTTPS صالح للخادم الوسيط في الإعدادات."
            )
        }
        return try {
            val reply = withTimeout(timeoutMs) {
                withContext(Dispatchers.IO) {
                    val payload = JSONObject().apply {
                        put("mode", "followup")
                        put("originalDescription", originalDescription)
                        put("summary", currentReport.caseSummary)
                        put("newUserInfo", newUserInfo)
                    }
                    val reqBuilder = Request.Builder()
                        .url(url)
                        .post(payload.toString().toRequestBody(jsonMediaType))
                    val token = authTokenProvider().trim()
                    if (token.isNotBlank()) {
                        reqBuilder.header("Authorization", "Bearer $token")
                    }
                    buildClient(timeoutMs).newCall(reqBuilder.build()).execute().use { resp ->
                        if (resp.code == 401 || resp.code == 403) {
                            throw SecurityException("INVALID_TOKEN")
                        }
                        if (!resp.isSuccessful) {
                            throw IllegalStateException("HTTP_${resp.code}")
                        }
                        val body = resp.body?.string().orEmpty()
                        val obj = JSONObject(body)
                        obj.optString("reply", body)
                    }
                }
            }
            AIResult.Success(reply, providerLabel)
        } catch (e: TimeoutCancellationException) {
            AIResult.Failure("انتهت مهلة الاتصال بالخادم الوسيط. بياناتك محفوظة ويمكنك إعادة المحاولة.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            AIResult.Failure("مفتاح الوصول أو صلاحية الخادم الوسيط غير صالحة. يرجى مراجعة الإعدادات.")
        } catch (e: Exception) {
            Log.e(PROXY_TAG, "Proxy follow-up failed", e)
            AIResult.Failure("تعذر إكمال التحليل بسبب مشكلة في الاتصال بالخادم الوسيط. بياناتك محفوظة ويمكنك إعادة المحاولة.")
        }
    }

    private suspend fun postToProxy(
        url: String,
        timeoutMs: Long,
        payloadBuilder: () -> JSONObject
    ): AIResult<AnalysisReport> {
        return try {
            val report = withTimeout(timeoutMs) {
                withContext(Dispatchers.IO) {
                    val reqBuilder = Request.Builder()
                        .url(url)
                        .post(payloadBuilder().toString().toRequestBody(jsonMediaType))
                    val token = authTokenProvider().trim()
                    if (token.isNotBlank()) {
                        reqBuilder.header("Authorization", "Bearer $token")
                    }
                    buildClient(timeoutMs).newCall(reqBuilder.build()).execute().use { resp ->
                        if (resp.code == 401 || resp.code == 403) {
                            throw SecurityException("INVALID_TOKEN")
                        }
                        if (!resp.isSuccessful) {
                            throw IllegalStateException("HTTP_${resp.code}")
                        }
                        val raw = resp.body?.string().orEmpty()
                        FirebaseGeminiAIService.parseStructuredReport(raw)
                    }
                }
            }
            AIResult.Success(report, providerLabel)
        } catch (e: TimeoutCancellationException) {
            AIResult.Failure("انتهت مهلة انتظار الخادم الوسيط. بياناتك محفوظة ويمكنك إعادة المحاولة.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            AIResult.Failure("مفتاح الوصول غير صالح لدى الخادم الوسيط. يرجى مراجعة الإعدادات.")
        } catch (e: Exception) {
            Log.e(PROXY_TAG, "Proxy analysis failed", e)
            AIResult.Failure("تعذر إكمال التحليل بسبب مشكلة في الاتصال بالخادم الوسيط. بياناتك محفوظة ويمكنك إعادة المحاولة.")
        }
    }
}
