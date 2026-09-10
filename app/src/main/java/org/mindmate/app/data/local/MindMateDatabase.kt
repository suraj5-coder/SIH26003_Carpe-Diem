package org.mindmate.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PatientEntity::class,
        CaregiverEntity::class,
        EmergencyContactEntity::class,
        FamilyMemberEntity::class,
        FoodEntity::class,
        SongEntity::class,
        FamiliarPlaceEntity::class,
        RoutineItemEntity::class,
        GameEntity::class,
        GameSessionEntity::class,
        GamePerformanceEntity::class,
        MedicineEntity::class,
        MedicineScheduleEntity::class,
        MedicineIntakeEntity::class,
        HydrationEntity::class,
        ExerciseEntity::class,
        ReminderEntity::class,
        GeofenceEntity::class,
        SosEntity::class,
        AlertEntity::class,
        SyncQueueEntity::class,
        AppSettingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MindMateDatabase : RoomDatabase() {
    abstract fun dao(): MindMateDao

    companion object {
        @Volatile private var instance: MindMateDatabase? = null

        fun get(context: Context): MindMateDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MindMateDatabase::class.java,
                "mindmate.db",
            ).build().also { instance = it }
        }
    }
}
