package org.mindmate.app.domain

enum class UserRole { PATIENT, CAREGIVER }

enum class SupportedLanguage(val code: String, val label: String, val localLabel: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    ASSAMESE("as", "Assamese", "অসমীয়া"),
}

enum class GameType(val title: String, val skill: Skill) {
    FAMILY("Family Memory", Skill.RECOGNITION),
    FOOD("Food Memory", Skill.MEMORY),
    MEMORY_TRAY("Memory Tray", Skill.ATTENTION),
    NUMBER("Number Memory", Skill.MEMORY),
    ROUTINE("Daily Routine", Skill.SEQUENCE),
    SONG("Finish the Song", Skill.MUSIC),
}

enum class Skill { MEMORY, ATTENTION, RECOGNITION, SEQUENCE, MUSIC, VOICE, REACTION }

enum class ReminderType { MEDICINE, WATER, MEAL, EXERCISE, APPOINTMENT, SLEEP, CUSTOM }

enum class MedicineIntakeStatus { CONFIRMED, REMIND_LATER, NEED_HELP, NOT_CONFIRMED }

data class PrescriptionDraft(
    val medicineName: String = "",
    val dose: String = "",
    val frequency: String = "",
    val duration: String = "",
    val rawText: String = "",
)
