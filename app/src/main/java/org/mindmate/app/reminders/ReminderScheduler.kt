package org.mindmate.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.mindmate.app.data.local.ReminderEntity
import java.util.Calendar

class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun schedule(reminder: ReminderEntity) {
        if (!reminder.enabled || reminder.id == 0L) return
        scheduleAt(reminder, nextOccurrence(reminder.hour, reminder.minute))
    }

    fun snooze(reminder: ReminderEntity, delayMillis: Long = 10 * 60_000L) {
        if (reminder.id == 0L) return
        scheduleAt(reminder, System.currentTimeMillis() + delayMillis)
    }

    private fun scheduleAt(reminder: ReminderEntity, triggerAt: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putReminder(reminder)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (reminder.exact && canUseExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(reminderId: Long) {
        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarms.cancel(pending)
    }

    private fun canUseExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    private fun nextOccurrence(hour: Int, minute: Int): Long = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        timeInMillis
    }
}

internal fun Intent.putReminder(reminder: ReminderEntity) {
    putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
    putExtra(ReminderReceiver.EXTRA_PATIENT_ID, reminder.patientId)
    putExtra(ReminderReceiver.EXTRA_TYPE, reminder.type)
    putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
    putExtra(ReminderReceiver.EXTRA_MESSAGE, reminder.message)
    putExtra(ReminderReceiver.EXTRA_HOUR, reminder.hour)
    putExtra(ReminderReceiver.EXTRA_MINUTE, reminder.minute)
    putExtra(ReminderReceiver.EXTRA_DAILY, reminder.repeatDaily)
    putExtra(ReminderReceiver.EXTRA_EXACT, reminder.exact)
    putExtra(ReminderReceiver.EXTRA_RELATED_ID, reminder.relatedRecordId ?: 0L)
}

internal fun Intent.toReminder() = ReminderEntity(
    id = getLongExtra(ReminderReceiver.EXTRA_ID, 0),
    patientId = getLongExtra(ReminderReceiver.EXTRA_PATIENT_ID, 0),
    type = getStringExtra(ReminderReceiver.EXTRA_TYPE).orEmpty(),
    title = getStringExtra(ReminderReceiver.EXTRA_TITLE).orEmpty(),
    message = getStringExtra(ReminderReceiver.EXTRA_MESSAGE).orEmpty(),
    hour = getIntExtra(ReminderReceiver.EXTRA_HOUR, 8),
    minute = getIntExtra(ReminderReceiver.EXTRA_MINUTE, 0),
    repeatDaily = getBooleanExtra(ReminderReceiver.EXTRA_DAILY, true),
    exact = getBooleanExtra(ReminderReceiver.EXTRA_EXACT, false),
    relatedRecordId = getLongExtra(ReminderReceiver.EXTRA_RELATED_ID, 0).takeIf { it > 0 },
)
