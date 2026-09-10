package org.mindmate.app.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.mindmate.app.ai.personalization.PersonalizationInput
import org.mindmate.app.ai.personalization.RuleBasedPersonalizationEngine
import org.mindmate.app.data.local.*
import org.mindmate.app.domain.GameType
import org.mindmate.app.domain.PrescriptionDraft
import org.mindmate.app.domain.UserRole
import java.util.Calendar

class MindMateRepository(private val database: MindMateDatabase) {
    private val dao = database.dao()
    private val personalization = RuleBasedPersonalizationEngine()

    val activeRole: Flow<UserRole?> = dao.observeSetting(ACTIVE_ROLE).map { setting ->
        setting?.value?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
    }
    val patient: Flow<PatientEntity?> = dao.observePatient()
    val caregiver: Flow<CaregiverEntity?> = dao.observeCaregiver()
    val family: Flow<List<FamilyMemberEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observeFamily(it.id)
    }
    val foods: Flow<List<FoodEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observeFoods(it.id)
    }
    val songs: Flow<List<SongEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observeSongs(it.id)
    }
    val places: Flow<List<FamiliarPlaceEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observePlaces(it.id)
    }
    val routine: Flow<List<RoutineItemEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observeRoutine(it.id)
    }
    val medicines: Flow<List<MedicineEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observeMedicines(it.id)
    }
    val reminders: Flow<List<ReminderEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observeReminders(it.id)
    }
    val contacts: Flow<List<EmergencyContactEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observeContacts(it.id)
    }
    val alerts: Flow<List<AlertEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observeAlerts(it.id)
    }
    val gameSessions: Flow<List<GameSessionEntity>> = patient.flatMapLatest {
        if (it == null) flowOf(emptyList()) else dao.observeRecentSessions(it.id)
    }
    val geofence: Flow<GeofenceEntity?> = patient.flatMapLatest {
        if (it == null) flowOf(null) else dao.observeGeofence(it.id)
    }

    private val todayRange: Pair<Long, Long>
        get() {
            val start = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            return start to start + 86_400_000L - 1
        }

    val hydrationToday: Flow<Int> = patient.flatMapLatest { profile ->
        if (profile == null) flowOf(0) else todayRange.let { (start, end) ->
            dao.observeHydration(profile.id, start, end).map { logs -> logs.sumOf { it.glasses } }
        }
    }
    val exerciseToday: Flow<Int> = patient.flatMapLatest { profile ->
        if (profile == null) flowOf(0) else todayRange.let { (start, end) ->
            dao.observeExercise(profile.id, start, end).map { logs -> logs.sumOf { it.durationMinutes } }
        }
    }

    suspend fun seedSampleData() {
        if (dao.patientCount() > 0) return
        database.withTransaction {
            val patientId = dao.upsertPatient(PatientEntity(name = "Anima", age = 72, preferredLanguage = "as"))
            dao.upsertCaregiver(CaregiverEntity(name = "Maya", relationship = "Daughter"))

            dao.insertFamily(FamilyMemberEntity(patientId = patientId, name = "Maya", relationship = "Daughter", description = "Lives nearby"))
            dao.insertFamily(FamilyMemberEntity(patientId = patientId, name = "Rohan", relationship = "Grandson"))
            dao.insertFood(FoodEntity(patientId = patientId, name = "Rice and dal", category = "Meal"))
            dao.insertFood(FoodEntity(patientId = patientId, name = "Banana", category = "Fruit"))
            dao.insertFood(FoodEntity(patientId = patientId, name = "Tea", category = "Drink"))
            dao.insertRoutine(RoutineItemEntity(patientId = patientId, title = "Wake up", minutesOfDay = 7 * 60))
            dao.insertRoutine(RoutineItemEntity(patientId = patientId, title = "Breakfast", minutesOfDay = 8 * 60))
            dao.insertRoutine(RoutineItemEntity(patientId = patientId, title = "Medicine", minutesOfDay = 8 * 60 + 30))
            dao.insertRoutine(RoutineItemEntity(patientId = patientId, title = "Walk", minutesOfDay = 17 * 60))
            dao.insertGames(GameType.entries.map { GameEntity(it.name, it.title) })
            dao.upsertSetting(AppSettingEntity(HYDRATION_TARGET, "6"))
            dao.upsertSetting(AppSettingEntity(EXERCISE_TARGET, "10"))
        }
    }

    suspend fun selectRole(role: UserRole?) {
        dao.upsertSetting(AppSettingEntity(ACTIVE_ROLE, role?.name.orEmpty()))
    }

    suspend fun savePatient(name: String, age: Int, language: String) {
        val existing = dao.patient()
        dao.upsertPatient(
            PatientEntity(
                id = existing?.id ?: 0,
                name = name.trim(),
                age = age.coerceIn(1, 120),
                preferredLanguage = language,
                photoUri = existing?.photoUri,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            ),
        )
    }

    suspend fun addContact(name: String, relationship: String, phone: String) = withPatient { id ->
        dao.insertContact(EmergencyContactEntity(patientId = id, name = name.trim(), relationship = relationship.trim(), phone = phone.trim()))
    }

    suspend fun addFamily(name: String, relationship: String, photoUri: String?) = withPatient { id ->
        val recordId = dao.insertFamily(FamilyMemberEntity(patientId = id, name = name.trim(), relationship = relationship.trim(), photoUri = photoUri))
        dao.enqueueSync(SyncQueueEntity(recordType = "family_member", recordId = recordId, operation = "UPSERT"))
    }

    suspend fun addFood(name: String, category: String, imageUri: String?) = withPatient { id ->
        val recordId = dao.insertFood(FoodEntity(patientId = id, name = name.trim(), category = category.trim(), imageUri = imageUri))
        dao.enqueueSync(SyncQueueEntity(recordType = "food", recordId = recordId, operation = "UPSERT"))
    }

    suspend fun addSong(name: String, language: String, audioUri: String?) = withPatient { id ->
        dao.insertSong(SongEntity(patientId = id, name = name.trim(), language = language, audioUri = audioUri))
    }

    suspend fun addPlace(name: String, description: String) = withPatient { id ->
        dao.insertPlace(FamiliarPlaceEntity(patientId = id, name = name.trim(), description = description.trim()))
    }

    suspend fun addRoutine(title: String, hour: Int, minute: Int) = withPatient { id ->
        dao.insertRoutine(RoutineItemEntity(patientId = id, title = title.trim(), minutesOfDay = hour * 60 + minute))
    }

    suspend fun addMedicine(draft: PrescriptionDraft, imageUri: String?): Long {
        val profile = dao.patient() ?: return 0
        return dao.insertMedicine(
            MedicineEntity(
                patientId = profile.id,
                name = draft.medicineName.trim(),
                dose = draft.dose.trim(),
                frequency = draft.frequency.trim(),
                duration = draft.duration.trim(),
                confirmedByCaregiver = true,
                prescriptionImageUri = imageUri,
            ),
        )
    }

    suspend fun addReminder(reminder: ReminderEntity): Long = dao.insertReminder(reminder)
    suspend fun enabledReminders(): List<ReminderEntity> = dao.enabledReminders()
    suspend fun setReminderEnabled(id: Long, enabled: Boolean) = dao.setReminderEnabled(id, enabled)

    suspend fun drinkWater() = withPatient { dao.insertHydration(HydrationEntity(patientId = it)) }
    suspend fun recordExercise(minutes: Int) = withPatient {
        dao.insertExercise(ExerciseEntity(patientId = it, type = "Walk", durationMinutes = minutes, targetMinutes = 10))
    }

    suspend fun saveGeofenceConfig(name: String, latitude: Double, longitude: Double, radius: Float): GeofenceEntity? {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
        require(radius in 100f..5_000f) { "Radius must be between 100 and 5000 metres" }
        val profile = dao.patient() ?: return null
        val existing = dao.geofence(profile.id)
        val config = GeofenceEntity(
            id = existing?.id ?: 0,
            patientId = profile.id,
            name = name.trim(),
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radius,
            enabled = false,
        )
        val insertedId = dao.upsertGeofence(config)
        return config.copy(id = existing?.id ?: insertedId)
    }

    suspend fun activateGeofence(zone: GeofenceEntity) {
        dao.upsertGeofence(zone.copy(enabled = true))
    }

    suspend fun recordGame(game: GameType, difficulty: Int, score: Int, correct: Int, incorrect: Int, startedAt: Long) {
        val profile = dao.patient() ?: return
        val completedAt = System.currentTimeMillis()
        database.withTransaction {
            val sessionId = dao.insertGameSession(
                GameSessionEntity(
                    patientId = profile.id,
                    gameType = game.name,
                    startedAt = startedAt,
                    completedAt = completedAt,
                    difficulty = difficulty,
                    scorePercent = score.coerceIn(0, 100),
                    correct = correct,
                    incorrect = incorrect,
                    attempts = correct + incorrect,
                    hints = 0,
                    responseTimeMs = completedAt - startedAt,
                ),
            )
            val old = dao.performance(profile.id, game.skill.name)
            val decision = personalization.evaluate(PersonalizationInput(game, old?.difficulty ?: difficulty, score))
            dao.upsertPerformance(
                GamePerformanceEntity(
                    id = old?.id ?: 0,
                    patientId = profile.id,
                    skill = game.skill.name,
                    difficulty = decision.difficulty,
                    recentScore = score,
                    sessions = (old?.sessions ?: 0) + 1,
                ),
            )
            dao.enqueueSync(SyncQueueEntity(recordType = "game_session", recordId = sessionId, operation = "UPSERT"))
        }
    }

    suspend fun primaryContact(patientId: Long) = dao.primaryContact(patientId)
    suspend fun recordSos(patientId: Long, action: String, phone: String?) =
        dao.insertSos(SosEntity(patientId = patientId, activatedAt = System.currentTimeMillis(), action = action, contactPhone = phone))

    private suspend inline fun withPatient(block: suspend (Long) -> Unit) {
        dao.patient()?.id?.let { block(it) }
    }

    companion object {
        const val ACTIVE_ROLE = "active_role"
        const val HYDRATION_TARGET = "hydration_target"
        const val EXERCISE_TARGET = "exercise_target"
    }
}
