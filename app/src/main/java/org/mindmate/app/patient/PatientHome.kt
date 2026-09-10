package org.mindmate.app.patient

import android.Manifest
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.mindmate.app.MainViewModel
import org.mindmate.app.domain.GameType
import org.mindmate.app.ui.components.LargeActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PatientHome(
    viewModel: MainViewModel,
    onGames: () -> Unit,
    onGame: (GameType) -> Unit,
    onSwitchRole: () -> Unit,
) {
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val hydration by viewModel.hydrationToday.collectAsStateWithLifecycle()
    val exercise by viewModel.exerciseToday.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var feedback by remember { mutableStateOf("") }
    val copy = PatientCopy.forLanguage(patient?.preferredLanguage ?: "en")
    val speak = rememberVoicePrompt(patient?.preferredLanguage ?: "en")
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${copy.greeting}, ${patient?.name ?: copy.friend}", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onSwitchRole) { Text(copy.changeUser) }
        }
        Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now)), style = MaterialTheme.typography.headlineLarge)
        if (feedback.isNotBlank()) {
            Text(feedback, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        LargeActionButton("▶  ${copy.play}", onGames, Modifier.fillMaxWidth().heightIn(min = 88.dp))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LargeActionButton("♫\n${copy.music}", { onGame(GameType.SONG) }, Modifier.fillMaxWidth().weight(1f))
                LargeActionButton("💊\n${copy.medicine}", {
                    feedback = "Your reminder will tell you when it is time."
                    speak(feedback)
                }, Modifier.fillMaxWidth().weight(1f))
                LargeActionButton("💧\n${copy.water}\n$hydration / 6", {
                    viewModel.drinkWater()
                    feedback = "Wonderful! Water recorded."
                    speak(feedback)
                }, Modifier.fillMaxWidth().weight(1f))
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LargeActionButton("🚶\n${copy.walk}\n$exercise / 10 min", {
                    viewModel.recordWalk()
                    feedback = "Great! Walk recorded."
                    speak(feedback)
                }, Modifier.fillMaxWidth().weight(1f))
                LargeActionButton("♥\n${copy.family}", { onGame(GameType.FAMILY) }, Modifier.fillMaxWidth().weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .semantics { contentDescription = "SOS. Press and hold to activate." }
                        .combinedClickable(
                            onClick = { feedback = "Press and hold SOS to activate."; speak(feedback) },
                            onLongClick = { feedback = "SOS alarm activated."; speak(feedback); viewModel.triggerSos() },
                        ),
                ) {
                    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("SOS\nPRESS & HOLD", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private data class PatientCopy(
    val greeting: String,
    val friend: String,
    val changeUser: String,
    val play: String,
    val music: String,
    val medicine: String,
    val water: String,
    val walk: String,
    val family: String,
) {
    companion object {
        fun forLanguage(code: String) = when (code) {
            "hi" -> PatientCopy("नमस्ते", "दोस्त", "उपयोगकर्ता", "खेलें", "संगीत", "दवाई", "पानी", "चलें", "परिवार")
            "as" -> PatientCopy("নমস্কাৰ", "বন্ধু", "ব্যৱহাৰকাৰী", "খেলক", "সংগীত", "ঔষধ", "পানী", "খোজ কাঢ়ক", "পৰিয়াল")
            else -> PatientCopy("Hello", "friend", "Change user", "PLAY", "MUSIC", "MEDICINE", "WATER", "WALK", "FAMILY")
        }
    }
}

@Composable
private fun rememberVoicePrompt(languageCode: String): (String) -> Unit {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context, languageCode) {
        val tts = TextToSpeech(context.applicationContext) { status -> ready = status == TextToSpeech.SUCCESS }
        engine = tts
        onDispose { tts.stop(); tts.shutdown(); engine = null }
    }
    return remember(ready, languageCode, engine) {
        { text ->
            engine?.takeIf { ready }?.let { tts ->
                val locale = when (languageCode) {
                    "hi" -> Locale("hi", "IN")
                    "as" -> Locale("as", "IN")
                    else -> Locale.ENGLISH
                }
                if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = locale
                    if (Build.VERSION.SDK_INT < 21 || tts.voice?.isNetworkConnectionRequired != true) {
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mindmate_prompt")
                    }
                }
            }
        }
    }
}
