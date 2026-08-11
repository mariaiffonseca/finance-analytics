# PR-004 — Analytics Workspace

| Field | Value |
|---|---|
| Sprint | 01 - Analytics Workspace |
| PR | PR-004 |
| Status | Ready |
| Goal | Establish the analytics workspace and feature boundaries |
| Depends On | PR-003 — Android Design System |

---

# Objective

Create the Android analytics workspace structure and navigation/state foundation for the core Finance Analytics experience.

This PR establishes feature boundaries and application flow.

It does **not** implement CSV parsing, financial data processing, database schema, analytics algorithms, or production analytics screens.

---

# Required Context

Read these documents before writing code:

1. `docs/foundation/00_ENGINEERING_STACK.md`
2. `docs/foundation/01_ANDROID_BLUEPRINT.md`
3. `docs/foundation/02_ANDROID_ARCHITECTURE.md`
4. `docs/foundation/03_ANDROID_FEATURE_TEMPLATE.md`
5. `docs/foundation/04_FEATURE_REVIEW_CHECKLIST.md`
6. `docs/foundation/06_AI_CONTEXT.md`
7. `docs/foundation/07_REPOSITORY_CONVENTIONS.md`
8. `docs/project/00_PROJECT_CHARTER.md`
9. `docs/project/01_PRODUCT_PRINCIPLES.md`
10. `docs/project/03_PRODUCT_REQUIREMENTS.md`
11. `docs/project/05_DESIGN_SYSTEM.md`

---

# Implementation Rules

- Follow MVVM.
- Use the Design System from PR-003.
- Do not introduce Clean Architecture.
- Do not introduce UseCases unless explicitly required.
- Keep feature boundaries clear.
- Keep navigation simple.
- Prefer existing Design System components.
- Do not create fake financial data in production code.
- Do not implement analytics logic to make screens appear functional.
- Do not introduce a backend.

---

# Product Flow

The application has two high-level states:

```text
No imported data
        |
        v
   Import Flow
        |
        v
Data available
        |
        v
Analytics Workspace
```

The Analytics Workspace has three primary destinations:

```text
Overview
Insights
Transactions
```

---

# Feature Structure

Create the following feature boundaries:

```text
features/
├── overview/
├── insights/
├── transactions/
├── categories/
├── settings/
└── import/
```

Each feature must follow `docs/foundation/02_ANDROID_ARCHITECTURE.md`.

Do not add additional feature modules without a clear requirement.

---

# Overview

Overview is the primary analytics destination.

Eventually it will contain:

- Selected period
- Total spending
- Income / savings metrics
- Main insight
- Spending trend
- Category breakdown
- Additional insights

For this PR:

- Create the feature boundary.
- Create the minimal ViewModel/state foundation.
- Create the navigation destination.
- Create a placeholder screen using the Design System.

Do not implement real analytics.

---

# Insights

Insights is responsible for discovering meaningful patterns.

The visual reference defines:

```text
Recent
Spending
Trends
Anomalies
Recurring
```

For this PR:

- Create the feature boundary.
- Create the minimal ViewModel/state foundation.
- Create the navigation destination.
- Create a placeholder screen using the Design System.
- Create filter state only where required by the UI foundation.

Do not implement insight generation.

---

# Transactions

Transactions provides access to underlying transaction data.

The visual reference defines:

- Search
- Date filters
- Category filters
- Transaction rows
- Transaction detail bottom sheet

For this PR:

- Create the feature boundary.
- Create the minimal ViewModel/state foundation.
- Create the navigation destination.
- Create a placeholder screen using the Design System.

Do not implement database access or transaction loading.

---

# Categories

Category analytics will eventually provide:

- Category spending
- Change vs previous period
- Change vs average
- Trend
- Merchant breakdown

For this PR:

- Establish the feature boundary only.
- Do not add Categories to primary navigation.
- Prepare future category-detail navigation only if genuinely required by the existing navigation design.

Do not implement analytics.

---

# Import

Import is the entry point when no financial data is available.

The eventual flow is:

```text
Select file
    |
    v
Preview
    |
    v
Validate
    |
    v
Import
    |
    v
Completed
```

For this PR:

- Establish the feature boundary.
- Establish the navigation/state foundation required to enter the flow.
- Create a minimal placeholder entry screen if needed.

Do not implement CSV parsing or persistence.

---

# Settings

Settings are intentionally minimal.

The visual reference includes:

- Appearance
- Currency
- Local-data/privacy information
- Reset demo data

For this PR:

- Establish the feature boundary.
- Do not add Settings to primary navigation unless required by the existing application navigation.
- Create only the foundation required for future implementation.

---

# Navigation

Implement primary navigation for:

```text
Overview
Insights
Transactions
```

Use the existing navigation infrastructure.

The selected destination must be represented in application state.

Navigation must not contain business logic.

Future destinations such as Category Detail and Transaction Detail should be reached from their respective features rather than becoming primary navigation destinations.

---

# Application State

Support the conceptual distinction:

```text
NoData
DataAvailable
```

Do not implement persistence for this state yet.

Do not introduce fake data or a temporary database solely to simulate it.

---

# UI States

Features should be designed to support:

```text
Loading
Content
Empty
Error
```

Only implement states required by this PR.

Do not create artificial loading or error flows without real data sources.

---

# Design System Usage

All UI created in this PR must use the Design System established by PR-003.

Do not:

- Add raw colours.
- Add custom typography values.
- Create feature-specific button styles.
- Create feature-specific spacing tokens.
- Create new reusable component styles without updating the Design System.

If a reusable component is genuinely missing:

1. Determine whether it belongs in the Design System.
2. Update the Design System if appropriate.
3. Otherwise keep the feature implementation minimal and document the decision.

---

# ViewModels and State

Primary features should have minimal ViewModels and immutable UI state where required.

Conceptually:

```text
OverviewUiState
OverviewViewModel

InsightsUiState
InsightsViewModel

TransactionsUiState
TransactionsViewModel
```

Do not add repositories or use cases yet.

ViewModels must not contain financial calculations.

---

# Testing

Add tests for the state/navigation foundation where meaningful.

At minimum:

- Verify default feature state.
- Verify navigation destinations can be represented.
- Verify relevant state transitions introduced by this PR.

Do not test analytics behaviour that does not exist yet.

---

# Documentation Impact

If implementation requires an architectural change, update the relevant documentation.

Otherwise state:

```text
Documentation impact: None.
```

---

# Out of Scope

Do NOT implement:

- CSV parsing
- File import logic
- Room entities
- DAOs
- Repositories
- Transaction persistence
- Merchant categorisation
- Analytics algorithms
- Insight generation
- Recommendations
- Real charts
- Financial calculations
- Backend/API integration
- Authentication
- Real financial data
- Fake production data

---

# Acceptance Criteria

## Structure

- Feature boundaries exist for Overview, Insights, Transactions, Categories, Import and Settings.
- The structure follows the documented MVVM architecture.
- No unnecessary architectural layers are introduced.

## Navigation

- Overview is reachable.
- Insights is reachable.
- Transactions is reachable.
- Navigation uses the existing navigation foundation.
- Navigation contains no business logic.

## UI

- Placeholder screens use the approved Design System.
- No feature introduces its own visual language.
- Light and dark themes remain supported.

## State

- Primary features have appropriate immutable UI state where needed.
- No fake financial data is required.
- No analytics logic is implemented.

## Testing

- Relevant state/navigation tests pass.
- The Android project builds successfully.

---

# Pull Request

## Title

```text
feat(android): initialise analytics workspace
```

## Description

Use:

```markdown
## Summary

Established the Android analytics workspace and feature boundaries.

## Changes

- Added analytics feature structure
- Added Overview foundation
- Added Insights foundation
- Added Transactions foundation
- Added Categories foundation
- Added Import foundation
- Added Settings foundation
- Added primary navigation
- Added initial UI state models

## Architecture Decisions

List decisions made during implementation.

## Testing

List commands executed and results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that no financial data processing or analytics logic was implemented.

## Follow-up

Next PR should focus on the data/analytics workspace.
```

---

# Engineering Reflection

Before opening the Pull Request, answer:

1. Are the feature boundaries clear?
2. Did we introduce any feature that should not exist yet?
3. Did we add any unnecessary abstraction?
4. Is navigation still simple?
5. Are ViewModels free of business logic?
6. Are all UI elements using the Design System?
7. Did we accidentally introduce fake data?
8. Could the feature structure be simpler?

---

# Stop Condition

After implementation, tests, documentation updates and PR preparation are complete:

**STOP.**

Do not implement the data/analytics pipeline or any future PR.

Wait for human review.
