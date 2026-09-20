# Offline-First Android Bug Tracker

An offline-first Android Bug Tracker built with Kotlin, Room, Retrofit, WorkManager, lifecycle-aware state management, and Git-based version control.

[![Academic Course](https://img.shields.io/badge/Course-CS%204405--01%20Mobile%20Applications-blue.svg)](https://www.uopeople.edu)
[![Assignment](https://img.shields.io/badge/Assignment-Unit%203%20Activity-brightgreen.svg)](https://www.uopeople.edu)
[![Student](https://img.shields.io/badge/Student-Abdul%20Wahid%20Chohan-orange.svg)](https://github.com)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-purple.svg)](https://kotlinlang.org)
[![Build Tool](https://img.shields.io/badge/Gradle-8.7-teal.svg)](https://gradle.org)

---

## 1. Academic Context

- **Course:** CS 4405-01 Mobile Applications
- **Assignment Activity:** Unit 3
- **Project Title:** Offline-First Bug Tracker
- **Student Name:** Abdul Wahid Chohan
- **Institution:** University of the People

This project was engineered to satisfy the academic and software engineering requirements of CS 4405-01 Unit 3. It demonstrates an architectural pattern where local data persistence (Room) functions as the single source of truth, network synchronization (Retrofit) happens asynchronously, and background job scheduling (WorkManager) guarantees reliable sync without blocking the UI thread or risking data loss during network interruptions.

---

## 2. GitHub Topics

```
android, kotlin, room-database, retrofit, workmanager, offline-first, mvvm, bug-tracker, android-development, mobile-application
```

---

## 3. Key Features

- **Uninterrupted Offline Operations:** Full CRUD capability (Create, Read, Update, Delete) without requiring an active internet connection.
- **Room as Local Source of Truth:** All mutations are recorded in SQLite via Room immediately; the user interface never waits on network latency.
- **Visual & Textual Synchronization Badges:** Clear status badges (`Pending Sync`, `Synchronized`, `Sync Failed`) ensure synchronization state is never communicated through color alone (adhering to WCAG accessibility principles).
- **Background Synchronization with WorkManager:** Asynchronous synchronization with network constraints (`NetworkType.CONNECTED`), exponential backoff retry (15 seconds base), and bounded retry limits.
- **Delete Tombstone Pattern:** Prevents deleted issues from "resurrecting" upon remote pull reconciliation.
- **Bidirectional Conflict Handling:** Protects pending local mutations from remote overwrite; applies newest-update-wins timestamp reconciliation for synchronized records.
- **Lifecycle & Process Death Survival:** `SavedStateHandle` preserves in-progress draft fields across screen rotations and Android OS process termination.
- **Comprehensive Error Model:** Strongly-typed `SyncResult` hierarchy categorizing Network, Server, Validation, and Client errors without exposing raw stack traces.

---

## 4. Technology Stack & Dependencies

| Component | Library / Technology | Version | Purpose |
|---|---|---|---|
| **Language** | Kotlin | `1.9.24` | Modern, null-safe language for Android development |
| **Persistence** | AndroidX Room | `2.6.1` | Local SQLite ORM and source of truth |
| **Annotation Processing** | KSP (Kotlin Symbol Processing) | `1.9.24-1.0.20` | Compile-time validation of Room entities and DAOs |
| **Remote Networking** | Retrofit | `2.11.0` | Type-safe REST client for remote CRUD operations |
| **JSON Parser** | Gson Converter | `2.11.0` | Serializes and deserializes network DTOs |
| **HTTP Client** | OkHttp Logging Interceptor | `4.12.0` | Inspects HTTP traffic for debugging |
| **Concurrency** | Kotlin Coroutines & Flow | `1.8.1` | Reactive streaming and asynchronous background execution |
| **Background Work** | AndroidX WorkManager | `2.9.0` | Deferrable, guaranteed background task synchronization |
| **Lifecycle & Architecture** | AndroidX Lifecycle & ViewModel | `2.8.4` | MVVM presentation architecture and state retention |
| **Process State** | SavedStateHandle | `2.8.4` | Preserves UI drafts across process death and orientation changes |
| **UI Components** | Google Material 3 Components | `1.12.0` | Accessible cards, text fields, buttons, and badges |
| **List Rendering** | RecyclerView + DiffUtil | Core AndroidX | Efficient item rendering and smooth list mutations |
| **Unit Testing** | JUnit 4 + Coroutines Test | `4.13.2` / `1.8.1` | Deterministic unit tests for mappers, repositories, and ViewModels |

---

## 5. Architectural Overview

The application is structured following the official **Android Jetpack Architecture Guide**, utilizing the **Model-View-ViewModel (MVVM)** pattern combined with the **Repository Pattern**:

```
[ User Interface ]
       │  ▲
       ▼  │ (StateFlow / SharedFlow)
[ ViewModels (with SavedStateHandle) ]
       │  ▲
       ▼  │ (Domain Calls / Flow)
[ IssueRepository ] ─── Enqueues ───► [ WorkManager (IssueSyncWorker) ]
       │          ╲                               │
       ▼           ▼                              ▼
[ Room Database ]  [ Retrofit REST API ] ◄────────┘
(Local Source of    (Remote Backend
     Truth)           Synchronization)
```

- **Presentation Layer (`ui/`):** Fragments and ViewModels. `IssueListViewModel` exposes a reactive `StateFlow<List<IssueEntity>>` collected by `IssueListFragment`. `IssueEditorViewModel` maintains user draft state via `SavedStateHandle`.
- **Repository Layer (`data/repository/`):** `IssueRepository` coordinates between local database transactions and remote API calls, enqueuing background work and enforcing conflict resolution.
- **Local Persistence Layer (`data/local/`):** `BugTrackerDatabase`, `IssueEntity`, `Converters`, and `IssueDao`.
- **Remote Networking Layer (`data/remote/`):** `IssueApi`, `IssueDto`, `IssueMapper`, and `RetrofitClient`.
- **Background Worker Layer (`worker/`):** `IssueSyncWorker` executes pending local operations and pulls remote updates whenever network connectivity is verified.

---

## 6. Project Directory Structure

```
app/src/main/java/com/uopeople/cs4405/bugtracker/
├── BugTrackerApplication.kt               # Application entry initializing AppContainer
├── MainActivity.kt                        # Host Activity managing Fragment navigation
├── data/
│   ├── local/
│   │   ├── BugTrackerDatabase.kt          # RoomDatabase definition
│   │   ├── Converters.kt                  # Enum TypeConverters (Priority, Status, SyncState)
│   │   ├── IssueDao.kt                    # Room SQL queries, Flow streams & merge transactions
│   │   └── IssueEntity.kt                 # Database table schema with UUID PK & sync tracking
│   ├── remote/
│   │   ├── IssueApi.kt                    # Retrofit endpoints (GET, POST, PUT, DELETE)
│   │   ├── IssueDto.kt                    # Network JSON payload data transfer object
│   │   ├── IssueMapper.kt                 # Entity <-> DTO translation with fallback handling
│   │   └── RetrofitClient.kt              # Centralized base URL configuration & OkHttp client
│   └── repository/
│       ├── IssueRepository.kt             # Offline-first coordinator
│       └── SyncResult.kt                  # Sealed interface for typed operation results
├── di/
│   └── AppContainer.kt                    # Lightweight Dependency Injection container
├── ui/
│   ├── list/
│   │   ├── IssueAdapter.kt                # RecyclerView ListAdapter with DiffUtil
│   │   ├── IssueListFragment.kt           # Fragment displaying active issues & swipe-to-refresh
│   │   └── IssueListViewModel.kt          # ViewModel observing Room Flow & handling deletes
│   └── editor/
│       ├── IssueEditorFragment.kt         # Create/Edit screen with accessible input fields
│       └── IssueEditorViewModel.kt        # ViewModel with SavedStateHandle draft preservation
└── worker/
    └── IssueSyncWorker.kt                 # CoroutineWorker with network constraint & retry policy
```

---

## 7. Offline-First Synchronization Workflow

### 1. Create Operation
1. The user inputs an issue title and description and selects a priority and status.
2. The title is validated. If blank, a validation error is surfaced immediately.
3. A UUID is generated locally (`UUID.randomUUID().toString()`).
4. The issue is saved in Room with `syncState = PENDING` and `operationType = CREATE`.
5. The local `Flow` updates the RecyclerView immediately; no spinner or network latency blocks the user.
6. A `OneTimeWorkRequest` is queued with WorkManager. When connectivity is available, `IssueSyncWorker` performs `POST /issues`.
7. Upon HTTP 200/201 response, Room marks the issue `syncState = SYNCED`, `operationType = NONE`.

### 2. Update Operation
1. The user edits an issue.
2. Room is updated immediately with `updatedAt = System.currentTimeMillis()`.
3. If the record was already pending creation (`CREATE`), it retains `operationType = CREATE` so that it sends a single clean `POST` upon reconnection. Otherwise, it transitions to `operationType = UPDATE` and `syncState = PENDING`.
4. WorkManager sync is enqueued to push `PUT /issues/{id}` upon network availability.

### 3. Delete Operation (Tombstone Pattern)
1. If an issue was created offline and deleted prior to any remote synchronization, it is purged from Room immediately.
2. If already synchronized, it is marked with `isDeleted = true`, `operationType = DELETE`, and `syncState = PENDING`.
3. Active DAO queries filter out deleted records (`WHERE isDeleted = 0`), hiding it from the user list.
4. When online, `IssueSyncWorker` issues `DELETE /issues/{id}`.
5. Only upon server confirmation (or 404 response indicating already gone) is the tombstone row purged from SQLite via `purgeTombstone(id)`.

### 4. Read & Pull Synchronization
1. The UI observes Room exclusively via `Flow<List<IssueEntity>>`.
2. Remote updates (`GET /issues`) are fetched in the background and merged into Room.
3. Pending local mutations are never overwritten by remote records.

---

## 8. Conflict Handling Strategy

```
                          ┌──────────────────────────┐
                          │ Incoming Remote Issue    │
                          └─────────────┬────────────┘
                                        │
                                        ▼
                         Does record exist in Room?
                                  /           \
                             No  /             \  Yes
                                ▼               ▼
                      Insert into Room      Is local record
                      as SYNCED             marked PENDING?
                                               /       \
                                         Yes  /         \  No
                                             ▼           ▼
                                   [PRESERVE LOCAL]   Is remote.updatedAt >=
                                   Do not overwrite    local.updatedAt?
                                   user's edits          /          \
                                                   Yes  /            \  No
                                                       ▼              ▼
                                                Update Room      Discard older
                                                with remote      remote record
```

---

## 9. Error Handling & State Preservation

### Strongly-Typed SyncResult Hierarchy
Errors are modeled as a closed algebraic data type rather than generic runtime exceptions:
- `SyncResult.Success<T>`: Carries successful payload.
- `SyncResult.NetworkError`: Identified by `IOException`; triggers WorkManager retry.
- `SyncResult.ServerError`: Identified by HTTP 5xx; triggers exponential backoff retry.
- `SyncResult.ValidationError`: User validation message (e.g., empty title); non-retryable.
- `SyncResult.AuthenticationError`: HTTP 401/403; non-retryable.
- `SyncResult.UnexpectedError`: Safe user-facing description preventing raw stack trace exposure.

### Lifecycle & Draft Preservation
- `SavedStateHandle` stores `key_title`, `key_description`, `key_priority`, `key_status`, and `key_issue_id`.
- If the user rotates the device or the operating system terminates the process while the editor is in the background, inputs are automatically restored.
- Submitted issues are written directly to SQLite before the draft state is cleared.

---

## 10. Building & Running the Application

### Prerequisites
- Java Development Kit: JDK 17 (Microsoft OpenJDK or Eclipse Temurin 17)
- Android SDK: API Level 34 (`platforms;android-34`, `build-tools;34.0.0`)
- Operating System: Windows, macOS, or Linux

### Command-Line Build Instructions (PowerShell / Windows)
```powershell
# Set JAVA_HOME and PATH if not globally configured
$env:JAVA_HOME = "C:\Users\Chohan PC\AppData\Local\Programs\jdk-17\jdk-17.0.12+7"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

# Clean the workspace
.\gradlew.bat clean

# Run the unit test suite
.\gradlew.bat testDebugUnitTest

# Assemble debug APK
.\gradlew.bat assembleDebug

# Run Android Lint analysis
.\gradlew.bat lintDebug
```

### Command-Line Build Instructions (Linux / macOS)
```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
```

---

## 11. Backend Configuration

The remote API endpoint is defined in a single centralized location:
- **File:** `app/src/main/java/com/uopeople/cs4405/bugtracker/data/remote/RetrofitClient.kt`
- **Current Placeholder:** `const val BASE_URL = "https://api.bugtracker.uopeople.internal/v1/"`

### Connecting to a Real or Local Staging Backend:
1. When testing against an Android Emulator connecting to a server running on your development machine, set:
   ```kotlin
   const val BASE_URL = "http://10.0.2.2:8080/v1/"
   ```
2. When testing against a physical device on the same local Wi-Fi network:
   ```kotlin
   const val BASE_URL = "http://192.168.x.x:8080/v1/"
   ```
3. To run completely offline or execute automated tests, the included `FakeIssueApi` simulates network latency and error conditions deterministically.

---

## 12. Testing Suite

The project includes unit and instrumentation test suites verifying core invariants:

1. **`IssueRepositoryTest.kt`:**
   - Offline create remains locally accessible with `PENDING` state.
   - Successful remote sync transitions state to `SYNCED`.
   - Network failure maintains `PENDING` state for WorkManager retry.
   - Successful deletion purges tombstone.
   - Failed deletion preserves tombstone.
   - Offline created-then-deleted records are purged immediately without sending invalid remote requests.
   - Conflict resolution: older remote records do not overwrite pending local data.
2. **`IssueEditorViewModelTest.kt`:**
   - Rejects blank issue titles with user-facing validation errors.
   - Persists valid issues to database.
   - Preserves draft state through `SavedStateHandle` across process recreation.
3. **`IssueMapperTest.kt`:**
   - Verifies lossless mapping between Room `IssueEntity` and network `IssueDto`.
   - Verifies graceful fallback to default enums when receiving unknown server strings.
4. **`IssueDaoInstrumentationTest.kt`:**
   - Verifies Room SQLite insert, update, soft-delete filtering, and tombstone purging on an active SQLite engine.

---

## 13. Git Workflow & Version Control Evidence

The repository adheres to professional Git hygiene:
- **Branches:**
  - `main`: Release-ready code.
  - `feature/offline-issue-sync`: Development of Room persistence, Retrofit client, repository, and WorkManager worker.
  - `hotfix/preserve-delete-tombstones`: Specific patch ensuring delete tombstones survive failed network sync.
- **Tags:**
  - `v1.0`: Annotated release tag marking stable offline CRUD and synchronization.

For the visual screenshot checklist and mapping to grading requirements, see:
- [`docs/IMPLEMENTATION_EVIDENCE.md`](docs/IMPLEMENTATION_EVIDENCE.md)
- [`docs/RUBRIC_MAPPING.md`](docs/RUBRIC_MAPPING.md)
- [`docs/ASSIGNMENT_NOTES.md`](docs/ASSIGNMENT_NOTES.md)

---

## 14. Current Implementation Status & Known Limitations

- **Implemented & Verified:**
  - Complete local Room persistence with type converters and reactive Flow.
  - Retrofit client, DTOs, and mapper with enum safety.
  - Offline-first repository with tombstone management and conflict protection.
  - WorkManager `IssueSyncWorker` with network constraints, exponential backoff, and bounded retries.
  - Material 3 XML UI with RecyclerView DiffUtil, ViewBinding, and SavedStateHandle draft restoration.
  - 17 unit tests covering repository invariants, ViewModels, and mappers.
- **Academic Demonstration & Backend Statement:**
  > Room-based offline CRUD, lifecycle-state restoration, synchronization scheduling, and synchronization decision logic were implemented and verified through compilation and automated tests. Retrofit defines bidirectional CRUD endpoints, while remote behaviors were tested with a fake API. Because no live backend was deployed, production HTTP synchronization was not claimed as live-tested.
  - Remote backend uses a documented placeholder URL (`https://api.bugtracker.uopeople.internal/v1/`); live demonstration relies on `FakeIssueApi` unless configured with a live server URL.
  - Conflict resolution uses timestamp comparison; multi-device production systems would require server-assigned monotonic version counters or vector clocks.

---

## 15. Author

**Abdul Wahid Chohan**  
Course: CS 4405-01 Mobile Applications  
Assignment: Unit 3 Assignment Activity – Offline-First Bug Tracker  
University of the People  
