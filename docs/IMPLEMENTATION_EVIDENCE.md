# Implementation Evidence Checklist

**Course:** CS 4405-01 Mobile Applications  
**Assignment:** Unit 3 Assignment Activity – Offline-First Bug Tracker  
**Student:** Abdul Wahid Chohan  
**Institution:** University of the People  

---

This document defines the required visual and execution evidence for the academic assessment of the Offline-First Bug Tracker application. Each section provides the exact verification criteria that each screenshot or command output must substantiate.

---

### Evidence Checklist

| # | Evidence Item | Verification Objective & What Screenshot Must Prove | Status |
|---|---|---|---|
| **1** | **Application Issue List** | Shows active issues displayed inside RecyclerView with title, priority badge, status badge, formatted timestamp, and textual sync indicator. Must demonstrate that status is not communicated through color alone. | `[PLACEHOLDER: docs/screenshots/01_issue_list.png]` |
| **2** | **Create Issue Screen** | Shows the issue editor with empty form fields, outlining input fields for Title and Description, and radio selections for Priority (Low/Medium/High/Critical) and Status (Open/In Progress/Resolved/Closed). | `[PLACEHOLDER: docs/screenshots/02_create_issue.png]` |
| **3** | **Update Issue Screen** | Shows existing issue data populated into the editor fields upon tapping an issue card or "Edit" button, ready for modification. | `[PLACEHOLDER: docs/screenshots/03_update_issue.png]` |
| **4** | **Offline Pending Issue** | Shows an issue created while airplane mode/disconnection is active. The badge must explicitly display **"Pending Sync"** in text, proving local persistence without network dependency. | `[PLACEHOLDER: docs/screenshots/04_offline_pending.png]` |
| **5** | **Issue Retained After App Restart** | Shows the issue list immediately upon cold launching the application after force-stopping it, proving persistent storage in SQLite via Room. | `[PLACEHOLDER: docs/screenshots/05_app_restart.png]` |
| **6** | **Draft Restored After Rotation** | Demonstrates landscape orientation change with partially filled title and description inputs intact, proving state preservation via `SavedStateHandle`. | `[PLACEHOLDER: docs/screenshots/06_rotation_draft.png]` |
| **7** | **Successful Synchronization** | Shows the issue sync badge changing to **"Synchronized"** after network connectivity is restored and `IssueSyncWorker` executes. | `[PLACEHOLDER: docs/screenshots/07_synced_badge.png]` |
| **8** | **Failed Sync and Bounded Retry** | Shows an issue with a **"Sync Failed"** badge or Logcat output showing `IssueSyncWorker` encountering a network error and scheduling exponential backoff retry. | `[PLACEHOLDER: docs/screenshots/08_sync_retry.png]` |
| **9** | **Room Database Inspector Evidence** | Shows the Android Studio App Inspection / Database Inspector view displaying the `issues` table rows, columns (`id`, `title`, `syncState`, `isDeleted`, `operationType`, `updatedAt`). | `[PLACEHOLDER: docs/screenshots/09_room_inspector.png]` |
| **10** | **Feature Branch (`feature/offline-issue-sync`)** | Terminal output of `git branch -a` or GitHub repository branch list showing the active feature branch where offline sync was developed. | `[PLACEHOLDER: docs/screenshots/10_git_feature_branch.png]` |
| **11** | **Hotfix Branch (`hotfix/preserve-delete-tombstones`)** | Terminal output of `git branch -a` or GitHub repository showing the hotfix branch dedicated to preserving tombstone records until server acknowledgement. | `[PLACEHOLDER: docs/screenshots/11_git_hotfix_branch.png]` |
| **12** | **Commit Graph (`git log --graph --oneline`)** | Terminal output of git commit log showing structured commits (`feat:`, `fix:`, `chore:`, `test:`) and branch merges. | `[PLACEHOLDER: docs/screenshots/12_git_commit_graph.png]` |
| **13** | **Merged Pull Request** | GitHub or Git CLI representation of feature branch merged into `main` branch. | `[PLACEHOLDER: docs/screenshots/13_merged_pr.png]` |
| **14** | **Release Tag (`v1.0`)** | Terminal output of `git tag -n -l` showing annotated release tag `v1.0: stable offline CRUD and synchronization`. | `[PLACEHOLDER: docs/screenshots/14_git_tag_v1.0.png]` |

---

### Step-by-Step Instructions to Capture Real Evidence

1. **Capturing Device Screenshots (Items 1–8):**
   ```powershell
   # Use the Android CLI or adb to capture high-resolution device screens
   android screenshot --output=docs/screenshots/01_issue_list.png
   ```

2. **Capturing Room Database Inspector (Item 9):**
   - Open Android Studio -> App Inspection -> Database Inspector.
   - Select `bug_tracker.db` and double-click `issues`.
   - Take a window capture showing the table rows and columns.

3. **Capturing Git Version Control Evidence (Items 10–14):**
   ```powershell
   # Terminal commands to display git evidence:
   git branch -vv
   git log --graph --oneline --decorate -n 15
   git tag -n -l
   ```
