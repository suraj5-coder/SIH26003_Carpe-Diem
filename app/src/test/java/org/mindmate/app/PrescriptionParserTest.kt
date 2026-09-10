package org.mindmate.app

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mindmate.app.ai.ocr.PrescriptionParser

class PrescriptionParserTest {
    @Test fun `extracts common English prescription fields`() {
        val draft = PrescriptionParser.parse(
            """
            Paracetamol 500mg
            1 tablet twice daily
            5 days
            """.trimIndent(),
        )

        assertEquals("Paracetamol", draft.medicineName)
        assertEquals("500mg", draft.dose)
        assertEquals("twice daily", draft.frequency)
        assertEquals("5 days", draft.duration)
    }

    @Test fun `leaves uncertain fields empty for caregiver review`() {
        val draft = PrescriptionParser.parse("Unknown handwritten medicine")
        assertEquals("Unknown handwritten medicine", draft.medicineName)
        assertEquals("", draft.dose)
        assertEquals("", draft.frequency)
        assertEquals("", draft.duration)
    }
}
