/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.data.remote.gemini

import android.graphics.Bitmap
import android.util.Base64
import com.agupta07505.attendsmartly.BuildConfig
import com.agupta07505.attendsmartly.domain.model.ParsedTimetableItem
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

    // Default candidate Gemini model names in order of preference as fallbacks
    private val defaultCandidateModels = listOf(
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-2.5-pro",
        "gemini-1.5-flash",
        "gemini-1.5-flash-8b",
        "gemini-1.5-pro",
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite-preview-02-05",
        "gemini-flash-latest"
    )

    suspend fun extractTimetableFromImage(
        bitmap: Bitmap,
        isSampleImage: Boolean = false,
        customApiKey: String? = null
    ): List<ParsedTimetableItem> = withContext(Dispatchers.IO) {
        val apiKey = (customApiKey ?: "").trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'").trim()
        val isApiKeyValid = apiKey.isNotEmpty()

        if (isSampleImage && !isApiKeyValid) {
            // Instant response for sample image when no user key is provided
            return@withContext getSampleTimetableItems()
        }

        if (!isApiKeyValid) {
            throw IllegalStateException("Gemini API key is required for AI timetable scanning. Please enter your Gemini API key in Settings or in the Scanner dialog.")
        }

        // Dynamically fetch models available for this API key from Gemini REST API
        val discoveredModels = fetchDiscoveredModels(apiKey)
        val modelsToTry = (discoveredModels + defaultCandidateModels).distinct()

        val resizedBitmap = resizeBitmapIfNeeded(bitmap, maxDimension = 1280)
        val base64Image = bitmapToBase64(resizedBitmap)

        val promptText = """
            You are an expert AI OCR system for extracting class timetables from images, photos, document scans, or screenshots.
            Analyze this image carefully. Ignore irrelevant noise like headers, footers, notes, branding, advertisements, or unrelated text.

            Extract all scheduled classes into a JSON object with a "schedules" array.
            Days MUST be integers 1 to 7 corresponding to:
            1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday, 7 = Sunday.

            For each class/entry extract:
            - subjectName: Primary subject identifier. STRICT ORDER OF PRIORITY:
              1. FIRST PRIORITY: Short subject name, abbreviation, or short acronym as written in or derived from the timetable (e.g., "DAA", "DSA", "OOPS", "SCS", "OS", "CN", "DBMS", "SE", "AI", "ML", "TOC", "CD", "Maths"). ALWAYS PREFER SHORT SUBJECT NAMES (e.g. use "DAA" instead of course code or full name).
              2. SECOND PRIORITY: Full subject name if no short name or acronym is present (e.g. "Design and Analysis of Algorithms", "Database Systems").
              3. THIRD PRIORITY: Subject or course code if neither short name nor full name is written (e.g. "CSE-3001").
              4. FOURTH PRIORITY: Relevant subject description based on slot context if nothing else is available.
            - subjectCode: Course code or subject catalog number if present (e.g., "CSE-3001", "CS201", "3001"). If subjectName is already "DAA", put "CSE-3001" here if present, else empty string "".
            - teacherName: Teacher, professor, instructor name or initials if present (e.g., "Dr. Smith", "Prof. Sharma", "AK"), else empty string "".
            - startTime: Time in HH:mm 24-hour format (e.g., "09:00", "14:30")
            - endTime: Time in HH:mm 24-hour format (e.g., "10:00", "16:00")
            - roomLocation: Room, lab, or hall number if available, else empty string ""
            - dayOfWeek: Integer 1-7 (1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday)
            - isPractical: Boolean true if this class is a practical session, lab, workshop, tutorial, or hands-on practice (or held in a lab room), else false for normal theory lectures/classes.
            CRITICAL: DO NOT MIX NORMAL THEORY CLASSES AND LAB SESSIONS. A normal lecture class (isPractical: false) and a lab class (isPractical: true) for the same subject MUST BE DISTINGUISHED.

            Respond ONLY with raw JSON in this exact structure:
            {
              "schedules": [
                {
                  "subjectName": "DAA",
                  "subjectCode": "CSE-3001",
                  "teacherName": "Dr. Smith",
                  "startTime": "09:00",
                  "endTime": "10:00",
                  "roomLocation": "Room 302",
                  "dayOfWeek": 1,
                  "isPractical": false
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
        for (modelName in modelsToTry) {
            try {
                val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(requestUrl)
                    .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    if (responseBodyString.contains("API_KEY_INVALID", ignoreCase = true) ||
                        responseBodyString.contains("API key not valid", ignoreCase = true) ||
                        responseBodyString.contains("keyInvalid", ignoreCase = true)) {
                        throw IllegalArgumentException("The Gemini API key provided is invalid. Please double check your API key in Settings.")
                    }
                    lastError = RuntimeException("Model $modelName status ${response.code}: $responseBodyString")
                    continue
                }

                val geminiResponse = gson.fromJson(responseBodyString, GeminiResponse::class.java)
                val rawText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw RuntimeException("No text candidate returned from $modelName")

                val cleanedJson = cleanJsonResponse(rawText)
                val extractedSchedules = gson.fromJson(cleanedJson, ExtractedSchedulesContainer::class.java)

                val parsedRaw = extractedSchedules.schedules?.mapNotNull { item ->
                    val name = item.subjectName?.trim()
                    if (name.isNullOrEmpty()) return@mapNotNull null

                    val day = (item.dayOfWeek ?: 1).coerceIn(1, 7)
                    val start = formatTime(item.startTime ?: "09:00")
                    val end = formatTime(item.endTime ?: "10:00")

                    ParsedTimetableItem(
                        subjectName = name,
                        subjectCode = item.subjectCode?.trim() ?: "",
                        teacherName = item.teacherName?.trim() ?: "",
                        startTime = start,
                        endTime = end,
                        roomLocation = item.roomLocation?.trim() ?: "",
                        dayOfWeek = day,
                        isPractical = item.isPractical == true
                    )
                } ?: emptyList()

                val mergedAndProcessed = processAndMergeParsedItems(parsedRaw)

                if (mergedAndProcessed.isNotEmpty()) {
                    return@withContext mergedAndProcessed
                }
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (isSampleImage) {
            return@withContext getSampleTimetableItems()
        }

        val rawMsg = lastError?.message ?: "Network or API request failed"
        val errorDetails = when {
            rawMsg.contains("API_KEY_INVALID", ignoreCase = true) || rawMsg.contains("keyNotValid", ignoreCase = true) ->
                "Invalid Gemini API key. Please re-enter a valid key from Google AI Studio."
            rawMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || rawMsg.contains("429") ->
                "Gemini API rate limit exceeded. Please wait a moment and try again."
            else -> rawMsg
        }
        throw RuntimeException("Timetable detection failed: $errorDetails")
    }

    private fun getSampleTimetableItems(): List<ParsedTimetableItem> {
        val rawSample = listOf(
            ParsedTimetableItem(subjectName = "Database Management System", subjectCode = "DBMS", teacherName = "Dr. A. Sharma", startTime = "09:00", endTime = "10:00", roomLocation = "Lab 302", dayOfWeek = 1, isPractical = false),
            ParsedTimetableItem(subjectName = "Database Management System", subjectCode = "DBMS", teacherName = "Dr. A. Sharma", startTime = "10:00", endTime = "11:00", roomLocation = "Lab 302", dayOfWeek = 1, isPractical = false),
            ParsedTimetableItem(subjectName = "Linear Algebra", subjectCode = "MATH101", teacherName = "Prof. R. Verma", startTime = "11:15", endTime = "12:15", roomLocation = "Hall B", dayOfWeek = 1, isPractical = false),
            ParsedTimetableItem(subjectName = "Physics Lab", subjectCode = "PHY201P", teacherName = "Dr. K. Patel", startTime = "10:00", endTime = "12:00", roomLocation = "Physics Lab 1", dayOfWeek = 2, isPractical = true),
            ParsedTimetableItem(subjectName = "Object Oriented Prog", subjectCode = "OOP", teacherName = "Prof. S. Gupta", startTime = "14:00", endTime = "15:00", roomLocation = "Room 401", dayOfWeek = 2, isPractical = false),
            ParsedTimetableItem(subjectName = "Data Structures Lab", subjectCode = "DSA Lab", teacherName = "Prof. M. Roy", startTime = "09:00", endTime = "11:00", roomLocation = "Computer Lab 2", dayOfWeek = 3, isPractical = true)
        )
        return processAndMergeParsedItems(rawSample)
    }

    private fun processAndMergeParsedItems(rawItems: List<ParsedTimetableItem>): List<ParsedTimetableItem> {
        val result = mutableListOf<ParsedTimetableItem>()

        // Process day by day (1..7)
        for (day in 1..7) {
            val dayItems = rawItems.filter { it.dayOfWeek == day }
                .sortedBy { parseTimeToMinutes(it.startTime) }

            if (dayItems.isEmpty()) continue

            var currentMerged: ParsedTimetableItem? = null

            for (item in dayItems) {
                if (currentMerged == null) {
                    currentMerged = item.copy()
                } else {
                    val currentEndMin = parseTimeToMinutes(currentMerged.endTime)
                    val nextStartMin = parseTimeToMinutes(item.startTime)

                    // Check if items are for the same subject and side-by-side / back-to-back (or small gap <= 15m)
                    val sameSubject = isSameSubject(currentMerged, item)
                    val isAdjacent = (nextStartMin <= currentEndMin) || (nextStartMin - currentEndMin <= 15)

                    if (sameSubject && isAdjacent) {
                        // Merge side-by-side classes into one
                        val newEndMin = maxOf(currentEndMin, parseTimeToMinutes(item.endTime))
                        currentMerged.endTime = minutesToTimeString(newEndMin)
                        if (currentMerged.subjectCode.isBlank() && item.subjectCode.isNotBlank()) {
                            currentMerged.subjectCode = item.subjectCode
                        }
                        if (currentMerged.teacherName.isBlank() && item.teacherName.isNotBlank()) {
                            currentMerged.teacherName = item.teacherName
                        }
                        if (currentMerged.roomLocation.isBlank() && item.roomLocation.isNotBlank()) {
                            currentMerged.roomLocation = item.roomLocation
                        }
                        if (item.isPractical) {
                            currentMerged.isPractical = true
                        }
                    } else {
                        // Finalize current merged item and start new
                        result.add(calculateClassCount(currentMerged))
                        currentMerged = item.copy()
                    }
                }
            }
            if (currentMerged != null) {
                result.add(calculateClassCount(currentMerged))
            }
        }
        return result
    }

    private fun isLabItem(item: ParsedTimetableItem): Boolean {
        return item.isPractical ||
                item.subjectName.contains("lab", ignoreCase = true) ||
                item.subjectName.contains("practical", ignoreCase = true) ||
                item.subjectName.contains("workshop", ignoreCase = true) ||
                item.subjectCode.contains("lab", ignoreCase = true) ||
                item.roomLocation.contains("lab", ignoreCase = true)
    }

    private fun isSameSubject(a: ParsedTimetableItem, b: ParsedTimetableItem): Boolean {
        // Critical: Do NOT merge a Normal Class with a LAB Class!
        val isLabA = isLabItem(a)
        val isLabB = isLabItem(b)
        if (isLabA != isLabB) {
            return false
        }

        val codeA = a.subjectCode.trim()
        val codeB = b.subjectCode.trim()
        if (codeA.isNotEmpty() && codeB.isNotEmpty() && codeA.equals(codeB, ignoreCase = true)) {
            return true
        }

        val nameA = a.subjectName.trim()
        val nameB = b.subjectName.trim()
        if (nameA.isNotEmpty() && nameB.isNotEmpty() && nameA.equals(nameB, ignoreCase = true)) {
            return true
        }

        // Check matching acronyms or code against name
        if (codeA.isNotEmpty() && nameB.equals(codeA, ignoreCase = true)) return true
        if (codeB.isNotEmpty() && nameA.equals(codeB, ignoreCase = true)) return true

        return false
    }

    private fun calculateClassCount(item: ParsedTimetableItem): ParsedTimetableItem {
        val startMin = parseTimeToMinutes(item.startTime)
        val endMin = parseTimeToMinutes(item.endTime)
        val durationMinutes = maxOf(15, endMin - startMin)

        val isLab = isLabItem(item)
        item.isPractical = isLab

        // User Rule:
        // Regular class: 60m == 1 count (e.g. 2 x 60m merged side-by-side = 120m == 2 counts)
        // Practical Lab: 120m == 1 count
        val unitMinutes = if (isLab) 120.0 else 60.0
        val count = maxOf(1, Math.round(durationMinutes.toDouble() / unitMinutes).toInt())
        item.attendanceUnitCount = count

        return item
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val h = parts[0].filter { it.isDigit() }.toIntOrNull() ?: 0
                val m = parts[1].filter { it.isDigit() }.toIntOrNull() ?: 0
                h * 60 + m
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun minutesToTimeString(totalMinutes: Int): String {
        val h = (totalMinutes / 60) % 24
        val m = totalMinutes % 60
        return String.format("%02d:%02d", h, m)
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
        text = text.trim()

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }
        return text.trim()
    }

    private fun fetchDiscoveredModels(apiKey: String): List<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = okHttpClient.newCall(request).execute()
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful || bodyStr.isBlank()) {
                return emptyList()
            }
            val listResp = gson.fromJson(bodyStr, GeminiModelListResponse::class.java)
            val models = listResp.models
                ?.filter { it.supportedGenerationMethods?.contains("generateContent") == true }
                ?.mapNotNull { it.name?.removePrefix("models/")?.trim() }
                ?.filter { it.isNotBlank() } ?: emptyList()

            models.sortedWith(Comparator { m1, m2 ->
                modelPriorityScore(m2) - modelPriorityScore(m1)
            })
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun modelPriorityScore(modelName: String): Int {
        val m = modelName.lowercase()
        return when {
            m.contains("3.6-flash") -> 100
            m.contains("3.5-flash-lite") -> 95
            m.contains("3.5-flash") -> 90
            m.contains("3.1-flash-lite") -> 85
            m.contains("2.5-flash-lite") -> 82
            m.contains("2.5-flash") -> 80
            m.contains("2.5-pro") -> 75
            m.contains("1.5-flash") -> 70
            m.contains("1.5-pro") -> 65
            m.contains("2.0-flash") -> 60
            m.contains("flash") -> 50
            m.contains("pro") -> 40
            else -> 10
        }
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
    val teacherName: String?,
    val startTime: String?,
    val endTime: String?,
    val roomLocation: String?,
    val dayOfWeek: Int?,
    val isPractical: Boolean?
)

private data class GeminiModelListResponse(
    val models: List<GeminiModelInfo>? = null
)

private data class GeminiModelInfo(
    val name: String? = null,
    val supportedGenerationMethods: List<String>? = null
)

