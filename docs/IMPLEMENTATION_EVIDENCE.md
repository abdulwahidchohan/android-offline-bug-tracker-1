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
| **1** | **Application Issue List** | Active issues rendered in RecyclerView with title, priority badge, status badge, formatted timestamp, and textual sync indicator. | `[PENDING DEVICE CAPTURE: docs/screenshots/01_issue_list.png]` |
| **2** | **Create Issue Screen** | Clean editor form with empty inputs for Title and Description, and radio selectors for Priority (Low/Medium/High/Critical) and Status (Open/In Progress/Resolved/Closed). | `[PENDING DEVICE CAPTURE: docs/screenshots/02_create_issue.png]` |
| **3** | **Update Issue Screen** | Populated form fields when editing an existing ticket, allowing mutation of title, description, priority, or status. | `[PENDING DEVICE CAPTURE: docs/screenshots/03_update_issue.png]` |
| **4** | **Offline Pending Issue** | Issue created while offline displaying explicit textual badge **"Pending Sync"** (never color alone, per WCAG accessibility requirements). | `[PENDING DEVICE CAPTURE: docs/screenshots/04_offline_pending.png]` |
| **5** | **Issue Retained After App Restart** | Cold launch of the application after force-stop, proving persistence in local SQLite database via Room. | `[PENDING DEVICE CAPTURE: docs/screenshots/05_app_restart.png]` |
| **6** | **Draft Restored After Rotation** | Screen rotation from portrait to landscape retaining partially typed title and description via `SavedStateHandle`. | `[PENDING DEVICE CAPTURE: docs/screenshots/06_rotation_draft.png]` |
| **7** | **Successful Synchronization** | Issue badge updating from "Pending Sync" to **"Synchronized"** upon background worker execution. | `[PENDING DEVICE CAPTURE: docs/screenshots/07_synced_badge.png]` |
| **8** | **Failed Sync and Bounded Retry** | Issue badge displaying **"Sync Failed"** or Logcat entries showing exponential backoff retry scheduling under network error. | `[PENDING DEVICE CAPTURE: docs/screenshots/08_sync_retry.png]` |
| **9** | **Room Database Inspector Evidence** | Android Studio Database Inspector view showing the `issues` table rows and columns. | `[PENDING INSPECTION CAPTURE: docs/screenshots/09_room_inspector.png]` |
| **10** | **Feature Branch (`feature/offline-issue-sync`)** | Terminal proof of active feature branch for offline sync development. | **VERIFIED (See Terminal Evidence Below)** |
| **11** | **Hotfix Branch (`hotfix/preserve-delete-tombstones`)** | Terminal proof of isolated hotfix branch with real code modifications safeguarding tombstones. | **VERIFIED (See Terminal Evidence Below)** |
| **12** | **Commit Graph (`git log --graph --oneline`)** | Visual graph of conventional commits, feature merge, hotfix merge, and release tag. | **VERIFIED (See Terminal Evidence Below)** |
| **13** | **Merged Pull Request** | GitHub Pull Request merging feature and hotfix branches into `main`. | `[AWAITING REMOTE PUSH: See Section C]` |
| **14** | **Release Tag (`v1.0`)** | Terminal proof of annotated release tag marking stable offline CRUD. | **VERIFIED (See Terminal Evidence Below)** |

---

### Verified Terminal Evidence

#### A. Git Branch Listing (`git branch -a`)
```
  feature/offline-issue-sync
  hotfix/preserve-delete-tombstones
* main
```

#### B. Git Annotated Release Tag (`git tag -n -l`)
```
v1.0            v1.0: stable offline CRUD and synchronization
```

#### C. Git Commit Graph (`git log --graph --oneline --decorate --all -n 15`)
```
*   4876c0f (HEAD -> main, tag: v1.0) merge: hotfix/preserve-delete-tombstones into main
|\  
| * 211921c (hotfix/preserve-delete-tombstones) fix: preserve deleted issues until server confirmation
|/  
*   3966b90 merge: feature/offline-issue-sync into main
|\  
| * 84aeb03 (feature/offline-issue-sync) docs: add setup, architecture documentation, and academic rubric mapping
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

#### D. Automated Test Suite Execution (`.\gradlew.bat testDebugUnitTest`)
```
> Task :app:compileDebugKotlin
> Task :app:kspDebugKotlin
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 1m 27s
28 actionable tasks: 16 executed, 12 up-to-date
```
**Outcome:** **17 tests completed, 17 passed, 0 failed.**

---

### Step-by-Step Instructions to Capture Device & GitHub Evidence

#### 1. Connecting Physical Phone & Installing APK
1. On your Android phone, navigate to **Settings -> About Phone** and tap **Build Number** 7 times to unlock Developer Options.
2. In **Settings -> System -> Developer Options**, enable **USB Debugging**.
3. Connect your phone to your PC via USB cable. In PowerShell, verify the connection:
   ```powershell
   adb devices
   ```
4. Install the generated debug APK directly to your phone:
   ```powershell
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```
5. Capture device screenshots directly from the command line:
   ```powershell
   # Create screenshots directory
   New-Item -ItemType Directory -Force -Path docs\screenshots

   # Take screenshot of open app screen:
   adb exec-out screencap -p > docs\screenshots\01_issue_list.png
   ```

#### 2. Running Connected Instrumentation Tests on Device
With your phone connected and unlocked:
```powershell
.\gradlew.bat connectedDebugAndroidTest
```
The test runner will execute `IssueDaoInstrumentationTest` on the physical SQLite engine and save an HTML test report to `app\build\reports\androidTests\connected\`.

#### 3. Connecting GitHub Remote & Pushing
Once you create an empty repository on GitHub named `android-offline-bug-tracker`:
```powershell
# 1. Add your remote repository URL
git remote add origin https://github.com/<your-username>/android-offline-bug-tracker.git

# 2. Push main branch and annotated release tag
git push -u origin main --tags

# 3. Push feature and hotfix branches for complete PR visibility
git push -u origin feature/offline-issue-sync
git push -u origin hotfix/preserve-delete-tombstones
```
