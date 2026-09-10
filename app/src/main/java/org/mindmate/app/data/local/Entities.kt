package org.mindmate.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val age: Int,
    val preferredLanguage: String,
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "caregivers")
data class CaregiverEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val relationship: String = "",
)

@Entity(tableName = "emergency_contacts", indices = [Index("patientId")])
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val relationship: String,
    val phone: String,
    val priority: Int = 0,
)

@Entity(tableName = "family_members", indices = [Index("patientId")])
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val relationship: String,
    val photoUri: String? = null,
    val description: String = "",
    val encryptedFaceEmbedding: ByteArray? = null,
)

@Entity(tableName = "foods", indices = [Index("patientId")])
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val imageUri: String? = null,
    val category: String = "",
)

@Entity(tableName = "songs", indices = [Index("patientId")])
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val language: String,
    val audioUri: String? = null,
    val lyrics: String = "",
    val localFeaturePath: String? = null,
)

@Entity(tableName = "familiar_places", indices = [Index("patientId")])
data class FamiliarPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String = "",
)

@Entity(
    tableName = "routine_items",
    indices = [
        Index("patientId"),
        Index(value = ["patientId", "minutesOfDay"])
    ]
)
data class RoutineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val title: String,
    val minutesOfDay: Int,
    val prompt: String = "",
)

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val type: String,
    val title: String,
    val enabled: Boolean = true,
)

@Entity(
    tableName = "game_sessions",
    indices = [
        Index("patientId"),
        Index("startedAt"),
        Index(value = ["patientId", "gameType"])
    ]
)
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val gameType: String,
    val startedAt: Long,
    val completedAt: Long,
    val difficulty: Int,
    val scorePercent: Int,
    val correct: Int,
    val incorrect: Int,
    val attempts: Int,
    val hints: Int,
    val responseTimeMs: Long,
)

@Entity(
    tableName = "game_performance",
    indices = [Index(value = ["patientId", "skill"], unique = true)]
)
data class GamePerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val skill: String,
    val difficulty: Int = 1,
    val recentScore: Int = 0,
    val sessions: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "medicines", indices = [Index("patientId")])
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val dose: String,
    val frequency: String,
    val duration: String,
    val confirmedByCaregiver: Boolean,
    val prescriptionImageUri: String? = null,
)

@Entity(tableName = "medicine_schedules", indices = [Index("medicineId")])
data class MedicineScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
)

@Entity(tableName = "medicine_intakes", indices = [Index("medicineId"), Index("scheduledAt")])
data class MedicineIntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val scheduledAt: Long,
    val respondedAt: Long? = null,
    val status: String,
)

@Entity(tableName = "hydration", indices = [Index("patientId"), Index("recordedAt")])
data class HydrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val glasses: Int = 1,
    val recordedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "exercises", indices = [Index("patientId"), Index("recordedAt")])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val type: String,
    val durationMinutes: Int,
    val targetMinutes: Int,
    val recordedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "reminders", indices = [Index("patientId"), Index("enabled")])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val type: String,
    val title: String,
    val message: String,
    val hour: Int,
    val minute: Int,
    val repeatDaily: Boolean = true,
    val exact: Boolean = false,
    val enabled: Boolean = true,
    val relatedRecordId: Long? = null,
)

@Entity(
    tableName = "geofences",
    indices = [Index(value = ["patientId"], unique = true)]
)
data class GeofenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val enabled: Boolean = true,
)

@Entity(tableName = "sos_events", indices = [Index("patientId"), Index("activatedAt")])
data class SosEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val activatedAt: Long,
    val action: String,
    val contactPhone: String? = null,
    val cancelled: Boolean = false,
)

@Entity(tableName = "alerts", indices = [Index("patientId"), Index("createdAt")])
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val type: String,
    val title: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val acknowledged: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Entity(tableName = "sync_queue", indices = [Index("createdAt"), Index("state")])
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordType: String,
    val recordId: Long,
    val operation: String,
    val createdAt: Long = System.currentTimeMillis(),
    val state: String = "PENDING",
    val attempts: Int = 0,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)