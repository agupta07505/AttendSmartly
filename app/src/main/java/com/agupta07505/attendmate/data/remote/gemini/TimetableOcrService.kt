package com.agupta07505.attendmate.data.remote.gemini

import android.graphics.Bitmap
import android.util.Base64
import com.agupta07505.attendmate.BuildConfig
import com.agupta07505.attendmate.domain.model.ParsedTimetableItem
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class TimetableOcrService {

    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Candidate Gemini model names in order of preference
    private val candidateModels = listOf(
        "gemini-2.5-flash",
        "gemini-1.5-flash",
        "gemini-2.0-flash",
        "gemini-flash-latest"
    )

    suspend fun extractTimetableFromImage(
        bitmap: Bitmap,
        isSampleImage: Boolean = false
    ): List<ParsedTimetableItem> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        val isApiKeyValid = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (isSampleImage && !isApiKeyValid) {
            // Instant reliable response for sample image when key is default
            return@withContext getSampleTimetableItems()
        }

        if (!isApiKeyValid) {
            if (isSampleImage) return@withContext getSampleTimetableItems()
            throw IllegalStateException("Gemini API key is not configured. Please set your GEMINI_API_KEY in the Secrets panel in AI Studio.")
        }

        val resizedBitmap = resizeBitmapIfNeeded(bitmap, maxDimension = 1280)
        val base64Image = bitmapToBase64(resizedBitmap)

        val promptText = """
            You are an expert AI OCR system for extracting class timetables from images, photos, document scans, or screenshots.
            Analyze this image carefully. Ignore irrelevant noise like headers, footers, notes, branding, advertisements, or unrelated text.

            Extract all scheduled classes into a JSON object with a "schedules" array.
            Days MUST be integers 1 to 7 corresponding to:
            1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday, 7 = Sunday.

            For each class/entry extract:
            - subjectName: Full clean subject name (e.g., "Data Structures", "Physics Lab")
            - subjectCode: Short code if available (e.g., "CS201", "PHY101") or empty string
            - startTime: Time in HH:mm 24-hour format (e.g., "09:00", "14:30")
            - endTime: Time in HH:mm 24-hour format (e.g., "10:00", "16:00")
            - roomLocation: Room or hall number if available, else empty string
            - dayOfWeek: Integer 1-7 (1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday)

            Respond ONLY with raw JSON in this exact structure:
            {
              "schedules": [
                {
                  "subjectName": "Data Structures",
                  "subjectCode": "CS201",
                  "startTime": "09:00",
                  "endTime": "10:00",
                  "roomLocation": "Room 302",
                  "dayOfWeek": 1
                }
              ]
            }
        """.trimIndent()

        val jsonPayload = gson.toJson(
            GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = promptText),
                            GeminiPart(
                                inlineData = GeminiInlineData(
                                    mimeType = "image/jpeg",
                                    data = base64Image
                                )
                            )
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json"
                )
            )
        )

        var lastError: Exception? = null
        for (modelName in candidateModels) {
            try {
                val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(requestUrl)
                    .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    lastError = RuntimeException("Model $modelName status ${response.code}: $responseBodyString")
                    continue
                }

                val geminiResponse = gson.fromJson(responseBodyString, GeminiResponse::class.java)
                val rawText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw RuntimeException("No text candidate returned from $modelName")

                val cleanedJson = cleanJsonResponse(rawText)
                val extractedSchedules = gson.fromJson(cleanedJson, ExtractedSchedulesContainer::class.java)

                val parsed = extractedSchedules.schedules?.mapNotNull { item ->
                    val name = item.subjectName?.trim()
                    if (name.isNullOrEmpty()) return@mapNotNull null

                    val day = (item.dayOfWeek ?: 1).coerceIn(1, 7)
                    val start = formatTime(item.startTime ?: "09:00")
                    val end = formatTime(item.endTime ?: "10:00")

                    ParsedTimetableItem(
                        subjectName = name,
                        subjectCode = item.subjectCode?.trim() ?: "",
                        startTime = start,
                        endTime = end,
                        roomLocation = item.roomLocation?.trim() ?: "",
                        dayOfWeek = day
                    )
                } ?: emptyList()

                if (parsed.isNotEmpty()) {
                    return@withContext parsed
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (isSampleImage) {
            return@withContext getSampleTimetableItems()
        }

        val errorDetails = lastError?.localizedMessage ?: "Network or API request failed"
        throw RuntimeException("Timetable detection failed: $errorDetails")
    }

    private fun getSampleTimetableItems(): List<ParsedTimetableItem> {
        return listOf(
            ParsedTimetableItem(subjectName = "Data Structures", subjectCode = "CS201", startTime = "09:00", endTime = "10:00", roomLocation = "Lab 302", dayOfWeek = 1),
            ParsedTimetableItem(subjectName = "Linear Algebra", subjectCode = "MATH101", startTime = "11:00", endTime = "12:30", roomLocation = "Hall B", dayOfWeek = 1),
            ParsedTimetableItem(subjectName = "Physics II", subjectCode = "PHY201", startTime = "10:00", endTime = "11:30", roomLocation = "Lab 105", dayOfWeek = 2),
            ParsedTimetableItem(subjectName = "Object Oriented Prog", subjectCode = "CS202", startTime = "14:00", endTime = "15:30", roomLocation = "Room 401", dayOfWeek = 2),
            ParsedTimetableItem(subjectName = "Data Structures", subjectCode = "CS201", startTime = "09:00", endTime = "10:30", roomLocation = "Lab 302", dayOfWeek = 3),
            ParsedTimetableItem(subjectName = "Technical Comm", subjectCode = "ENG102", startTime = "11:00", endTime = "12:30", roomLocation = "Room 204", dayOfWeek = 4),
            ParsedTimetableItem(subjectName = "Database Systems", subjectCode = "CS203", startTime = "10:00", endTime = "12:00", roomLocation = "Lab 305", dayOfWeek = 5)
        )
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val aspectRatio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (width > height) {
            maxDimension to (maxDimension / aspectRatio).toInt()
        } else {
            (maxDimension * aspectRatio).toInt() to maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun cleanJsonResponse(jsonText: String): String {
        var text = jsonText.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }

    private fun formatTime(raw: String): String {
        val trimmed = raw.trim().lowercase()
        return try {
            val parts = trimmed.split(":")
            if (parts.size >= 2) {
                var hour = parts[0].filter { it.isDigit() }.toIntOrNull() ?: 9
                val minPart = parts[1].filter { it.isDigit() }.take(2)
                var min = minPart.toIntOrNull() ?: 0
                if (trimmed.contains("pm") && hour < 12) hour += 12
                if (trimmed.contains("am") && hour == 12) hour = 0
                String.format("%02d:%02d", hour.coerceIn(0, 23), min.coerceIn(0, 59))
            } else {
                "09:00"
            }
        } catch (e: Exception) {
            "09:00"
        }
    }
}

// Data models for Gemini API
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

private data class GeminiContent(
    val parts: List<GeminiPart>
)

private data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

private data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

private data class GeminiGenerationConfig(
    @SerializedName("responseMimeType")
    val responseMimeType: String? = "application/json"
)

private data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

private data class GeminiCandidate(
    val content: GeminiContent?
)

private data class ExtractedSchedulesContainer(
    val schedules: List<RawExtractedItem>?
)

private data class RawExtractedItem(
    val subjectName: String?,
    val subjectCode: String?,
    val startTime: String?,
    val endTime: String?,
    val roomLocation: String?,
    val dayOfWeek: Int?
)
