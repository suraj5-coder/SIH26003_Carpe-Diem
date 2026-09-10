package org.mindmate.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.mindmate.app.data.local.MindMateDatabase
import org.mindmate.app.data.repository.MindMateRepository
import org.mindmate.app.geofence.GeofenceManager
import org.mindmate.app.reminders.ReminderScheduler
import org.mindmate.app.security.CaregiverAccess
import org.mindmate.app.sos.SosManager

class MindMateApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { MindMateDatabase.get(this) }
    val repository by lazy { MindMateRepository(database) }
    val reminderScheduler by lazy { ReminderScheduler(this) }
    val geofenceManager by lazy { GeofenceManager(this) }
    val caregiverAccess by lazy { CaregiverAccess(this) }
    val sosManager by lazy { SosManager(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { repository.seedSampleData() }
    }
}
