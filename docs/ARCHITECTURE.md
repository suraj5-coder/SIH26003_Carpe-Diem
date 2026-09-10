# Architecture and continuation notes

## Source of truth

Room is authoritative. UI state is exposed as `Flow` from `MindMateRepository`; platform callbacks also write to Room. This keeps patient behavior deterministic when the network is unavailable.

## Optional synchronization

Implement synchronization as a separate adapter that consumes `SyncQueueEntity` records only when a validated network is available. A Firebase implementation must:

- be optional at build/runtime,
- never block local writes or patient navigation,
- use authenticated per-patient authorization,
- exclude photographs and biometric data by default,
- use idempotent record IDs and conflict metadata,
- retry with bounded exponential backoff,
- preserve failed items for later review,
- avoid claiming a remote alert was delivered without acknowledgement.

Do not read Firebase directly from Compose screens.

## Reminder lifecycle

1. Caretaker saves a confirmed record to Room.
2. `ReminderScheduler` creates a stable `PendingIntent`.
3. `ReminderReceiver` displays a local notification.
4. Medicine reminders create a local `NOT_CONFIRMED` intake record.
5. Notification actions update that record.
6. `MedicineConfirmationWorker` creates a local alert only if it is still not confirmed after the grace period.
7. `SystemEventReceiver` restores enabled reminders after reboot, app replacement, timezone, or clock changes.

Add a settings UI for the grace period before making it configurable; the MVP uses 30 minutes.

## Geofence lifecycle

The caretaker stores one circular safe area, grants fine/background location, and activates it. Google Play services manages boundary detection; the app does not poll GPS. Exit events are stored as `AlertEntity` records with last-known coordinates when available.

The coordinate/radius preview intentionally avoids an online map dependency. A future offline map module should use separately downloaded regional tiles with clear storage controls.

## Data evolution

The database is schema version 1 and exports JSON schemas to `app/schemas`. Before release 2:

- add explicit Room migrations,
- add migration instrumentation tests,
- define retention for detailed events and prescription images,
- provide caretaker export/delete controls,
- evaluate full-database encryption for the deployment threat model.

`LocalVault` is suitable for small sensitive blobs and secrets; it is not full Room database encryption.

## Known MVP boundaries

- Same-device caretaker dashboard only
- One patient profile and one primary caretaker
- Circular safe zone configured by coordinates
- Rule-based difficulty decisions are persisted by skill; game rounds currently start at their base level and should next read the persisted recommendation
- Family and food games use caretaker labels
- Song audio plays locally, but singing capture/scoring remains disabled until real DSP is integrated
- Firebase, remote monitoring, and push delivery are not included
