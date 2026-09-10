package org.mindmate.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MindMateDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun observeSetting(key: String): Flow<AppSettingEntity?>

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun setting(key: String): AppSettingEntity?

    @Upsert suspend fun upsertSetting(setting: AppSettingEntity)

    @Query("SELECT * FROM patients ORDER BY id LIMIT 1")
    fun observePatient(): Flow<PatientEntity?>

    @Query("SELECT * FROM patients ORDER BY id LIMIT 1")
    suspend fun patient(): PatientEntity?

    @Upsert suspend fun upsertPatient(patient: PatientEntity): Long
    @Upsert suspend fun upsertCaregiver(caregiver: CaregiverEntity): Long

    @Query("SELECT * FROM caregivers ORDER BY id LIMIT 1")
    fun observeCaregiver(): Flow<CaregiverEntity?>

    @Query("SELECT * FROM family_members WHERE patientId = :patientId ORDER BY name")
    fun observeFamily(patientId: Long): Flow<List<FamilyMemberEntity>>
    @Insert suspend fun insertFamily(member: FamilyMemberEntity): Long
    @Query("DELETE FROM family_members WHERE id = :id") suspend fun deleteFamily(id: Long)

    @Query("SELECT * FROM foods WHERE patientId = :patientId ORDER BY name")
    fun observeFoods(patientId: Long): Flow<List<FoodEntity>>
    @Insert suspend fun insertFood(food: FoodEntity): Long
    @Query("DELETE FROM foods WHERE id = :id") suspend fun deleteFood(id: Long)

    @Query("SELECT * FROM songs WHERE patientId = :patientId ORDER BY name")
    fun observeSongs(patientId: Long): Flow<List<SongEntity>>
    @Insert suspend fun insertSong(song: SongEntity): Long

    @Query("SELECT * FROM familiar_places WHERE patientId = :patientId ORDER BY name")
    fun observePlaces(patientId: Long): Flow<List<FamiliarPlaceEntity>>
    @Insert suspend fun insertPlace(place: FamiliarPlaceEntity): Long

    @Query("SELECT * FROM routine_items WHERE patientId = :patientId ORDER BY minutesOfDay")
    fun observeRoutine(patientId: Long): Flow<List<RoutineItemEntity>>
    @Insert suspend fun insertRoutine(item: RoutineItemEntity): Long
    @Query("DELETE FROM routine_items WHERE id = :id") suspend fun deleteRoutine(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertGames(games: List<GameEntity>)
    @Insert suspend fun insertGameSession(session: GameSessionEntity): Long

    @Query("SELECT * FROM game_sessions WHERE patientId = :patientId ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecentSessions(patientId: Long, limit: Int = 20): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_performance WHERE patientId = :patientId AND skill = :skill LIMIT 1")
    suspend fun performance(patientId: Long, skill: String): GamePerformanceEntity?
    @Upsert suspend fun upsertPerformance(performance: GamePerformanceEntity)

    @Query("SELECT * FROM medicines WHERE patientId = :patientId ORDER BY name")
    fun observeMedicines(patientId: Long): Flow<List<MedicineEntity>>
    @Insert suspend fun insertMedicine(medicine: MedicineEntity): Long
    @Insert suspend fun insertMedicineSchedule(schedule: MedicineScheduleEntity): Long
    @Insert suspend fun insertMedicineIntake(intake: MedicineIntakeEntity): Long
    @Query("SELECT * FROM medicine_intakes WHERE id = :id LIMIT 1")
    suspend fun medicineIntake(id: Long): MedicineIntakeEntity?
    @Query("UPDATE medicine_intakes SET status = :status, respondedAt = :respondedAt WHERE id = :id")
    suspend fun updateMedicineIntake(id: Long, status: String, respondedAt: Long)

    @Query("SELECT * FROM hydration WHERE patientId = :patientId AND recordedAt BETWEEN :start AND :end")
    fun observeHydration(patientId: Long, start: Long, end: Long): Flow<List<HydrationEntity>>
    @Insert suspend fun insertHydration(hydration: HydrationEntity): Long

    @Query("SELECT * FROM exercises WHERE patientId = :patientId AND recordedAt BETWEEN :start AND :end")
    fun observeExercise(patientId: Long, start: Long, end: Long): Flow<List<ExerciseEntity>>
    @Insert suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Query("SELECT * FROM reminders WHERE patientId = :patientId ORDER BY hour, minute")
    fun observeReminders(patientId: Long): Flow<List<ReminderEntity>>
    @Query("SELECT * FROM reminders WHERE enabled = 1") suspend fun enabledReminders(): List<ReminderEntity>
    @Insert suspend fun insertReminder(reminder: ReminderEntity): Long
    @Query("UPDATE reminders SET enabled = :enabled WHERE id = :id") suspend fun setReminderEnabled(id: Long, enabled: Boolean)

    @Query("SELECT * FROM geofences WHERE patientId = :patientId LIMIT 1")
    fun observeGeofence(patientId: Long): Flow<GeofenceEntity?>
    @Query("SELECT * FROM geofences WHERE patientId = :patientId LIMIT 1")
    suspend fun geofence(patientId: Long): GeofenceEntity?
    @Query("SELECT * FROM geofences WHERE enabled = 1") suspend fun enabledGeofences(): List<GeofenceEntity>
    @Upsert suspend fun upsertGeofence(geofence: GeofenceEntity): Long

    @Query("SELECT * FROM emergency_contacts WHERE patientId = :patientId ORDER BY priority, id")
    fun observeContacts(patientId: Long): Flow<List<EmergencyContactEntity>>
    @Query("SELECT * FROM emergency_contacts WHERE patientId = :patientId ORDER BY priority, id LIMIT 1")
    suspend fun primaryContact(patientId: Long): EmergencyContactEntity?
    @Insert suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Insert suspend fun insertSos(event: SosEntity): Long
    @Insert suspend fun insertAlert(alert: AlertEntity): Long
    @Query("SELECT * FROM alerts WHERE patientId = :patientId ORDER BY createdAt DESC LIMIT 50")
    fun observeAlerts(patientId: Long): Flow<List<AlertEntity>>
    @Query("UPDATE alerts SET acknowledged = 1 WHERE id = :id") suspend fun acknowledgeAlert(id: Long)

    @Insert suspend fun enqueueSync(item: SyncQueueEntity): Long
    @Query("SELECT * FROM sync_queue WHERE state = 'PENDING' ORDER BY createdAt LIMIT :limit")
    suspend fun pendingSync(limit: Int = 50): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM patients") suspend fun patientCount(): Int
}
