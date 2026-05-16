package com.example.raitha_vartha

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.regex.Pattern

object VerificationUtils {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractAgeFromId(context: Context, uri: Uri): Int? {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            val text = result.text
            
            // Regex for common date formats in IDs (DD/MM/YYYY or YYYY-MM-DD)
            val datePattern = Pattern.compile("(\\d{2}[/\\-]\\d{2}[/\\-]\\d{4})|(\\d{4}[/\\-]\\d{2}[/\\-]\\d{2})")
            val matcher = datePattern.matcher(text)
            
            var birthYear: Int? = null
            
            while (matcher.find()) {
                val dateStr = matcher.group()
                val yearMatch = Pattern.compile("\\d{4}").matcher(dateStr)
                if (yearMatch.find()) {
                    val year = yearMatch.group().toInt()
                    if (year in 1920..2020) {
                        birthYear = year
                        break
                    }
                }
            }
            
            if (birthYear == null) {
                val yearOnlyPattern = Pattern.compile("\\b(19[3-9]\\d|20[0-2]\\d)\\b")
                val yearMatcher = yearOnlyPattern.matcher(text)
                if (yearMatcher.find()) {
                    birthYear = yearMatcher.group().toInt()
                }
            }

            birthYear?.let {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                currentYear - it
            }
        } catch (e: Exception) {
            null
        }
    }
}
