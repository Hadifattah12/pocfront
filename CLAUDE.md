# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Android App
```bash
./gradlew :app:assembleDebug                  # build debug APK
./gradlew :app:testDebugUnitTest              # run unit tests
./gradlew :app:connectedDebugAndroidTest      # run instrumented tests (device/emulator required)
./gradlew :app:lint                           # run lint
```

### Spring Boot Backend (`test1/`)
```bash
cd test1 && ./mvnw spring-boot:run   # start server (requires MySQL at localhost:3306/communication)
cd test1 && ./mvnw test              # run backend tests
cd test1 && ./mvnw package           # build JAR
```

## Architecture

Two independent components:

**Android App** (`app/`) — Kotlin, Jetpack Compose, Room, WorkManager, Retrofit

**Spring Boot Backend** (`test1/`) — Java, Spring Data JPA, MySQL

### Android Data Flow

```
ContactReader → ContactSyncRepository → ContactSyncWorker → Retrofit → Backend
     ↑                   ↓                      ↓
device contacts    delta computation      PendingSyncChunk
                   Room snapshot          (retry loop)
```

1. `ContactReader` reads raw device contacts from Android's ContactsProvider
2. `ContactSyncRepository` diffs device contacts against the local Room snapshot (`ContactSnapshot`) to produce added/updated/deleted sets
3. `ContactSyncWorker` (WorkManager) chunks the delta (~100 contacts/chunk), uploads each chunk via Retrofit, and retries failed chunks with 2s delays until the backend returns `"SUCCESS"`
4. Failed chunks are persisted in `PendingSyncChunk` (Room) so they survive process death

### Key Android Packages

| Package | Role |
|---|---|
| `ui/` | Compose screens: `SyncScreen`, `ConsentDialog` |
| `network/` | Retrofit `ContactSyncApiService`, DTOs, OkHttp client |
| `repository/ContactSyncRepository` | Delta computation, Room snapshot management |
| `worker/ContactSyncWorker` | Sync orchestration, chunk retry loop |
| `data/` | Room entities (`ContactSnapshot`, `PendingSyncChunk`), DAO, JSON type converters |

### Key Backend Packages

| Package | Role |
|---|---|
| `controllers/ContactSyncController` | REST endpoints for sessions and chunks |
| `Services/ContactSyncService` | Session lifecycle, chunk validation, deduplication |
| `models/` | JPA entities: `UserContact`, `SyncSession` |
| `enums/` | `SyncType` (INITIAL/INCREMENTAL), `SyncSessionStatus`, `ContactStatus` |
| `utils/` | `PhoneNumberUtils` (libphonenumber normalization), `HashUtils` (HMAC-SHA256) |

### Backend REST API

All requests require `X-User-Id` header for multi-tenancy.

| Method | Path | Purpose |
|---|---|---|
| POST | `/contact-sync/sessions` | Create sync session (idempotent via `clientSyncId`) |
| POST | `/contact-sync/sessions/{id}/chunks` | Upload a contact chunk |
| GET | `/contact-sync/sessions/{id}/status` | Poll sync progress |

## Critical Design Decisions

- **Chunking**: Additions, updates, and deletions are chunked independently (~100 each); chunk indices must be sequential per sync type
- **Idempotency**: Session creation is deduplicated by `clientSyncId`; duplicate chunk indices return cached responses
- **Phone hashing**: Backend normalizes phone numbers via libphonenumber then HMAC-SHA256 hashes them (key in `application.yml`)
- **Room migrations**: Currently use destructive fallback (drops all tables on schema change) — marked TODO to fix before production

## Dev Environment Notes

- Android emulator reaches host machine at `http://10.0.2.2:8080/` — hardcoded in `ApiClient.kt`
- `usesCleartextTraffic=true` in `AndroidManifest.xml` — development only
- MySQL credentials and hash secret are hardcoded in `test1/src/main/resources/application.yml` — not production-safe
- `TestList.kt` and `test.kt` at the repo root are scratch files, not production code
