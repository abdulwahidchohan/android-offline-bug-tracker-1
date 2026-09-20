# Academic Assignment Notes & Oral Defense Guide

**Course:** CS 4405-01 Mobile Applications  
**Assignment:** Unit 3 Assignment Activity – Offline-First Bug Tracker  
**Student:** Abdul Wahid Chohan  
**Institution:** University of the People  

---

### 1. Why Room is Used Instead of Direct SQLite APIs

Direct usage of Android's legacy `SQLiteOpenHelper` and raw SQLite APIs introduces significant development risks and maintenance overhead:
- **Compile-Time Verification:** Raw SQLite queries are treated as plain strings. Syntax errors, typos in table or column names, and mismatched data types are only detected at runtime when a query executes, leading to application crashes in production. Room verifies all SQL queries at compile time via KSP/annotation processing.
- **Boilerplate Reduction:** Direct SQLite requires tedious manual mapping between `Cursor` objects and Kotlin data models. Room automates entity serialization, deserialization, and TypeConverters, reducing thousands of lines of fragile boilerplate code.
- **Reactive Streaming Integration:** Raw SQLite does not provide reactive observation out of the box. Room integrates natively with Kotlin Coroutines `Flow`, emitting new query results automatically whenever the underlying database table is modified.

---

### 2. Responsibilities of Entity, DAO, and Database

- **`IssueEntity` (Data Model):** Represents a persistent relational table (`issues`) in SQLite. Defines table schema, primary keys (`UUID`), data types, and synchronization tracking fields (`syncState`, `operationType`, `isDeleted`, `updatedAt`).
- **`IssueDao` (Data Access Object):** Serves as the clean abstraction barrier between relational tables and Kotlin code. Defines all SQL queries (`@Query`, `@Insert`, `@Update`), specifies conflict algorithms, exposes reactive `Flow<List<IssueEntity>>` streams, and encapsulates multi-operation atomic transactions (`@Transaction`).
- **`BugTrackerDatabase` (Database Container):** Serves as the database connection holder extending `RoomDatabase`. Defines the schema version, registers entities, binds `TypeConverters`, and provides thread-safe singleton access to prevent connection leaks or database locking contention.

---

### 3. How All CRUD Operations Work Offline

The application adopts Room as the single, authoritative source of truth. Every CRUD operation is committed to Room first:
- **CREATE:**
  1. A UUID is generated locally on the device (`UUID.randomUUID().toString()`).
  2. The issue is persisted immediately into Room with `syncState = PENDING` and `operationType = CREATE`.
  3. The local `Flow` immediately updates the UI without waiting for network connectivity.
  4. Background synchronization is enqueued with WorkManager.
- **READ:**
  1. The UI observes Room reactively through `Flow<List<IssueEntity>>`.
  2. The UI never queries the remote REST API as its direct data source. When remote data is retrieved, it is merged into Room, which in turn automatically notifies the UI.
- **UPDATE:**
  1. The user's changes are applied immediately to the local Room record.
  2. The `updatedAt` timestamp is refreshed to the current local epoch milliseconds.
  3. If the record was already pending creation (`operationType == CREATE`), it retains `CREATE` so that it will be transmitted as a single `POST` upon reconnection. Otherwise, it is marked with `operationType = UPDATE` and `syncState = PENDING`.
  4. WorkManager sync is enqueued.
- **DELETE (Tombstone Pattern):**
  1. If an issue was created offline and deleted before ever being synchronized (`operationType == CREATE` and `syncState == PENDING`), it is permanently removed from Room immediately (no remote delete request is required for an ID unknown to the server).
  2. If the issue has already reached the server, it is marked with `isDeleted = true`, `operationType = DELETE`, and `syncState = PENDING`.
  3. Active queries filter out deleted rows (`WHERE isDeleted = 0`), hiding it from the user immediately while preserving the tombstone record for remote synchronization.

---

### 4. How Retrofit Endpoints Correspond to Server CRUD

The `IssueApi` interface maps standard HTTP methods to RESTful server endpoints:
- `GET /issues`: Retrieves the list of remote issues for initial sync and pull reconciliation.
- `POST /issues`: Transmits a newly created `IssueDto` payload to the server.
- `PUT /issues/{id}`: Replaces/updates an existing issue resource on the server by its UUID.
- `DELETE /issues/{id}`: Directs the remote server to remove or mark the issue resource deleted.

---

### 5. How Bidirectional Synchronization Works

Synchronization operates as a two-stage sequential pipeline inside `IssueSyncWorker` and `IssueRepository`:
1. **Push Phase (Local to Remote):**
   - The repository queries Room for all rows where `syncState = PENDING` or `FAILED`.
   - Operations are executed sequentially: `CREATE` calls `POST`, `UPDATE` calls `PUT`, and `DELETE` calls `DELETE`.
   - On server confirmation (HTTP 200/201/204), local records transition to `syncState = SYNCED` and `operationType = NONE`, while deleted tombstones are permanently purged via `purgeTombstone(id)`.
2. **Pull Phase (Remote to Local):**
   - The repository requests `GET /issues`.
   - Incoming DTOs are mapped to entities and passed to `mergeRemoteIssues()`.
   - Local records currently marked `PENDING` are shielded from being overwritten.
   - For already-synced records, timestamp comparisons ensure that the newest update wins.

---

### 6. How Delete Tombstones Prevent Silent Data Loss

In a distributed, offline-first environment, deleting a local record immediately by issuing `DELETE FROM issues WHERE id = :id` creates a critical synchronization flaw:
- When the device reconnects and executes a remote pull (`GET /issues`), the server—which has not yet been notified of the deletion—will return the record as an active remote issue.
- The local database will treat this as a newly discovered remote record and re-insert it, causing the deleted issue to "resurrect" mysteriously.
- By using a **tombstone** (`isDeleted = true`), the intent to delete is preserved locally. The record remains invisible to user-facing lists while enabling the background worker to execute `DELETE /issues/{id}`. Only once the server responds successfully is the local tombstone permanently expunged.

---

### 7. Conflict Resolution Strategy

1. **Pending Local Mutation Protection (Primary Invariant):**
   - If a local entity has `syncState == PENDING`, it indicates that the user has uncommitted local edits. The remote sync pull is forbidden from overwriting this record. Local user input is strictly preserved.
2. **Timestamp-Based Reconciliation (Newest-Update-Wins):**
   - For records where `syncState == SYNCED`, incoming remote entities are compared against the local record using `remote.updatedAt >= local.updatedAt`. If the remote update is newer, the local record is updated; otherwise, the older remote update is discarded.

---

### 8. How SavedStateHandle and Room Solve Different State Problems

- **`SavedStateHandle` (Transient UI & Draft State):**
  - Handles transient, in-flight user interactions before submission (e.g., text currently being typed into an input field, selected dropdown index).
  - Preserves data across **Activity/Fragment recreation** caused by configuration changes (such as screen rotation) and system-initiated background process termination (under low memory).
  - Does not survive application uninstall or device reboot, and should not store persistent business entities.
- **`Room Database` (Persistent Business State):**
  - Handles committed, long-term domain data (submitted bug reports, synchronization queues, tombstones).
  - Backed by an on-disk SQLite database file that survives process death, application updates, and device power cycles.
  - Accessible across background workers (`IssueSyncWorker`) even when no UI activity is in memory.

---

### 9. Why WorkManager is Used for Background Synchronization

WorkManager is the recommended Android Jetpack library for deferrable, guaranteed background work:
- **Persistent Job Scheduling:** Work requests are saved in an internal SQLite database, guaranteeing that pending work executes even if the application process is terminated or the device reboots.
- **System Resource Constraints:** WorkManager respects battery and operating system power-saving features (such as Doze mode). It defers execution until declared constraints (e.g., `NetworkType.CONNECTED`) are fully satisfied.
- **Deduplication:** Through `enqueueUniqueWork`, WorkManager prevents redundant duplicate worker instances from running concurrently, eliminating race conditions during data synchronization.

---

### 10. Failure Classification & Retry Policy

The repository categorizes errors using `SyncResult` to ensure precise handling:
- **Retryable Failures (`Result.retry()`):**
  - `IOException` / Network disconnections (no route to host, connection timed out, DNS lookup failure).
  - Remote HTTP 5xx server errors (500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable).
  - Bounded by exponential backoff (starting at 15 seconds) up to a maximum of 3 attempts (`MAX_RETRIES`).
- **Non-Retryable Failures (`Result.failure()`):**
  - Validation errors (empty titles, illegal input values).
  - Authentication/Authorization failures (401 Unauthorized, 403 Forbidden).
  - Malformed request syntax (400 Bad Request).
  - Prevents endless retry loops from draining the user's device battery and network bandwidth.

---

### 11. Version Control & Git Strategy

The project adheres to professional version control practices:
- **Branch Hierarchy:**
  - `main`: Stable production-ready releases.
  - `feature/offline-issue-sync`: Dedicated branch for Room schemas, Retrofit endpoints, repository sync, and WorkManager implementation.
  - `hotfix/preserve-delete-tombstones`: Isolated branch addressing edge cases where deleted records must be retained as tombstones until remote confirmation.
- **Conventional Commits:** All commit messages use standardized semantic prefixes (`feat:`, `fix:`, `test:`, `docs:`, `chore:`) to establish a clean, traceable development history.
- **Annotated Release Tags:** Releases are marked using annotated semantic tags (e.g., `git tag -a v1.0 -m "v1.0: stable offline CRUD and synchronization"`).

---

### 12. Honest Limitations of the Completed Project

To preserve academic integrity, the following real-world boundaries are explicitly acknowledged:
1. **Placeholder Remote URL:** In the absence of a hosted cloud REST backend, `RetrofitClient` points to `https://api.bugtracker.uopeople.internal/v1/`. For live testing, a local mock server (e.g., Node.js/Express, WireMock, or MockWebServer) can be substituted.
2. **Simplified Conflict Resolution:** The newest-update-wins strategy relies on device system clocks. In a multi-user enterprise setting, distributed clocks can drift; a production architecture would utilize server-assigned sequence numbers, vector clocks, or ETags to detect three-way merge conflicts.
3. **No Biometric / OAuth2 Authentication:** The current academic release focuses on core offline-first synchronization and does not include user authentication or role-based access control.
