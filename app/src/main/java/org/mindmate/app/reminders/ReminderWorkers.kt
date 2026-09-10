package org.mindmate.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.mindmate.app.data.local.AlertEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import org.mindmate.app.data.local.MindMateDatabase
import org.mindmate.app.geofence.GeofenceManager
import kotlin.coroutines.resume

class MedicineConfirmationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val intakeId = inputData.getLong("intake_id", 0)
        val patientId = inputData.getLong("patient_id", 0)
        val intake = MindMateDatabase.get(applicationContext).dao().medicineIntake(intakeId)
        if (intake?.status == "NOT_CONFIRMED") {
            MindMateDatabase.get(applicationContext).dao().insertAlert(
                AlertEntity(
                    patientId = patientId,
                    type = "MEDICINE_NOT_CONFIRMED",
                    title = "Medicine intake not confirmed",
                    message = "Medicine intake was not confirmed within the configured time.",
                ),
            )
        }
        return Result.success()
    }
}

class RescheduleRemindersWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = MindMateDatabase.get(applicationContext).dao()
        dao.enabledReminders().forEach { ReminderScheduler(applicationContext).schedule(it) }
        dao.enabledGeofences().forEach { zone ->
            suspendCancellableCoroutine { continuation ->
                GeofenceManager(applicationContext).register(zone) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
        return Result.success()
    }
}

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build())
    }
}
