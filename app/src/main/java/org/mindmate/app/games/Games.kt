package org.mindmate.app.games

import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.mindmate.app.MainViewModel
import org.mindmate.app.domain.GameType
import org.mindmate.app.ui.components.EmptyMessage
import org.mindmate.app.ui.components.LargeActionButton
import org.mindmate.app.ui.components.LocalImage
import org.mindmate.app.ui.components.ScreenHeader
import java.util.Locale
import kotlin.random.Random

@Composable
fun GameHub(onBack: () -> Unit, onGame: (GameType) -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        ScreenHeader("Choose a game", onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(GameType.entries.size) { index ->
                val game = GameType.entries[index]
                LargeActionButton(game.title.uppercase(), { onGame(game) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun GameScreen(type: GameType, viewModel: MainViewModel, onBack: () -> Unit) {
    when (type) {
        GameType.FAMILY -> FamilyGame(viewModel, onBack)
        GameType.FOOD -> FoodGame(viewModel, onBack)
        GameType.MEMORY_TRAY -> MemoryTrayGame(viewModel, onBack)
        GameType.NUMBER -> NumberGame(viewModel, onBack)
        GameType.ROUTINE -> RoutineGame(viewModel, onBack)
        GameType.SONG -> SongGame(viewModel, onBack)
    }
}

@Composable
private fun FamilyGame(viewModel: MainViewModel, onBack: () -> Unit) {
    val family by viewModel.family.collectAsStateWithLifecycle()
    val startedAt = remember { System.currentTimeMillis() }
    val target = remember(family) { family.randomOrNull() }
    val choices = remember(family, target) { family.shuffled().take(4) }
    GameLayout("Who is this?", onBack) {
        if (target == null || choices.size < 2) {
            EmptyMessage("The caretaker needs to add at least two family members first.")
        } else {
            LocalImage(target.photoUri, target.name, Modifier.fillMaxWidth().height(250.dp))
            Text("Choose the name", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            AnswerChoices(choices.map { it.name }) { answer ->
                val correct = answer == target.name
                viewModel.recordGame(GameType.FAMILY, 1, if (correct) 100 else 0, if (correct) 1 else 0, if (correct) 0 else 1, startedAt)
                correct
            }
        }
    }
}

@Composable
private fun FoodGame(viewModel: MainViewModel, onBack: () -> Unit) {
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val startedAt = remember { System.currentTimeMillis() }
    val target = remember(foods) { foods.randomOrNull() }
    val choices = remember(foods, target) { foods.shuffled().take(4) }
    GameLayout("Food memory", onBack) {
        if (target == null || choices.size < 2) EmptyMessage("The caretaker needs to add at least two favourite foods first.")
        else {
            Text("Find ${target.name}", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            choices.forEach { food ->
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 94.dp),
                    contentPadding = PaddingValues(10.dp),
                ) {
                    LocalImage(food.imageUri, food.name, Modifier.size(70.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(food.name, style = MaterialTheme.typography.titleLarge)
                }
            }
            FoodAnswerOverlay(choices.map { it.name }, target.name, startedAt, viewModel)
        }
    }
}

@Composable
private fun FoodAnswerOverlay(choices: List<String>, target: String, startedAt: Long, viewModel: MainViewModel) {
    var answered by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tap the matching label:", style = MaterialTheme.typography.bodyLarge)
        choices.forEach { value ->
            LargeActionButton(value, {
                if (answered == null) {
                    answered = value
                    val correct = value == target
                    viewModel.recordGame(GameType.FOOD, 1, if (correct) 100 else 0, if (correct) 1 else 0, if (correct) 0 else 1, startedAt)
                }
            }, Modifier.fillMaxWidth(), enabled = answered == null)
        }
        answered?.let { FriendlyFeedback(it == target) }
    }
}

@Composable
private fun MemoryTrayGame(viewModel: MainViewModel, onBack: () -> Unit) {
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val options = remember(foods) { foods.map { it.name }.distinct().ifEmpty { listOf("Cup", "Key", "Apple", "Book", "Flower", "Spoon") } }
    val shown = remember(options) { options.shuffled().take(3.coerceAtMost(options.size)) }
    var phase by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val startedAt = remember { System.currentTimeMillis() }
    LaunchedEffect(phase) { if (phase == 0) { delay(4_000); phase = 1 } }
    GameLayout("Memory tray", onBack) {
        when (phase) {
            0 -> {
                Text("Remember these", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                shown.forEach { LargeActionButton(it, {}, Modifier.fillMaxWidth(), enabled = false) }
            }
            1 -> {
                Text("Which ones did you see?", style = MaterialTheme.typography.headlineMedium)
                options.shuffled().take(6).forEach { value ->
                    FilterChip(
                        selected = value in selected,
                        onClick = { selected = if (value in selected) selected - value else selected + value },
                        label = { Text(value, style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                    )
                }
                LargeActionButton("DONE", {
                    val correct = selected.count { it in shown }
                    val mistakes = selected.count { it !in shown } + shown.count { it !in selected }
                    val score = ((correct.toFloat() / shown.size) * 100).toInt().coerceIn(0, 100)
                    viewModel.recordGame(GameType.MEMORY_TRAY, 1, score, correct, mistakes, startedAt)
                    phase = 2
                }, Modifier.fillMaxWidth())
            }
            else -> FriendlyFeedback(selected.containsAll(shown))
        }
    }
}

@Composable
private fun NumberGame(viewModel: MainViewModel, onBack: () -> Unit) {
    val expected = remember { List(2) { Random.nextInt(0, 10) }.joinToString("") }
    var visible by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    var complete by remember { mutableStateOf(false) }
    val startedAt = remember { System.currentTimeMillis() }
    val speak = rememberSpeech()
    LaunchedEffect(Unit) { speak("Remember: ${expected.toCharArray().joinToString(" ")}"); delay(4_000); visible = false }
    GameLayout("Number memory", onBack) {
        if (complete) FriendlyFeedback(input == expected)
        else if (visible) {
            Text("Remember", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(expected.toCharArray().joinToString("  "), style = MaterialTheme.typography.displayLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            LargeActionButton("SPEAK AGAIN", { speak("Remember: ${expected.toCharArray().joinToString(" ")}") }, Modifier.fillMaxWidth())
        } else {
            Text(if (input.isBlank()) "Enter the number" else input, style = MaterialTheme.typography.displayLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "✓")).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        LargeActionButton(key, {
                            when (key) {
                                "⌫" -> input = input.dropLast(1)
                                "✓" -> if (input.length == expected.length) {
                                    complete = true
                                    val correct = input == expected
                                    viewModel.recordGame(GameType.NUMBER, 1, if (correct) 100 else 0, if (correct) expected.length else 0, if (correct) 0 else 1, startedAt)
                                }
                                else -> if (input.length < expected.length) input += key
                            }
                        }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineGame(viewModel: MainViewModel, onBack: () -> Unit) {
    val routine by viewModel.routine.collectAsStateWithLifecycle()
    val ordered = remember(routine) { routine.sortedBy { it.minutesOfDay } }
    val index = remember(ordered) { if (ordered.size > 1) Random.nextInt(0, ordered.lastIndex) else 0 }
    val expected = ordered.getOrNull(index + 1)
    val choices = remember(ordered, expected) { ordered.shuffled().take(4).map { it.title } }
    val startedAt = remember { System.currentTimeMillis() }
    GameLayout("Daily routine", onBack) {
        if (expected == null) EmptyMessage("The caretaker needs to add at least two routine activities first.")
        else {
            Text("What comes after ${ordered[index].title}?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            AnswerChoices(choices) { answer ->
                val correct = answer == expected.title
                viewModel.recordGame(GameType.ROUTINE, 1, if (correct) 100 else 0, if (correct) 1 else 0, if (correct) 0 else 1, startedAt)
                correct
            }
        }
    }
}

@Composable
private fun SongGame(viewModel: MainViewModel, onBack: () -> Unit) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { player?.release() } }
    GameLayout("Finish the song", onBack) {
        if (songs.isEmpty()) EmptyMessage("The caretaker can add a meaningful song from the dashboard.")
        songs.forEach { song ->
            LargeActionButton("PLAY ${song.name.uppercase()}", {
                player?.release()
                player = song.audioUri?.let { uri ->
                    runCatching { MediaPlayer.create(context, Uri.parse(uri))?.apply { start() } }.getOrNull()
                }
            }, Modifier.fillMaxWidth(), enabled = song.audioUri != null)
        }
        EmptyMessage("Singing evaluation is not enabled in this build. A tested YIN/pYIN + chroma extractor must be integrated before a real game-performance score can be calculated.")
        Text("No clinical score is calculated.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AnswerChoices(choices: List<String>, onAnswer: (String) -> Boolean) {
    var result by remember { mutableStateOf<Boolean?>(null) }
    choices.forEach { choice ->
        LargeActionButton(choice, { if (result == null) result = onAnswer(choice) }, Modifier.fillMaxWidth(), enabled = result == null)
    }
    result?.let { FriendlyFeedback(it) }
}

@Composable
private fun FriendlyFeedback(correct: Boolean) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Text(
            if (correct) "Wonderful!\nGreat memory!" else "Good try!\nLet's try another one.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun GameLayout(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        ScreenHeader(title, onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) { item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) } }
    }
}

@Composable
private fun rememberSpeech(): (String) -> Unit {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context.applicationContext) { ready = it == TextToSpeech.SUCCESS }
        tts = engine
        onDispose { engine.stop(); engine.shutdown(); tts = null }
    }
    return remember(ready, tts) {{ text -> if (ready) { tts?.language = Locale.ENGLISH; tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "number_game") } }}
}
