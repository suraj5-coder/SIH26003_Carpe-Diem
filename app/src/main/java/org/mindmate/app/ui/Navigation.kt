package org.mindmate.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mindmate.app.MainViewModel
import org.mindmate.app.caregiver.CaregiverDashboard
import org.mindmate.app.caregiver.CaregiverSection
import org.mindmate.app.caregiver.CaregiverSectionScreen
import org.mindmate.app.domain.GameType
import org.mindmate.app.domain.UserRole
import org.mindmate.app.games.GameHub
import org.mindmate.app.games.GameScreen
import org.mindmate.app.patient.PatientHome
import org.mindmate.app.ui.components.LargeActionButton

enum class RootScreen { HOME, GAMES, GAME, CAREGIVER_SECTION }

@Composable
fun MindMateRoot(viewModel: MainViewModel) {
    val role by viewModel.activeRole.collectAsStateWithLifecycle()
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    var screen by rememberSaveable(role) { mutableStateOf(RootScreen.HOME) }
    var selectedGame by rememberSaveable { mutableStateOf(GameType.FAMILY) }
    var caregiverSection by rememberSaveable { mutableStateOf(CaregiverSection.PROFILE) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (role) {
            null -> RoleSelection(viewModel)
            UserRole.PATIENT -> when (screen) {
                RootScreen.HOME -> PatientHome(
                    viewModel = viewModel,
                    onGames = { screen = RootScreen.GAMES },
                    onGame = { selectedGame = it; screen = RootScreen.GAME },
                    onSwitchRole = { viewModel.selectRole(null) },
                )
                RootScreen.GAMES -> GameHub(
                    onBack = { screen = RootScreen.HOME },
                    onGame = { selectedGame = it; screen = RootScreen.GAME },
                )
                RootScreen.GAME -> GameScreen(selectedGame, viewModel) { screen = RootScreen.GAMES }
                RootScreen.CAREGIVER_SECTION -> Unit
            }
            UserRole.CAREGIVER -> when (screen) {
                RootScreen.HOME -> CaregiverDashboard(
                    patient = patient,
                    viewModel = viewModel,
                    onSection = { caregiverSection = it; screen = RootScreen.CAREGIVER_SECTION },
                    onSwitchRole = { viewModel.selectRole(null) },
                )
                RootScreen.CAREGIVER_SECTION -> CaregiverSectionScreen(caregiverSection, viewModel) { screen = RootScreen.HOME }
                RootScreen.GAMES, RootScreen.GAME -> Unit
            }
        }
    }
}

@Composable
private fun RoleSelection(viewModel: MainViewModel) {
    var caretakerRequested by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val hasPin = remember(caretakerRequested) { viewModel.hasCaregiverPin() }
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("MindMate", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        if (!caretakerRequested) {
            Text("Who is using the app?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(36.dp))
            LargeActionButton("PATIENT", { viewModel.selectRole(UserRole.PATIENT) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            LargeActionButton("CARETAKER", { caretakerRequested = true }, Modifier.fillMaxWidth())
        } else {
            Text(if (hasPin) "Enter caretaker PIN" else "Create a caretaker PIN", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8); error = "" },
                label = { Text("4–8 digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge,
            )
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            LargeActionButton(if (hasPin) "UNLOCK" else "SAVE PIN", {
                val accepted = if (hasPin) viewModel.verifyCaregiverPin(pin) else viewModel.setCaregiverPin(pin)
                if (accepted) viewModel.selectRole(UserRole.CAREGIVER) else error = if (hasPin) "Incorrect PIN" else "Use 4–8 numbers"
                pin = ""
            }, Modifier.fillMaxWidth(), enabled = pin.length >= 4)
            Spacer(Modifier.height(12.dp))
            LargeActionButton("BACK", { caretakerRequested = false; pin = ""; error = "" }, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(28.dp))
        Text("Works offline • Information stays on this device", style = MaterialTheme.typography.bodyMedium)
    }
}
