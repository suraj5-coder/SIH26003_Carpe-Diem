package org.mindmate.app.caregiver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mindmate.app.MainViewModel
import org.mindmate.app.data.local.PatientEntity
import org.mindmate.app.domain.SupportedLanguage
import org.mindmate.app.medicine.PrescriptionCameraScreen
import org.mindmate.app.ui.components.EmptyMessage
import org.mindmate.app.ui.components.LargeActionButton
import org.mindmate.app.ui.components.LocalImage
import org.mindmate.app.ui.components.ScreenHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CaregiverSection(val title: String) {
    PROFILE("Patient profile"), CONTACTS("Emergency contacts"), FAMILY("Family members"), FOODS("Favourite foods"), SONGS("Favourite songs"),
    PLACES("Familiar places"), ROUTINE("Daily routine"), MEDICINES("Medicines"), HYDRATION("Hydration"),
    EXERCISE("Exercise"), GEOFENCE("Safe area"), REMINDERS("Reminders"), PERFORMANCE("Game performance"),
    ALERTS("Alerts"),
}

@Composable
fun CaregiverDashboard(
    patient: PatientEntity?,
    viewModel: MainViewModel,
    onSection: (CaregiverSection) -> Unit,
    onSwitchRole: () -> Unit,
) {
    val hydration by viewModel.hydrationToday.collectAsStateWithLifecycle()
    val exercise by viewModel.exerciseToday.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Caretaker", style = MaterialTheme.typography.headlineLarge)
                Text(patient?.name ?: "Patient profile", style = MaterialTheme.typography.titleLarge)
            }
            OutlinedButton(onClick = onSwitchRole, modifier = Modifier.heightIn(min = 52.dp)) { Text("Change user") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Water", "$hydration / 6", Modifier.weight(1f))
            SummaryCard("Activity", "$exercise / 10 min", Modifier.weight(1f))
            SummaryCard("Alerts", alerts.count { !it.acknowledged }.toString(), Modifier.weight(1f))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(CaregiverSection.entries) { section ->
                OutlinedButton(
                    onClick = { onSection(section) },
                    modifier = Modifier.padding(5.dp).fillMaxWidth().heightIn(min = 78.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(section.title, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun CaregiverSectionScreen(section: CaregiverSection, viewModel: MainViewModel, onBack: () -> Unit) {
    when (section) {
        CaregiverSection.PROFILE -> ProfileEditor(viewModel, onBack)
        CaregiverSection.CONTACTS -> ContactEditor(viewModel, onBack)
        CaregiverSection.FAMILY -> FamilyEditor(viewModel, onBack)
        CaregiverSection.FOODS -> FoodEditor(viewModel, onBack)
        CaregiverSection.SONGS -> SongEditor(viewModel, onBack)
        CaregiverSection.PLACES -> PlaceEditor(viewModel, onBack)
        CaregiverSection.ROUTINE -> RoutineEditor(viewModel, onBack)
        CaregiverSection.MEDICINES -> PrescriptionCameraScreen(viewModel, onBack)
        CaregiverSection.HYDRATION -> WellnessSummary("Hydration", viewModel, onBack, true)
        CaregiverSection.EXERCISE -> WellnessSummary("Exercise", viewModel, onBack, false)
        CaregiverSection.GEOFENCE -> GeofenceEditor(viewModel, onBack)
        CaregiverSection.REMINDERS -> ReminderEditor(viewModel, onBack)
        CaregiverSection.PERFORMANCE -> PerformanceScreen(viewModel, onBack)
        CaregiverSection.ALERTS -> AlertsScreen(viewModel, onBack)
    }
}

@Composable
private fun ProfileEditor(viewModel: MainViewModel, onBack: () -> Unit) {
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    var name by remember(patient) { mutableStateOf(patient?.name.orEmpty()) }
    var age by remember(patient) { mutableStateOf(patient?.age?.toString().orEmpty()) }
    var language by remember(patient) { mutableStateOf(patient?.preferredLanguage ?: "en") }
    EditorScaffold("Patient profile", onBack) {
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(age, { age = it.filter(Char::isDigit) }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyLarge)
        Text("Preferred language", style = MaterialTheme.typography.titleLarge)
        SupportedLanguage.entries.forEach { option ->
            FilterChip(
                selected = language == option.code,
                onClick = { language = option.code },
                label = { Text("${option.label} • ${option.localLabel}", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.heightIn(min = 52.dp),
            )
        }
        LargeActionButton("SAVE PROFILE", { viewModel.savePatient(name, age.toIntOrNull() ?: 1, language) }, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
        Text("Additional Indian languages can be installed later as separate language packs.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ContactEditor(viewModel: MainViewModel, onBack: () -> Unit) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    EditorScaffold("Emergency contacts", onBack) {
        Text("The first contact is used by SOS.", style = MaterialTheme.typography.bodyMedium)
        contacts.forEach { contact ->
            Text("${contact.name} • ${contact.relationship}\n${contact.phone.ifBlank { "No phone number set" }}", style = MaterialTheme.typography.bodyLarge)
        }
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(relationship, { relationship = it }, label = { Text("Relationship") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("Phone number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        LargeActionButton("ADD CONTACT", {
            viewModel.addContact(name, relationship, phone)
            name = ""; relationship = ""; phone = ""
        }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && phone.any(Char::isDigit))
        Text("SOS opens the system dialer or SMS composer. MindMate does not claim a contact was reached.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FamilyEditor(viewModel: MainViewModel, onBack: () -> Unit) {
    val family by viewModel.family.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        photoUri = uri?.toString()
        if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    EditorScaffold("Family members", onBack) {
        Text("Photos and future face embeddings remain on this device by default.", style = MaterialTheme.typography.bodyMedium)
        family.forEach { member ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LocalImage(member.photoUri, member.name, Modifier.size(72.dp))
                Column { Text(member.name, style = MaterialTheme.typography.titleLarge); Text(member.relationship) }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(relationship, { relationship = it }, label = { Text("Relationship") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.heightIn(min = 56.dp)) { Text(if (photoUri == null) "CHOOSE PHOTO" else "PHOTO SELECTED") }
        LargeActionButton("ADD FAMILY MEMBER", { viewModel.addFamily(name, relationship, photoUri); name = ""; relationship = ""; photoUri = null }, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
        EmptyMessage("Automatic face matching is disabled until a validated MobileFaceNet model is installed. Photo-label games work now.")
    }
}

@Composable
private fun FoodEditor(viewModel: MainViewModel, onBack: () -> Unit) {
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        imageUri = uri?.toString()
        if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    EditorScaffold("Favourite foods", onBack) {
        foods.forEach { food ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LocalImage(food.imageUri, food.name, Modifier.size(68.dp)); Text("${food.name}\n${food.category}", style = MaterialTheme.typography.bodyLarge)
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("Food name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.heightIn(min = 56.dp)) { Text("CHOOSE PHOTO") }
        LargeActionButton("ADD FOOD", { viewModel.addFood(name, category, imageUri); name = ""; category = ""; imageUri = null }, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
        Text("Food recognition is not needed: your label is used directly.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SongEditor(viewModel: MainViewModel, onBack: () -> Unit) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var audioUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        audioUri = uri?.toString()
        if (uri != null) runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    EditorScaffold("Favourite songs", onBack) {
        songs.forEach { Text("♫ ${it.name} • ${it.language}", style = MaterialTheme.typography.bodyLarge) }
        OutlinedTextField(name, { name = it }, label = { Text("Song name") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }, modifier = Modifier.heightIn(min = 56.dp)) { Text("CHOOSE AUDIO") }
        LargeActionButton("ADD SONG", { viewModel.addSong(name, "en", audioUri); name = ""; audioUri = null }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && audioUri != null)
        EmptyMessage("Song playback is stored locally. Singing comparison will activate only after the tested YIN/chroma extractor is installed; no made-up score is shown.")
    }
}

@Composable
private fun PlaceEditor(viewModel: MainViewModel, onBack: () -> Unit) {
    val places by viewModel.places.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    EditorScaffold("Familiar places", onBack) {
        places.forEach { Text("• ${it.name}: ${it.description}", style = MaterialTheme.typography.bodyLarge) }
        OutlinedTextField(name, { name = it }, label = { Text("Place name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        LargeActionButton("ADD PLACE", { viewModel.addPlace(name, description); name = ""; description = "" }, Modifier.fillMaxWidth(), enabled = name.isNotBlank())
    }
}

@Composable
private fun RoutineEditor(viewModel: MainViewModel, onBack: () -> Unit) {
    val routine by viewModel.routine.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("8") }
    var minute by remember { mutableStateOf("00") }
    EditorScaffold("Daily routine", onBack) {
        routine.forEach { Text("${it.minutesOfDay / 60}:${(it.minutesOfDay % 60).toString().padStart(2, '0')}  ${it.title}", style = MaterialTheme.typography.bodyLarge) }
        OutlinedTextField(title, { title = it }, label = { Text("Activity") }, modifier = Modifier.fillMaxWidth())
        TimeFields(hour, { hour = it }, minute, { minute = it })
        LargeActionButton("ADD ROUTINE ITEM", { viewModel.addRoutine(title, hour.toIntOrNull() ?: 8, minute.toIntOrNull() ?: 0); title = "" }, Modifier.fillMaxWidth(), enabled = title.isNotBlank())
    }
}

@Composable
private fun ReminderEditor(viewModel: MainViewModel, onBack: () -> Unit) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("9") }
    var minute by remember { mutableStateOf("00") }
    var type by remember { mutableStateOf("CUSTOM") }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    EditorScaffold("Reminders", onBack) {
        if (Build.VERSION.SDK_INT >= 33) {
            OutlinedButton(
                onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.heightIn(min = 56.dp),
            ) { Text("ALLOW REMINDER NOTIFICATIONS") }
        }
        reminders.forEach { reminder ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${reminder.hour}:${reminder.minute.toString().padStart(2, '0')}  ${reminder.title}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(reminder.enabled, { viewModel.setReminderEnabled(reminder, it) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("WATER", "EXERCISE", "MEAL", "CUSTOM").forEach { value ->
                FilterChip(selected = type == value, onClick = { type = value }, label = { Text(value) })
            }
        }
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(message, { message = it }, label = { Text("Patient message") }, modifier = Modifier.fillMaxWidth())
        TimeFields(hour, { hour = it }, minute, { minute = it })
        LargeActionButton("SAVE REMINDER", { viewModel.addReminder(type, title, message, hour.toIntOrNull() ?: 9, minute.toIntOrNull() ?: 0); title = ""; message = "" }, Modifier.fillMaxWidth(), enabled = title.isNotBlank() && message.isNotBlank())
        Text("Reminders are scheduled on this device and do not need internet.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WellnessSummary(title: String, viewModel: MainViewModel, onBack: () -> Unit, hydration: Boolean) {
    val water by viewModel.hydrationToday.collectAsStateWithLifecycle()
    val exercise by viewModel.exerciseToday.collectAsStateWithLifecycle()
    EditorScaffold(title, onBack) {
        Text(if (hydration) "Today's hydration: $water / 6 glasses" else "Today's activity: $exercise minutes\nTarget: 10 minutes", style = MaterialTheme.typography.headlineMedium)
        LargeActionButton(
            if (hydration) "ADD ONE GLASS" else "ADD 10 MINUTE WALK",
            { if (hydration) viewModel.drinkWater() else viewModel.recordWalk() },
            Modifier.fillMaxWidth(),
        )
        Text("This is a daily activity record, not a medical diagnosis.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GeofenceEditor(viewModel: MainViewModel, onBack: () -> Unit) {
    val zone by viewModel.geofence.collectAsStateWithLifecycle()
    var name by remember(zone) { mutableStateOf(zone?.name ?: "Home") }
    var latitude by remember(zone) { mutableStateOf(zone?.latitude?.toString().orEmpty()) }
    var longitude by remember(zone) { mutableStateOf(zone?.longitude?.toString().orEmpty()) }
    var radius by remember(zone) { mutableStateOf(zone?.radiusMeters?.toInt()?.toString() ?: "500") }
    var result by remember { mutableStateOf("") }
    val context = LocalContext.current
    val backgroundPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        result = if (granted) "Background location allowed." else "Background location is needed for safety alerts when the app is closed."
    }
    val foregroundPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        result = if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) "Location allowed. Now allow background access." else "Precise location is needed for the safe area."
    }
    EditorScaffold("Safe area", onBack) {
        Box(Modifier.fillMaxWidth().height(190.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(150.dp).background(Color(0x66338866), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(18.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
            Text("Offline safe-area preview\nRadius: ${radius.ifBlank { "—" }} m", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp))
        }
        OutlinedTextField(name, { name = it }, label = { Text("Safe place name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(radius, { radius = it.filter(Char::isDigit) }, label = { Text("Radius in metres") }, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = {
            foregroundPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }, modifier = Modifier.heightIn(min = 56.dp)) { Text("1. ALLOW PRECISE LOCATION") }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            OutlinedButton(onClick = {
                val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!fineGranted) result = "Allow precise location first."
                else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) backgroundPermission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                else context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            }, modifier = Modifier.heightIn(min = 56.dp)) { Text("2. ALLOW LOCATION ALL THE TIME") }
        }
        LargeActionButton("SAVE AND ACTIVATE", {
            val lat = latitude.toDoubleOrNull(); val lon = longitude.toDoubleOrNull(); val r = radius.toFloatOrNull()
            if (lat == null || lon == null || r == null) result = "Enter valid coordinates and radius."
            else viewModel.saveAndRegisterGeofence(name, lat, lon, r) { result = it }
        }, Modifier.fillMaxWidth())
        if (result.isNotBlank()) Text(result, style = MaterialTheme.typography.bodyLarge)
        Text("Android monitors boundary transitions without continuous GPS polling. Alerts can be delayed by the operating system.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PerformanceScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val sessions by viewModel.gameSessions.collectAsStateWithLifecycle()
    EditorScaffold("Game performance", onBack) {
        Text("Game performance and engagement only — not a dementia diagnosis.", style = MaterialTheme.typography.titleLarge)
        if (sessions.isEmpty()) EmptyMessage("No games completed yet.")
        sessions.forEach { session ->
            Text("${session.gameType.replace('_', ' ')} • ${session.scorePercent}% • Level ${session.difficulty}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AlertsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    EditorScaffold("Alerts", onBack) {
        if (alerts.isEmpty()) EmptyMessage("No local alerts.")
        alerts.forEach { alert ->
            Surface(color = if (alert.acknowledged) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(alert.title, style = MaterialTheme.typography.titleLarge)
                    Text(alert.message, style = MaterialTheme.typography.bodyLarge)
                    Text(SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(alert.createdAt)))
                    alert.latitude?.let { Text("Last known: $it, ${alert.longitude}") }
                }
            }
        }
    }
}

@Composable
private fun TimeFields(hour: String, onHour: (String) -> Unit, minute: String, onMinute: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(hour, { onHour(it.filter(Char::isDigit).take(2)) }, label = { Text("Hour 0–23") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        OutlinedTextField(minute, { onMinute(it.filter(Char::isDigit).take(2)) }, label = { Text("Minute") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EditorScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        ScreenHeader(title, onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) { item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) } }
    }
}
