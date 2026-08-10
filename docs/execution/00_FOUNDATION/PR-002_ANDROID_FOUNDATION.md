# PR-002 — Android Foundation

| Field | Value |
|--------|-------|
| Sprint | 00 - Foundation |
| PR | PR-002 |
| Status | Ready |
| Goal | Create the Android application foundation |
| Depends On | PR-001 |

---

# Objective

Create the Android application foundation according to the project's architecture and engineering conventions.

The application must build and launch successfully, but must not contain Finance Analytics business functionality.

---

# Required Context

Read these documents before writing code:

1. `docs/foundation/00_ENGINEERING_STACK.md`
2. `docs/foundation/01_ANDROID_BLUEPRINT.md`
3. `docs/foundation/02_ANDROID_ARCHITECTURE.md`
4. `docs/foundation/06_AI_CONTEXT.md`
5. `docs/foundation/07_REPOSITORY_CONVENTIONS.md`
6. `docs/project/00_PROJECT_CHARTER.md`

---

# Implementation Rules

- Follow MVVM.
- Use Jetpack Compose.
- Use Koin for dependency injection.
- Do not introduce Clean Architecture layers.
- Do not create UseCases unless explicitly required.
- Keep the foundation minimal.
- Do not add libraries that are not required by this PR.
- Use Gradle Version Catalog for dependency management.
- Keep Android-specific code inside `android/`.

---

# Tasks

## 1. Android Project

Create the Android application inside:

```text
android/
```

The project must:

- Compile successfully.
- Launch successfully.
- Use Kotlin.
- Use Jetpack Compose.
- Use Material 3.

---

## 2. Gradle

Configure:

- Gradle Wrapper
- Android Gradle Plugin
- Kotlin
- Version Catalog
- Repository configuration

All dependency versions must be managed through the Version Catalog.

---

## 3. Application

Create the main application module.

The initial application should display a simple placeholder Compose screen.

Do not implement product UI.

---

## 4. Core Structure

Establish the foundation for:

```text
core/
    common/
    database/
    designsystem/
    navigation/
    network/
    testing/
```

Do not create unnecessary Gradle modules merely to satisfy this structure. If a component does not yet justify a separate module, use an appropriate package structure and document the decision.

---

## 5. Features

Create the base `features/` structure.

Do not create Finance Analytics features yet.

Do not create:

- import
- dashboard
- insights
- transactions
- categories

---

## 6. Koin

Configure Koin at application level.

- Create the application class if required.
- Initialise Koin.
- Create the appropriate module structure.
- Demonstrate that dependency injection is correctly configured.

Do not create artificial dependencies purely to demonstrate Koin.

---

## 7. Navigation

Configure the minimum navigation infrastructure required by the application.

A single placeholder destination is sufficient.

Do not implement feature navigation.

---

## 8. Room

Add and configure the Room foundation for future database work.

Do not create:

- Entities
- DAOs
- Database tables
- Repositories

---

## 9. Retrofit

Add Retrofit according to the project stack.

Do not create API interfaces or network calls.

---

## 10. DataStore

Add DataStore according to the project stack.

Do not create application preferences or persistence logic yet.

---

## 11. Testing

Configure the Android testing foundation.

At minimum:

- Unit test dependencies.
- Instrumentation test dependencies where appropriate.
- One basic test proving the setup works.

Do not create feature tests.

---

## 12. Formatting / Static Analysis

Configure the formatting/static-analysis tooling defined by the foundation documents.

Do not introduce additional tools without justification.

---

# Out of Scope

Do NOT implement:

- CSV import
- Transaction model
- Room entities
- DAOs
- Repositories
- Analytics
- Categorisation
- Dashboard
- Insights
- Recommendations
- Authentication
- Backend
- API calls
- Business logic
- Production UI

---

# Acceptance Criteria

## Build

- Android project builds successfully.
- Gradle configuration is clean.
- Dependencies resolve successfully.

## Application

- Application launches successfully.
- A placeholder Compose screen is displayed.

## Architecture

- MVVM-compatible foundation exists.
- Koin is configured.
- Navigation foundation exists.
- Core structure is consistent with the architecture document.
- No Clean Architecture layers were introduced.

## Testing

- Unit tests execute successfully.
- At least one basic test passes.

## Code Quality

- No unnecessary abstractions.
- No unused dependencies.
- No business logic.
- No unfinished work that belongs to this PR.

---

# Documentation Impact

Update documentation only if implementation decisions differ from the existing documentation.

If an architectural or repository decision changes, update the relevant document and mention it in the PR.

If nothing changed:

```text
Documentation impact: None.
```

---

# Pull Request

## Title

```text
build(android): initialise Android foundation
```

## Description

```markdown
## Summary

Implemented the Android project foundation.

## Changes

- Android project setup
- Gradle configuration
- Compose
- Koin
- Navigation
- Room foundation
- Retrofit foundation
- DataStore foundation
- Testing foundation

## Architecture Decisions

List decisions not already defined by the documentation.

## Testing

List commands executed and their results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that no product functionality was implemented.

## Follow-up

PR-003 — Analytics Workspace
```

---

# Engineering Reflection

Before opening the PR, answer:

1. Did we introduce any unnecessary dependency?
2. Did we create any abstraction with no current consumer?
3. Is the module/package structure simpler than it could be?
4. Is the architecture consistent with MVVM without Clean Architecture?
5. Could another developer understand the Android structure immediately?
6. Is any documentation update required?

---

# Stop Condition

After implementation, tests, documentation updates and PR preparation are complete:

**STOP.**

Do not implement PR-003 or any future functionality.

Wait for human review.
