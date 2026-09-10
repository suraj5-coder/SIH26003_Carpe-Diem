package org.mindmate.app.ai.personalization

import org.mindmate.app.domain.GameType
import org.mindmate.app.domain.Skill

/** Game tuning only. These values are not clinical or diagnostic measurements. */
data class PersonalizationInput(
    val gameType: GameType,
    val currentDifficulty: Int,
    val recentScorePercent: Int,
    val minDifficulty: Int = 1,
    val maxDifficulty: Int = 4,
)

data class PersonalizationDecision(
    val difficulty: Int,
    val skill: Skill,
    val reason: String,
)

interface PersonalizationEngine {
    fun evaluate(input: PersonalizationInput): PersonalizationDecision
}

class RuleBasedPersonalizationEngine : PersonalizationEngine {
    override fun evaluate(input: PersonalizationInput): PersonalizationDecision {
        val next = when {
            input.recentScorePercent > 85 -> input.currentDifficulty + 1
            input.recentScorePercent < 60 -> input.currentDifficulty - 1
            else -> input.currentDifficulty
        }.coerceIn(input.minDifficulty, input.maxDifficulty)

        val reason = when {
            next > input.currentDifficulty -> "Strong recent game performance; one level higher"
            next < input.currentDifficulty -> "A gentler level may feel more comfortable"
            else -> "Keep the current level for steady practice"
        }
        return PersonalizationDecision(next, input.gameType.skill, reason)
    }
}
