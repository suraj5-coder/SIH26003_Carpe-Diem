package org.mindmate.app.ai.face

import android.graphics.Bitmap

sealed interface ModelAvailability {
    data object Available : ModelAvailability
    data class Unavailable(val reason: String) : ModelAvailability
}

data class FaceEmbedding(val values: FloatArray)

interface FaceEmbeddingEngine : AutoCloseable {
    val availability: ModelAvailability
    suspend fun detectAndEmbed(bitmap: Bitmap): Result<FaceEmbedding>
}

/**
 * The base APK intentionally ships without a face identity model. Integrate MediaPipe face
 * detection and a licensed INT8 MobileFaceNet model here; encrypt output with LocalVault.
 */
class UnavailableFaceEmbeddingEngine : FaceEmbeddingEngine {
    override val availability = ModelAvailability.Unavailable(
        "Face recognition model is not installed. Photos and labels still work offline.",
    )

    override suspend fun detectAndEmbed(bitmap: Bitmap): Result<FaceEmbedding> =
        Result.failure(IllegalStateException((availability as ModelAvailability.Unavailable).reason))

    override fun close() = Unit
}
