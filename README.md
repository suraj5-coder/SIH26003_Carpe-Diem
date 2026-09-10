# MindMate Android

MindMate is an offline-first Android application for elderly dementia patients and their caretakers, designed with North Eastern India in mind.

> Game scores are performance and engagement indicators only. MindMate does not diagnose dementia, determine medical severity, or confirm that medicine was or was not taken.

## MVP capabilities

- Patient/caretaker role selection with separate interfaces
- Calm, high-contrast patient home with large actions and long-press SOS
- English, Hindi, and Assamese patient-home labels and offline TTS when a device voice is installed
- Local patient profile, family, food, songs, familiar places, routine, medicine, reminder, hydration, exercise, geofence, alert, and game data
- Family, food, memory tray, number, and routine games
- Rule-based local difficulty decisions (`>85%` increase, `60–85%` hold, `<60%` reduce)
- CameraX prescription capture and bundled ML Kit OCR
- Mandatory caretaker review before medicine save or reminder scheduling
- AlarmManager reminders restored after boot/time changes
- Medication notification actions: `TAKEN`, `REMIND ME`, and `NEED HELP`
- Carefully worded delayed local alert: “Medicine intake not confirmed”
- Android geofencing without continuous GPS polling
- Local SOS event log with alarm and system dialer/SMS handoff
- Keystore-backed AES-GCM vault for future biometric embeddings and small sensitive values
- Room outbox (`sync_queue`) ready for optional connectivity-based synchronization

The first database launch includes clearly editable sample patient, family, food, and routine content. No sample alarm, geofence, SMS, or call is activated automatically.

## Offline architecture

```text
Compose UI → ViewModel → Repository → Room
                         ├→ AlarmManager / WorkManager
                         ├→ Android Geofencing
                         ├→ Android Keystore
                         └→ Lazy, feature-scoped on-device capabilities

Room → SyncQueue → optional Firebase adapter (not included in base APK)
```

Room is the source of truth. Patient features never wait for a network request. Firebase is intentionally absent from the base build and can only be added as an optional outbox consumer.

## Build

Requirements:

- Android Studio with JDK 17
- Android SDK 35
- Min SDK 26 device or emulator

```bash
./gradlew test
./gradlew assembleDebug
```

Open the root directory in Android Studio and let it create `local.properties` for your SDK path.

## Important platform behavior

- Android 13+ requires notification permission.
- Exact medicine alarms depend on the device’s exact-alarm permission. MindMate falls back to an allow-while-idle alarm rather than disabling reminders.
- Reliable geofence exits require fine and background location permission. Geofence delivery is OS-managed and may be delayed.
- SOS opens an approved system dialer or SMS composer; the app does not claim that contact succeeded.
- Device TTS is used only when an installed, non-network voice is available.

## Model status

The project does **not** fake model output:

| Capability | MVP status |
|---|---|
| Prescription OCR | Working, bundled on-device ML Kit Latin/Devanagari |
| Family photo-label games | Working without recognition |
| MediaPipe + MobileFaceNet identity embedding | Interface only; validated/licensed model required |
| AI4Bharat IndicConformer + VAD | Language-pack interface only |
| YIN/chroma extraction | Interface only |
| Dynamic Time Warping | Implemented and unit tested |
| Firebase synchronization | Outbox schema only; optional adapter not included |

See `docs/MODEL_INTEGRATION.md` before adding model assets.

## Privacy defaults

- No cleartext traffic
- Database and secure preferences excluded from cloud backup
- Face data never uploaded by default
- Selected photo/audio document permissions remain local
- No analytics, advertising, cloud AI, or Firebase SDK
- Release builds enable code and resource shrinking

## Project layout

```text
app/src/main/java/org/mindmate/app/
├── ai/                 # OCR plus honest model boundaries
├── caregiver/          # Local dashboard/editors
├── data/local/         # Room entities, DAO, database
├── data/repository/    # Offline-first repository and seed data
├── games/              # Local games
├── geofence/           # Safety-zone registration/events
├── medicine/           # CameraX + OCR confirmation flow
├── patient/            # Elderly-friendly home
├── reminders/          # Alarm and WorkManager engine
├── security/           # Android Keystore vault
├── sos/                # Local SOS actions
└── ui/                 # Theme, navigation, components
```
