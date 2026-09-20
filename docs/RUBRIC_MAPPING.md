# Course Rubric Mapping

**Course:** CS 4405-01 Mobile Applications  
**Assignment:** Unit 3 Assignment Activity – Offline-First Bug Tracker  
**Student:** Abdul Wahid Chohan  
**Institution:** University of the People  

---

This document explicitly maps the codebase implementation, architecture, and artifacts against each grading criterion of the CS 4405 Unit 3 assignment rubric.

---

### Rubric Criteria & Codebase Mapping

| Rubric Area | Required Standard | Implementation Location & Evidence | Academic Justification |
|---|---|---|---|
| **Q1: Room & Retrofit Implementation** | Complete local Room persistence with offline CRUD, plus Retrofit remote synchronization. | - `BugTrackerDatabase.kt`<br>- `IssueDao.kt`<br>- `IssueEntity.kt`<br>- `IssueApi.kt`<br>- `IssueDto.kt`<br>- `IssueMapper.kt`<br>- `IssueRepository.kt` | Room serves as the local single source of truth. All CRUD operations mutate Room immediately without network blocking. Retrofit handles remote DTO serialization. WorkManager coordinates bidirectional sync. |
| **Q2: Error Handling & Lifecycle Management** | Robust error handling, non-blocking operations, and lifecycle-aware state management. | - `SyncResult.kt`<br>- `IssueEditorViewModel.kt`<br>- `IssueListViewModel.kt`<br>- `IssueSyncWorker.kt`<br>- `fragment_issue_editor.xml` | `SyncResult` sealed hierarchy separates Network, Server, and Validation failures without leaking stack traces. `SavedStateHandle` guarantees editor draft survival during rotation and process recreation. UI observes Room reactively via `Flow`. |
| **Q3: Git Workflow & Version Control** | Demonstrates feature branches, hotfix branches, descriptive commits, pushes, and release tags. | - Branches: `main`, `feature/offline-issue-sync`, `hotfix/preserve-delete-tombstones`<br>- Tags: `v1.0`<br>- Commits following Conventional Commits | Repository demonstrates professional Git hygiene: feature branching for core development, hotfix isolation for bug remedies, atomic descriptive commits, and semantic annotated version tagging. |
| **Information Quality** | Technical depth, clear explanations, and absence of fabricated results. | - `README.md`<br>- `docs/ASSIGNMENT_NOTES.md`<br>- Unit tests in `app/src/test/` | Architecture follows official Android Jetpack guidelines. Placeholder backend URLs and test doubles are honestly labeled without fabricating remote endpoints. |
| **Sources & Evidence** | Clear verification artifacts, testing logs, and documentation references. | - `docs/IMPLEMENTATION_EVIDENCE.md`<br>- Unit & instrumentation test suites<br>- Verification command logs | All verification commands, test assertions, and screenshot placeholders are cataloged with specific proof criteria. |
| **Effective Communication** | Clean formatting, readable Kotlin code, clear markdown structure. | - Full project source code<br>- Material 3 design system<br>- Accessible labels and contrast | Code is cleanly structured, commented for oral defense, adheres to Kotlin style conventions, and enforces accessibility guidelines (WCAG touch targets and textual status badges). |
| **Word Count & Formatting** | Structured documentation formatted with clear headers, tables, and code snippets. | - `README.md`<br>- `docs/ASSIGNMENT_NOTES.md`<br>- `docs/RUBRIC_MAPPING.md` | Comprehensive written material organized into standard academic sections suitable for assignment submission and peer review. |
