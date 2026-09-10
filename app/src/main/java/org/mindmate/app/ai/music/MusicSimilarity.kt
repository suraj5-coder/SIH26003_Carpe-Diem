package org.mindmate.app.ai.music

import kotlin.math.abs
import kotlin.math.max

/** Lightweight sequence comparison used after YIN/chroma feature extraction. */
object DynamicTimeWarping {
    fun similarity(expected: FloatArray, actual: FloatArray): Int {
        if (expected.isEmpty() || actual.isEmpty()) return 0
        val previous = FloatArray(actual.size + 1) { Float.POSITIVE_INFINITY }
        val current = FloatArray(actual.size + 1) { Float.POSITIVE_INFINITY }
        previous[0] = 0f
        expected.forEach { target ->
            current[0] = Float.POSITIVE_INFINITY
            actual.forEachIndexed { index, observed ->
                val cost = abs(target - observed)
                current[index + 1] = cost + minOf(current[index], previous[index + 1], previous[index])
            }
            current.copyInto(previous)
        }
        val normalizedCost = previous[actual.size] / max(expected.size, actual.size)
        return ((1f - normalizedCost.coerceIn(0f, 1f)) * 100).toInt()
    }
}

interface MusicFeatureExtractor : AutoCloseable {
    suspend fun pitchAndChroma(pcm: ShortArray, sampleRate: Int): Result<FloatArray>
}

/** Add a tested YIN/chroma extractor here; the app never invents a singing score without it. */
class UnavailableMusicFeatureExtractor : MusicFeatureExtractor {
    override suspend fun pitchAndChroma(pcm: ShortArray, sampleRate: Int): Result<FloatArray> =
        Result.failure(IllegalStateException("Offline music feature extractor is not installed."))

    override fun close() = Unit
}
