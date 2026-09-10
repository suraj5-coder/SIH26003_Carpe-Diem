package org.mindmate.app.medicine

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import org.mindmate.app.MainViewModel
import org.mindmate.app.ai.ocr.PrescriptionOcr
import org.mindmate.app.domain.PrescriptionDraft
import org.mindmate.app.ui.components.EmptyMessage
import org.mindmate.app.ui.components.LargeActionButton
import org.mindmate.app.ui.components.ScreenHeader
import java.io.File

@Composable
fun PrescriptionCameraScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val patient by viewModel.patient.collectAsStateWithLifecycle()
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var hasCamera by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(PrescriptionDraft()) }
    var status by remember { mutableStateOf("Take a clear photo, then review every field.") }
    var hour by remember { mutableStateOf("8") }
    var minute by remember { mutableStateOf("30") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasCamera = it }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        ScreenHeader("Medicines", onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (medicines.isNotEmpty()) {
                item { Text("Confirmed medicines", style = MaterialTheme.typography.titleLarge) }
                items(medicines.size) { index ->
                    val medicine = medicines[index]
                    Text("• ${medicine.name} ${medicine.dose} — ${medicine.frequency}", style = MaterialTheme.typography.bodyLarge)
                }
                item { HorizontalDivider() }
            }
            item {
                Text("Scan prescription", style = MaterialTheme.typography.titleLarge)
                Text("OCR runs on this device. It can make mistakes.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                if (!hasCamera) {
                    LargeActionButton("ALLOW CAMERA", { permission.launch(Manifest.permission.CAMERA) }, Modifier.fillMaxWidth())
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView ->
                                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                                providerFuture.addListener({
                                    val provider = providerFuture.get()
                                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                    val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                                    imageCapture = capture
                                    runCatching {
                                        provider.unbindAll()
                                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                                    }.onFailure { status = "Camera could not start: ${it.message}" }
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                    )
                }
            }
            item {
                LargeActionButton("TAKE PHOTO AND READ", {
                    val capture = imageCapture ?: return@LargeActionButton
                    val directory = File(context.filesDir, "prescriptions").apply { mkdirs() }
                    val file = File(directory, "prescription-${System.currentTimeMillis()}.jpg")
                    status = "Reading prescription on this device…"
                    capture.takePicture(
                        ImageCapture.OutputFileOptions.Builder(file).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val uri = Uri.fromFile(file)
                                imageUri = uri.toString()
                                scope.launch {
                                    val ocr = PrescriptionOcr(patient?.preferredLanguage ?: "en")
                                    val result = runCatching { InputImage.fromFilePath(context, uri) }
                                        .fold(
                                            onSuccess = { ocr.recognize(it) },
                                            onFailure = { Result.failure(it) },
                                        )
                                    ocr.close()
                                    result.onSuccess { draft = it; status = "Review and correct the extracted information." }
                                        .onFailure { status = "Could not read this photo: ${it.message}" }
                                }
                            }
                            override fun onError(exception: ImageCaptureException) {
                                status = "Photo failed: ${exception.message}"
                            }
                        },
                    )
                }, Modifier.fillMaxWidth(), enabled = hasCamera && imageCapture != null)
            }
            item { EmptyMessage(status) }
            item { MedicineField("Medicine", draft.medicineName) { draft = draft.copy(medicineName = it) } }
            item { MedicineField("Dose", draft.dose) { draft = draft.copy(dose = it) } }
            item { MedicineField("Frequency", draft.frequency) { draft = draft.copy(frequency = it) } }
            item { MedicineField("Duration", draft.duration) { draft = draft.copy(duration = it) } }
            item {
                Text("Reminder time", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, label = { Text("Hour") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(minute, { minute = it.filter(Char::isDigit).take(2) }, label = { Text("Minute") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
            item {
                LargeActionButton("CONFIRM AND SCHEDULE", {
                    if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    viewModel.confirmMedicine(draft, imageUri, (hour.toIntOrNull() ?: 8).coerceIn(0, 23), (minute.toIntOrNull() ?: 30).coerceIn(0, 59))
                    status = "Medicine confirmed and local reminder scheduled."
                    draft = PrescriptionDraft()
                    imageUri = null
                }, Modifier.fillMaxWidth(), enabled = draft.medicineName.isNotBlank())
            }
            item { Text("MindMate never schedules medicine directly from OCR. Caretaker confirmation is required.", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun MedicineField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyLarge)
}
