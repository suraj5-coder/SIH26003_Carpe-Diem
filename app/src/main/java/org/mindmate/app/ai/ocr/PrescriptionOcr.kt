package org.mindmate.app.ai.ocr

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.mindmate.app.domain.PrescriptionDraft
import kotlin.coroutines.resume

class PrescriptionOcr(languageCode: String) : AutoCloseable {
    private val recognizer: TextRecognizer = if (languageCode == "hi") {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    } else {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognize(image: InputImage): Result<PrescriptionDraft> =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text ->
                    if (continuation.isActive) {
                        continuation.resume(Result.success(PrescriptionParser.parse(text.text)))
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resume(Result.failure(error))
                }
        }

    override fun close() = recognizer.close()
}

/** Conservative extraction: caregiver confirmation is mandatory before anything is stored. */
object PrescriptionParser {
    private val dosePattern = Regex("(?i)\\b(\\d+(?:\\.\\d+)?\\s?(?:mg|mcg|g|ml))\\b")
    private val durationPattern = Regex("(?i)\\b(\\d+\\s?(?:day|days|week|weeks|दिन))\\b")
    private val frequencyPattern = Regex(
        "(?i)\\b(once daily|twice daily|thrice daily|daily|at night|after food|before food|OD|BD|TDS)\\b",
    )

    fun parse(raw: String): PrescriptionDraft {
        val lines = raw.lines().map(String::trim).filter(String::isNotBlank)
        val dose = dosePattern.find(raw)?.value.orEmpty()
        val duration = durationPattern.find(raw)?.value.orEmpty()
        val frequency = frequencyPattern.find(raw)?.value.orEmpty()
        val medicine = lines.firstOrNull { line ->
            line.any(Char::isLetter) && !line.contains("doctor", ignoreCase = true)
        }?.replace(dosePattern, "")?.trim().orEmpty()
        return PrescriptionDraft(medicine, dose, frequency, duration, raw)
    }
}
