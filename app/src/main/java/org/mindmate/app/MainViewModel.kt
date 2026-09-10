package org.mindmate.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mindmate.app.data.local.GeofenceEntity
import org.mindmate.app.data.local.ReminderEntity
import org.mindmate.app.domain.GameType
import org.mindmate.app.domain.PrescriptionDraft
import org.mindmate.app.domain.UserRole

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MindMateApplication
    private val repository = app.repository

    val activeRole = repository.activeRole.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val patient = repository.patient.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val caregiver = repository.caregiver.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val family = repository.family.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val foods = repository.foods.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val songs = repository.songs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val places = repository.places.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val routine = repository.routine.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val medicines = repository.medicines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reminders = repository.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val contacts = repository.contacts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val alerts = repository.alerts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val gameSessions = repository.gameSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hydrationToday = repository.hydrationToday.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val exerciseToday = repository.exerciseToday.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val geofence = repository.geofence.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectRole(role: UserRole?) = viewModelScope.launch { repository.selectRole(role) }

    fun hasCaregiverPin(): Boolean = app.caregiverAccess.hasPin()

    fun setCaregiverPin(pin: String): Boolean = app.caregiverAccess.setPin(pin.toCharArray())

    fun verifyCaregiverPin(pin: String): Boolean = app.caregiverAccess.verify(pin.toCharArray())

    fun savePatient(name: String, age: Int, language: String) = viewModelScope.launch {
        repository.savePatient(name, age, language)
    }

    fun addContact(name: String, relationship: String, phone: String) = viewModelScope.launch {
        if (name.isNotBlank() && phone.any(Char::isDigit)) repository.addContact(name, relationship, phone)
    }

    fun addFamily(name: String, relationship: String, photoUri: String?) = viewModelScope.launch {
        if (name.isNotBlank()) repository.addFamily(name, relationship, photoUri)
    }

    fun addFood(name: String, category: String, imageUri: String?) = viewModelScope.launch {
        if (name.isNotBlank()) repository.addFood(name, category, imageUri)
    }

    fun addSong(name: String, language: String, audioUri: String?) = viewModelScope.launch {
        if (name.isNotBlank()) repository.addSong(name, language, audioUri)
    }

    fun addPlace(name: String, description: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.addPlace(name, description)
    }

    fun addRoutine(title: String, hour: Int, minute: Int) = viewModelScope.launch {
        if (title.isNotBlank()) repository.addRoutine(title, hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    fun confirmMedicine(draft: PrescriptionDraft, imageUri: String?, hour: Int, minute: Int) = viewModelScope.launch {
        if (draft.medicineName.isBlank()) return@launch
        val medicineId = repository.addMedicine(draft, imageUri)
        val profile = patient.value ?: return@launch
        val reminderId = repository.addReminder(
            ReminderEntity(
                patientId = profile.id,
                type = "MEDICINE",
                title = "Medicine",
                message = "Time for your medicine: ${draft.medicineName}",
                hour = hour,
                minute = minute,
                exact = true,
                relatedRecordId = medicineId,
            ),
        )
        app.reminderScheduler.schedule(
            ReminderEntity(
                id = reminderId,
                patientId = profile.id,
                type = "MEDICINE",
                title = "Medicine",
                message = "Time for your medicine: ${draft.medicineName}",
                hour = hour,
                minute = minute,
                exact = true,
                relatedRecordId = medicineId,
            ),
        )
    }

    fun addReminder(type: String, title: String, message: String, hour: Int, minute: Int) = viewModelScope.launch {
        val profile = patient.value ?: return@launch
        val reminder = ReminderEntity(
            patientId = profile.id,
            type = type,
            title = title.trim(),
            message = message.trim(),
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            exact = type == "MEDICINE",
        )
        val id = repository.addReminder(reminder)
        app.reminderScheduler.schedule(reminder.copy(id = id))
    }

    fun setReminderEnabled(reminder: ReminderEntity, enabled: Boolean) = viewModelScope.launch {
        repository.setReminderEnabled(reminder.id, enabled)
        if (enabled) app.reminderScheduler.schedule(reminder.copy(enabled = true)) else app.reminderScheduler.cancel(reminder.id)
    }

    fun drinkWater() = viewModelScope.launch { repository.drinkWater() }
    fun recordWalk(minutes: Int = 10) = viewModelScope.launch { repository.recordExercise(minutes) }

    fun recordGame(game: GameType, difficulty: Int, score: Int, correct: Int, incorrect: Int, startedAt: Long) =
        viewModelScope.launch { repository.recordGame(game, difficulty, score, correct, incorrect, startedAt) }

    fun saveAndRegisterGeofence(name: String, latitude: Double, longitude: Double, radius: Float, onResult: (String) -> Unit) =
        viewModelScope.launch {
            val zone = runCatching { repository.saveGeofenceConfig(name, latitude, longitude, radius) }
                .getOrElse { onResult(it.message ?: "Invalid safe area"); return@launch }
                ?: return@launch
            app.geofenceManager.register(zone) { result ->
                if (result is org.mindmate.app.geofence.GeofenceRegistrationResult.Registered) {
                    viewModelScope.launch { repository.activateGeofence(zone) }
                }
                onResult(result.toString())
            }
        }

    fun triggerSos(useSms: Boolean = false) = viewModelScope.launch {
        val profile = patient.value ?: return@launch
        val contact = repository.primaryContact(profile.id)
        app.sosManager.localAlarm()
        val phone = contact?.phone.orEmpty()
        val opened = if (useSms) app.sosManager.openSms(phone, profile.name) else app.sosManager.openDialer(phone)
        repository.recordSos(profile.id, if (useSms) "SMS_INTENT" else "DIALER", phone)
        if (!opened) repository.recordSos(profile.id, "INTENT_FAILED", phone)
    }
}
