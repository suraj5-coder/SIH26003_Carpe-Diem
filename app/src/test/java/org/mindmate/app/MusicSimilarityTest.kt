package org.mindmate.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mindmate.app.ai.music.DynamicTimeWarping

class MusicSimilarityTest {
    @Test fun `identical sequences score one hundred`() {
        assertEquals(100, DynamicTimeWarping.similarity(floatArrayOf(0.1f, 0.4f), floatArrayOf(0.1f, 0.4f)))
    }

    @Test fun `similar melody scores above different melody`() {
        val expected = floatArrayOf(0.1f, 0.2f, 0.4f, 0.5f)
        val close = DynamicTimeWarping.similarity(expected, floatArrayOf(0.1f, 0.22f, 0.39f, 0.52f))
        val different = DynamicTimeWarping.similarity(expected, floatArrayOf(0.9f, 0.8f, 0.7f, 0.6f))
        assertTrue(close > different)
    }

    @Test fun `empty sequence has no score`() {
        assertEquals(0, DynamicTimeWarping.similarity(floatArrayOf(), floatArrayOf(0.2f)))
    }
}
