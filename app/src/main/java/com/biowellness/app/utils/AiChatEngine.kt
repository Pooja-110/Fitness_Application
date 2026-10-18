package com.biowellness.app.utils

import com.biowellness.app.BuildConfig
import com.biowellness.app.data.models.LiveAnalysis
import com.biowellness.app.data.models.WellnessReport
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String,
    val content: String,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

object AiChatEngine {

    private const val API_URL = "https://api.groq.com/openai/v1/chat/completions"

    // ACTIVE MODEL ID (Ensure this matches a valid Groq model ID,)
    private const val MODEL = "openai/gpt-oss-20b"

    val DEFAULT_API_KEY: String get() = BuildConfig.GROQ_API_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Builds the system instructions. Using triple quotes (""") allows multi-line text
     * without causing syntax errors.
     */
    private fun systemPrompt(a: LiveAnalysis?, r: WellnessReport?) = buildString {
        append(
            """
            You are the BioWellness AI Coach — a friendly, honest fitness and wellness assistant inside the 
            BioWellness app. The app has: (1) real-time heart rate from the phone camera (PPG) or a connected 
            Bluetooth heart-rate strap/watch, (2) an accelerometer-based sleep/rest tracker, (3) a manual 
            blood-pressure log (the app cannot measure blood pressure itself — no phone or watch sensor can 
            without a calibrated cuff), and (4) a Gym section with illustrated exercises, step-by-step form, 
            common mistakes and sets/reps guidance.

            Answer exercise and fitness questions using standard, well-established exercise science (the kind of 
            guidance in ACE/NASM/ACSM material): proper form, workout structure, progression, recovery, general 
            nutrition and hydration. Never diagnose a medical condition, never claim to detect a disease, and 
            never invent a number (blood pressure, SpO2, etc.) the app hasn't measured. For anything that sounds 
            medical (chest pain, dizziness, fainting, a diagnosis question) tell the person to see a doctor. 
            Keep answers practical and concise, and point to the Gym section for exercises it covers.
            """.trimIndent()
        )

        if (a != null && a.wellnessScore > 0) {
            append("\n\nThis scan's readings: wellness ${a.wellnessScore}/100, stress ${a.stressLevel}/100, fatigue ${a.fatigueScore}/100.")
        }

        if (r != null && r.risks.isNotEmpty()) {
            append("\n\nFlags from this scan: ${r.risks.joinToString(", ") { it.name }}.")
        }
    }

    fun send(
        apiKey: String,
        userMessage: String,
        history: List<ChatMessage>,
        liveAnalysis: LiveAnalysis?,
        report: WellnessReport?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanPassedKey = apiKey.trim().replace("\n", "").replace("\r", "")
        val cleanDefaultKey = DEFAULT_API_KEY.trim().replace("\n", "").replace("\r", "")

        val keyToUse = if (cleanPassedKey.startsWith("gsk_") && cleanPassedKey.length > 30) {
            cleanPassedKey
        } else {
            cleanDefaultKey
        }

        if (keyToUse.isBlank()) {
            onError("AI chat isn't configured — no Groq API key found.")
            return
        }

        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "system").put("content", systemPrompt(liveAnalysis, report)))

        history.forEach { m ->
            if (!m.isLoading && !m.isError) {
                msgs.put(JSONObject().put("role", m.role).put("content", m.content))
            }
        }

        msgs.put(JSONObject().put("role", "user").put("content", userMessage))

        val bodyJson = JSONObject()
            .put("model", MODEL)
            .put("messages", msgs)
            .put("temperature", 0.7)

        val body = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $keyToUse")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string()

                if (!response.isSuccessful || raw == null) {
                    val detailedError = try {
                        val jsonErr = JSONObject(raw.orEmpty())
                        jsonErr.getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        raw
                    }
                    onError("API error ${response.code}: $detailedError")
                    return
                }

                try {
                    val reply = JSONObject(raw)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    onSuccess(reply)
                } catch (e: Exception) {
                    onError("Parse error: ${e.message}")
                }
            }
        })
    }
}