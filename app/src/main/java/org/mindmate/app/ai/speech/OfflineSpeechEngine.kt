package org.mindmate.app.ai.speech

interface OfflineSpeechEngine : AutoCloseable {
    val isAvailable: Boolean
    suspend fun transcribe(pcm16KhzMono: ShortArray, languageCode: String): Result<String>
}

/**
 * Integration point for one user-installed AI4Bharat IndicConformer language pack plus a small
 * VAD model. No speech model is bundled in the low-storage base APK and no cloud fallback occurs.
 */
class LanguagePackSpeechEngine : OfflineSpeechEngine {
    override val isAvailable: Boolean = false

    override suspend fun transcribe(
        pcm16KhzMono: ShortArray,
        languageCode: String,
    ): Result<String> = Result.failure(
        IllegalStateException("Install a compatible offline language pack to enable recognition."),
    )

    override fun close() = Unit
}
