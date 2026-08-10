# PR-003 — Workspace Shell & Navigation

| Field | Value |
|--------|-------|
| Sprint | 01 - Analytics Workspace |
| PR | PR-003 |
| Status | Ready |
| Goal | Build the navigable app shell for the Finance Analytics workspace |
| Depends On | PR-002 |

---

# Objective

Replace PR-002's single placeholder destination with the real navigation shell of the
app: a phase-based flow (Empty → Import → App), a bottom tab bar for the three main
screens (Overview, Insights, Transactions), and the fully built empty/onboarding
screen. This establishes the information architecture that PR-004 onward will fill in
screen by screen.

Source: `Finance Analytics.dc.html` (Claude Design prototype), sections
`EMPTY STATE` and the outer phase/tab scaffolding. Import, Overview, Insights,
Transactions and Categories *content* are explicitly out of scope — this PR only
proves the navigation graph reaches them.

---

# Required Context

Read these documents before writing code:

1. `docs/foundation/00_ENGINEERING_STACK.md`
2. `docs/foundation/01_ANDROID_BLUEPRINT.md`
3. `docs/foundation/02_ANDROID_ARCHITECTURE.md`
4. `docs/foundation/06_AI_CONTEXT.md`
5. `docs/foundation/07_REPOSITORY_CONVENTIONS.md`
6. `docs/project/00_PROJECT_CHARTER.md`
7. `docs/execution/00_FOUNDATION/PR-002_ANDROID_FOUNDATION.md` (the foundation this
   PR builds on)

---

# Implementation Rules

- Follow MVVM. No UseCases.
- One feature package: `features/workspace/` (per `01_ANDROID_BLUEPRINT.md`'s
  feature-first structure), containing the phase/tab navigation and the empty screen.
- Overview, Insights, and Transactions tab screens are **stubs** in this PR (title
  only, proving the tab bar routes correctly) — their real content is PR-005/006/007.
- Import is a **stub destination** in this PR (proving the phase transition works)
  — the real multi-step wizard is PR-004.
- No CSV parsing, no Room entities, no real transaction/category data. Where the
  design references computed values (totals, insights, etc.), they do not exist yet
  — do not fabricate them here.
- Reuse the `core/designsystem` theme and `core/navigation` package from PR-002;
  do not create a second theme or a parallel NavHost.

---

# Tasks

## 1. Navigation Model

Define the phase/tab destinations in `core/navigation`:

- `empty` — onboarding screen.
- `import` — stub screen (placeholder for PR-004's wizard).
- `app` — hosts the bottom tab bar with three nested tabs: `overview`, `insights`,
  `transactions`.

Replace the PR-002 placeholder `NavHost` route with this graph. Start destination is
`empty`.

## 2. Bottom Tab Bar

Material 3 `NavigationBar` with three items (Overview, Insights, Transactions),
shown only while in the `app` phase. Each tab renders a stub screen (a centered
title, e.g. "Overview — coming soon") so the graph is provably wired end-to-end.

## 3. Empty / Onboarding Screen

Build to match the design's `EMPTY STATE` section:

- Eyebrow label ("FINANCE ANALYTICS")
- Headline ("Understand where your money goes.")
- Supporting copy (one paragraph)
- Three privacy bullet points (on-device analysis, no bank connection, works
  offline)
- "Import CSV" primary button → navigates to the `import` stub destination

Owns a `WorkspaceUiState` (static content — no ViewModel logic beyond exposing the
copy) and a `WorkspaceViewModel` registered in Koin, consistent with PR-002's
DI pattern.

## 4. Import Stub Screen

A minimal screen with a title, a "back" affordance to `empty`, and a "Continue"
action that navigates to `app`. This proves the full phase chain
(Empty → Import → App) is reachable end-to-end through real UI, without a
test-only entry point. No wizard steps yet — the real multi-step flow is PR-004.

---

# Out of Scope

Do NOT implement:

- CSV import wizard (select/preview/validate/progress/done/error) — PR-004
- Overview dashboard content — PR-005
- Insights list/detail/filters — PR-006
- Transactions list/search/detail — PR-007
- Category list/detail — PR-008
- Settings sheet, theme override, currency — PR-009
- Any Room entity, DAO, or repository
- Any mock/demo data generation (introduced in PR-004 alongside the import flow
  that produces it)

---

# Acceptance Criteria

## Build

- Android project builds successfully.
- Dependencies resolve successfully; no new dependencies were added.

## Navigation

- App launches into the Empty screen.
- Tapping "Import CSV" navigates to the Import stub.
- Tapping "Continue" on the Import stub navigates to the `app` phase, which shows
  a working bottom tab bar that switches between three stub screens.

## Architecture

- MVVM-compatible: `WorkspaceViewModel` + `WorkspaceUiState`.
- Koin provides the ViewModel; no artificial dependencies introduced.
- No Clean Architecture layers introduced.

## Testing

- Unit test(s) for `WorkspaceViewModel` pass.
- Instrumentation test proving Empty → Import navigation passes.

## Code Quality

- No unnecessary abstractions.
- No unused dependencies.
- No business logic (totals, insights, categorisation).

---

# Documentation Impact

Update documentation only if implementation decisions differ from the existing
documentation.

If nothing changed:

```text
Documentation impact: None.
```

---

# Pull Request

## Title

```text
feat(workspace): add workspace shell and navigation
```

## Description

```markdown
## Summary

Built the navigable app shell: phase-based navigation (Empty → Import → App),
bottom tab bar for Overview/Insights/Transactions, and the empty/onboarding screen.

## Changes

- Navigation graph: empty / import / app (with nested tabs) destinations
- Bottom tab bar (Material 3 NavigationBar) with three stub tab screens
- Empty/onboarding screen matching the Finance Analytics design
- Import stub destination
- WorkspaceViewModel + WorkspaceUiState, Koin-registered

## Architecture Decisions

List decisions not already defined by the documentation.

## Testing

List commands executed and their results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that no CSV import, dashboard, insights, transactions, categories, or
settings content was implemented — only the navigation shell and the empty screen.

## Follow-up

PR-004 — CSV Import Flow (mock data)
```

---

# Engineering Reflection

Before opening the PR, answer:

1. Did we introduce any unnecessary dependency?
2. Did we create any abstraction with no current consumer?
3. Is the module/package structure simpler than it could be?
4. Is the architecture consistent with MVVM without Clean Architecture?
5. Could another developer understand the navigation shell immediately?
6. Is any documentation update required?

---

# Stop Condition

After implementation, tests, documentation updates and PR preparation are complete:

**STOP.**

Do not implement PR-004 or any later PR in this sprint.

Wait for human review.
