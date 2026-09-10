package org.mindmate.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mindmate.app.MainActivity
import org.mindmate.app.R
import org.mindmate.app.data.local.AlertEntity
import org.mindmate.app.data.local.HydrationEntity
import org.mindmate.app.data.local.MedicineIntakeEntity
import org.mindmate.app.data.local.MindMateDatabase
import java.util.concurrent.TimeUnit

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_FIRE -> fire(context, intent)
                    ACTION_TAKEN -> respond(context, intent, "CONFIRMED")
                    ACTION_REMIND -> respond(context, intent, "REMIND_LATER")
                    ACTION_HELP -> respond(context, intent, "NEED_HELP")
                    ACTION_WATER -> recordWater(context, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun fire(context: Context, intent: Intent) {
        val reminder = intent.toReminder()
        createChannel(context)
        val notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (android.os.Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED)
        if (!notificationsAllowed) {
            MindMateDatabase.get(context).dao().insertAlert(
                AlertEntity(
                    patientId = reminder.patientId,
                    type = "REMINDER_BLOCKED",
                    title = "Reminder notifications are off",
                    message = "Enable notifications so the patient can receive ${reminder.title} reminders.",
                ),
            )
            if (reminder.repeatDaily) ReminderScheduler(context).schedule(reminder)
            else MindMateDatabase.get(context).dao().setReminderEnabled(reminder.id, false)
            return
        }
        val intakeId = if (reminder.type == "MEDICINE" && reminder.relatedRecordId != null) {
            val id = MindMateDatabase.get(context).dao().insertMedicineIntake(
                MedicineIntakeEntity(
                    medicineId = reminder.relatedRecordId,
                    scheduledAt = System.currentTimeMillis(),
                    status = "NOT_CONFIRMED",
                ),
            )
            scheduleConfirmationCheck(context, id, reminder.patientId)
            id
        } else 0L
        showNotification(context, reminder, intakeId)
        if (reminder.repeatDaily) ReminderScheduler(context).schedule(reminder)
        else MindMateDatabase.get(context).dao().setReminderEnabled(reminder.id, false)
    }

    private suspend fun respond(context: Context, intent: Intent, status: String) {
        val intakeId = intent.getLongExtra(EXTRA_INTAKE_ID, 0)
        val patientId = intent.getLongExtra(EXTRA_PATIENT_ID, 0)
        if (intakeId > 0) {
            MindMateDatabase.get(context).dao().updateMedicineIntake(intakeId, status, System.currentTimeMillis())
            WorkManager.getInstance(context).cancelUniqueWork("medicine-intake-$intakeId")
        }
        if (status == "REMIND_LATER") {
            ReminderScheduler(context).snooze(intent.toReminder())
        }
        if (status == "NEED_HELP") {
            MindMateDatabase.get(context).dao().insertAlert(
                AlertEntity(patientId = patientId, type = "MEDICINE_HELP", title = "Medicine help requested", message = "The patient asked for help with medicine."),
            )
        }
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
    }

    private suspend fun recordWater(context: Context, intent: Intent) {
        val patientId = intent.getLongExtra(EXTRA_PATIENT_ID, 0)
        if (patientId > 0) MindMateDatabase.get(context).dao().insertHydration(HydrationEntity(patientId = patientId))
        NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0))
    }

    private fun showNotification(context: Context, reminder: org.mindmate.app.data.local.ReminderEntity, intakeId: Long) {
        createChannel(context)
        val notificationId = reminder.id.hashCode()
        val openApp = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(reminder.title)
            .setContentText(reminder.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)

        if (reminder.type == "MEDICINE") {
            builder.addAction(action(context, reminder, intakeId, notificationId, ACTION_TAKEN, "TAKEN", 1))
            builder.addAction(action(context, reminder, intakeId, notificationId, ACTION_REMIND, "REMIND ME", 2))
            builder.addAction(action(context, reminder, intakeId, notificationId, ACTION_HELP, "NEED HELP", 3))
        } else if (reminder.type == "WATER") {
            builder.addAction(action(context, reminder, 0, notificationId, ACTION_WATER, "YES", 4))
        }
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33) {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }

    private fun action(
        context: Context,
        reminder: org.mindmate.app.data.local.ReminderEntity,
        intakeId: Long,
        notificationId: Int,
        action: String,
        label: String,
        offset: Int,
    ): NotificationCompat.Action {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putReminder(reminder)
            putExtra(EXTRA_INTAKE_ID, intakeId)
            putExtra(EXTRA_PATIENT_ID, reminder.patientId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + offset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(0, label, pending).build()
    }

    private fun scheduleConfirmationCheck(context: Context, intakeId: Long, patientId: Long) {
        val input = Data.Builder().putLong("intake_id", intakeId).putLong("patient_id", patientId).build()
        val request = OneTimeWorkRequestBuilder<MedicineConfirmationWorker>()
            .setInitialDelay(30, TimeUnit.MINUTES)
            .setInputData(input)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "medicine-intake-$intakeId",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.notification_channel_reminders), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    companion object {
        const val ACTION_FIRE = "org.mindmate.action.REMINDER"
        const val ACTION_TAKEN = "org.mindmate.action.MEDICINE_TAKEN"
        const val ACTION_REMIND = "org.mindmate.action.MEDICINE_REMIND"
        const val ACTION_HELP = "org.mindmate.action.MEDICINE_HELP"
        const val ACTION_WATER = "org.mindmate.action.WATER_TAKEN"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_PATIENT_ID = "patient_id"
        const val EXTRA_TYPE = "reminder_type"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_MESSAGE = "reminder_message"
        const val EXTRA_HOUR = "reminder_hour"
        const val EXTRA_MINUTE = "reminder_minute"
        const val EXTRA_DAILY = "reminder_daily"
        const val EXTRA_EXACT = "reminder_exact"
        const val EXTRA_RELATED_ID = "related_id"
        const val EXTRA_INTAKE_ID = "intake_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val CHANNEL_REMINDERS = "mindmate_reminders"
    }
}
