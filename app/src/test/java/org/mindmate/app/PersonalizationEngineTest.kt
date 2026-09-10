package org.mindmate.app

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mindmate.app.ai.personalization.PersonalizationInput
import org.mindmate.app.ai.personalization.RuleBasedPersonalizationEngine
import org.mindmate.app.domain.GameType

class PersonalizationEngineTest {
    private val engine = RuleBasedPersonalizationEngine()

    @Test fun `score over 85 increases one level`() {
        val result = engine.evaluate(PersonalizationInput(GameType.NUMBER, 2, 86))
        assertEquals(3, result.difficulty)
    }

    @Test fun `score from 60 through 85 keeps level`() {
        assertEquals(2, engine.evaluate(PersonalizationInput(GameType.FOOD, 2, 60)).difficulty)
        assertEquals(2, engine.evaluate(PersonalizationInput(GameType.FOOD, 2, 85)).difficulty)
    }

    @Test fun `score under 60 reduces one level without crossing minimum`() {
        assertEquals(1, engine.evaluate(PersonalizationInput(GameType.FAMILY, 2, 59)).difficulty)
        assertEquals(1, engine.evaluate(PersonalizationInput(GameType.FAMILY, 1, 10)).difficulty)
    }

    @Test fun `difficulty does not exceed caregiver bounds`() {
        val result = engine.evaluate(PersonalizationInput(GameType.ROUTINE, 3, 100, maxDifficulty = 3))
        assertEquals(3, result.difficulty)
    }
}
