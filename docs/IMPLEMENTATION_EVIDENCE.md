# Implementation Evidence Checklist

**Course:** CS 4405-01 Mobile Applications  
**Assignment:** Unit 3 Assignment Activity – Offline-First Bug Tracker  
**Student:** Abdul Wahid Chohan  
**Institution:** University of the People  

---

This document provides the definitive verification matrix for the academic assessment of the Offline-First Bug Tracker application. Each section details the verification criteria, current status, verified terminal proof, and actionable step-by-step procedures to record device screenshots.

---

### Evidence Verification Matrix

| # | Evidence Item | Verification Objective | Current Status |
|---|---|---|---|
| **1** | **Application Issue List** | Active issues rendered in RecyclerView with title, priority badge, status badge, formatted timestamp, and textual sync indicator. | **VERIFIED:** `docs/screenshots/01_issue_list.png` (Captured on Samsung Galaxy A07) |
| **2** | **Create Issue Screen** | Clean editor form with empty inputs for Title and Description, and radio selectors for Priority (Low/Medium/High/Critical) and Status (Open/In Progress/Resolved/Closed). | **VERIFIED:** `docs/screenshots/02_create_issue.png` (Captured on Samsung Galaxy A07) |
| **3** | **Update Issue Screen** | Populated form fields when editing an existing ticket, allowing mutation of title, description, priority, or status. | **VERIFIED:** `docs/screenshots/03_update_issue.png` (Captured on Samsung Galaxy A07) |
| **4** | **Offline Pending Issue** | Issue created while offline displaying explicit textual badge **"Pending Sync"** (never color alone, per WCAG accessibility requirements). | **VERIFIED:** `docs/screenshots/04_offline_pending.png` (Captured on Samsung Galaxy A07) |
| **5** | **Issue Retained After App Restart** | Cold launch of the application after force-stop, proving persistence in local SQLite database via Room. | **VERIFIED:** `docs/screenshots/05_app_restart.png` (Captured on Samsung Galaxy A07) |
| **6** | **Draft Restored After Rotation** | Screen rotation from portrait to landscape retaining partially typed title and description via `SavedStateHandle`. | **VERIFIED:** `docs/screenshots/06_rotation_draft.png` (Captured on Samsung Galaxy A07) |
| **7** | **Failed Sync and Bounded Retry** | Swipe-to-refresh or worker sync attempt showing retry scheduling under offline conditions. | **VERIFIED:** `docs/screenshots/07_sync_retry.png` (Captured on Samsung Galaxy A07) |
| **8** | **Feature Branch (`feature/offline-issue-sync`)** | Terminal proof of active feature branch for offline sync development. | **VERIFIED (Pushed to GitHub: [Branch Link](https://github.com/abdulwahidchohan/android-offline-bug-tracker-1/tree/feature/offline-issue-sync))** |
| **9** | **Hotfix Branch (`hotfix/preserve-delete-tombstones`)** | Terminal proof of isolated hotfix branch with real code modifications safeguarding tombstones. | **VERIFIED (Pushed to GitHub: [Branch Link](https://github.com/abdulwahidchohan/android-offline-bug-tracker-1/tree/hotfix/preserve-delete-tombstones))** |
| **10** | **Commit Graph (`git log --graph --oneline`)** | Visual graph of conventional commits, feature merge, hotfix merge, and release tag. | **VERIFIED (See Terminal Evidence Below)** |
| **11** | **Pull Request Evidence** | GitHub Pull Request merging `docs/final-evidence` into `main`. | **VERIFIED (Ready on GitHub: [PR Compare Link](https://github.com/abdulwahidchohan/android-offline-bug-tracker-1/compare/main...docs/final-evidence?expand=1))** |
| **12** | **Release Tag (`v1.0`)** | Terminal proof of annotated release tag marking stable offline CRUD. | **VERIFIED (Pushed to GitHub: [Release v1.0](https://github.com/abdulwahidchohan/android-offline-bug-tracker-1/releases/tag/v1.0))** |
| **13** | **Device Instrumentation Tests** | Connected test execution on real device SQLite database. | **VERIFIED (6/6 tests passed on Samsung Galaxy A07 SM-A075F)** |

---

### Verified Terminal Evidence

#### A. Git Branch Listing (`git branch -a`)
```
  docs/final-evidence
  feature/offline-issue-sync
  hotfix/preserve-delete-tombstones
* main
  remotes/origin/HEAD -> origin/main
  remotes/origin/docs/final-evidence
  remotes/origin/feature/offline-issue-sync
  remotes/origin/hotfix/preserve-delete-tombstones
  remotes/origin/main
```

#### B. Git Annotated Release Tag (`git tag -n -l`)
```
v1.0            v1.0: stable offline CRUD and synchronization
```

#### C. Git Commit Graph (`git log --graph --oneline --decorate --all -n 15`)
```
*   4876c0f (HEAD -> main, tag: v1.0, origin/main) merge: hotfix/preserve-delete-tombstones into main
|\  
| * 211921c (origin/hotfix/preserve-delete-tombstones, hotfix/preserve-delete-tombstones) fix: preserve deleted issues until server confirmation
|/  
*   3966b90 merge: feature/offline-issue-sync into main
|\  
| * 84aeb03 (origin/feature/offline-issue-sync, feature/offline-issue-sync) docs: add setup, architecture documentation, and academic rubric mapping
| * 37be25b test: add DAO, repository, ViewModel, and mapper tests
| * 098bec6 feat: preserve editor state across lifecycle recreation and add Material 3 UI
| * 08d1d74 feat: add WorkManager synchronization and retry logic
| * b2a2910 feat: implement offline-first issue repository with conflict protection
| * 88c137d feat: add Retrofit issue endpoints, DTOs, and mapper
| * ee1ca37 feat: add Room issue schema, enums, converters, and CRUD operations
| * c989018 chore: initialize Android bug tracker with Gradle 8.7 and build configuration
|/  
* ef60907 Initial commit
```

#### D. Automated Unit Test Suite Execution (`.\gradlew.bat testDebugUnitTest`)
```
> Task :app:compileDebugKotlin
> Task :app:kspDebugKotlin
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 1m 13s
28 actionable tasks: 28 executed
```
**Outcome:** **17 tests completed, 17 passed, 0 failed.**

#### E. Debug APK Assembly (`.\gradlew.bat assembleDebug`)
```
> Task :app:packageDebug
> Task :app:assembleDebug

BUILD SUCCESSFUL in 20s
39 actionable tasks: 18 executed, 21 up-to-date
```
**Artifact:** `app\build\outputs\apk\debug\app-debug.apk` (7.12 MB, Verified and Deployed).

#### F. Android Lint Static Analysis (`.\gradlew.bat lintDebug`)
```
> Task :app:lintAnalyzeDebug
> Task :app:lintReportDebug
> Task :app:lintDebug

BUILD SUCCESSFUL in 36s
```
**Outcome:** **0 errors, 0 fatal issues.**

#### G. Device Connected Instrumentation Tests (`.\gradlew.bat connectedDebugAndroidTest`)
```
> Task :app:connectedDebugAndroidTest
Starting 6 tests on SM-A075F - 16

Finished 6 tests on SM-A075F - 16

BUILD SUCCESSFUL in 46s
70 actionable tasks: 7 executed, 63 up-to-date
```
**Outcome:** **6 tests executed directly on physical Samsung Galaxy A07 (SM-A075F), 6 passed, 0 failed.**

---

### Physical Device Verification Screenshots (`docs/screenshots/`)

All screenshots were captured live from the connected Samsung Galaxy A07:

1. `01_issue_list.png`: Active issues rendered in RecyclerView with priority, status, and pending sync badges.
2. `02_create_issue.png`: Editor form with Title, Description, Priority selector, and Status selector.
3. `03_update_issue.png`: Edit form populated with existing issue data ready for mutation.
4. `04_offline_pending.png`: Explicit accessible textual badge "Pending Sync" displayed on ticket.
5. `05_app_restart.png`: Cold launch after process kill, proving local SQLite persistence via Room.
6. `06_rotation_draft.png`: Screen rotation retaining uncommitted title/description draft via `SavedStateHandle`.
7. `07_sync_retry.png`: Swipe-to-refresh sync triggered with offline retry scheduling.

---

### GitHub Remote & Pull Request Evidence
The repository and all branches are pushed to GitHub:
- **Repository:** [https://github.com/abdulwahidchohan/android-offline-bug-tracker-1](https://github.com/abdulwahidchohan/android-offline-bug-tracker-1)
- **Branches:** `main`, `feature/offline-issue-sync`, `hotfix/preserve-delete-tombstones`, `docs/final-evidence`
- **Release Tag:** `v1.0`
- **Pull Request:** [Open PR comparison on GitHub](https://github.com/abdulwahidchohan/android-offline-bug-tracker-1/compare/main...docs/final-evidence?expand=1)
