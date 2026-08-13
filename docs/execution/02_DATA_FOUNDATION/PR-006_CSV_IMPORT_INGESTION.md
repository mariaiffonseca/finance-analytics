# PR-006 — CSV Import & Ingestion

| Field | Value |
|---|---|
| Sprint | 02 - Data Foundation |
| PR | PR-006 |
| Status | Ready |
| Goal | Import real transaction data into the local database |
| Depends On | PR-005 — Transaction Data Foundation |

---

# Objective

Implement the CSV ingestion pipeline that allows users to select a bank-exported CSV, validate its rows, transform valid rows into the project's `Transaction` model, and persist them locally.

This PR is responsible for **ingestion only**.

Do not implement analytics, machine learning, insight generation, or advanced categorisation.

---

# Required Context

Read:

1. `docs/foundation/00_ENGINEERING_STACK.md`
2. `docs/foundation/01_ANDROID_BLUEPRINT.md`
3. `docs/foundation/02_ANDROID_ARCHITECTURE.md`
4. `docs/foundation/03_ANDROID_FEATURE_TEMPLATE.md`
5. `docs/foundation/04_FEATURE_REVIEW_CHECKLIST.md`
6. `docs/foundation/06_AI_CONTEXT.md`
7. `docs/foundation/07_REPOSITORY_CONVENTIONS.md`
8. `docs/project/00_PROJECT_CHARTER.md`
9. `docs/project/01_PRODUCT_PRINCIPLES.md`
10. `docs/project/02_DOMAIN_MODEL.md`
11. `docs/project/03_PRODUCT_REQUIREMENTS.md`
12. `docs/project/05_DESIGN_SYSTEM.md`
13. `docs/execution/02_DATA_FOUNDATION/PR-005_TRANSACTION_DATA_FOUNDATION.md`

---

# Implementation Rules

- Keep CSV parsing independent from Android UI.
- Keep parsing and validation deterministic and testable.
- Do not put parsing logic in ViewModels.
- Do not put CSV logic in Room entities.
- Do not introduce a backend.
- Do not introduce ML or LLM classification.
- Do not silently discard invalid rows.
- Do not hard-code undocumented bank-specific assumptions.
- Prefer a small explicit ingestion pipeline over a generic CSV framework.

---

# 1. File Selection

Implement Android file selection using the appropriate platform document/file picker.

Requirements:

- User can select a CSV file.
- Unsupported file types are rejected.
- File access is handled safely.
- The selected file is passed to the import pipeline.

Do not permanently copy files unless required by the design.

---

# 2. CSV Parsing

Create a dedicated parser that converts CSV input into an intermediate representation.

The parser must not write directly to Room.

Pipeline:

```text
CSV file
   ↓
Parser
   ↓
Parsed row
   ↓
Validation
   ↓
Transaction mapping
   ↓
TransactionRepository
   ↓
Room
```

Support the format required by the current project requirements, including as applicable:

- Header detection
- Explicit column mapping
- Quoted values
- Empty values
- Numeric values
- Dates

Do not build a universal CSV parser.

Document source-format assumptions.

---

# 3. Column Mapping

Define an explicit mapping between CSV columns and transaction fields.

Cover the fields required by:

`docs/project/02_DOMAIN_MODEL.md`

Do not infer columns based on vague similarity.

---

# 4. Validation

Validate each parsed row before persistence.

At minimum:

- Required columns
- Required values
- Date format
- Amount format
- Currency where required
- Duplicate rows where detectable

Invalid rows must produce structured validation errors.

Never silently ignore invalid data.

---

# 5. Import Result

Create an explicit import result model containing:

- Rows read
- Valid transactions
- Invalid rows
- Duplicates
- Validation errors

It must be suitable for displaying an import summary.

---

# 6. Transaction Mapping

Convert validated rows into the domain `Transaction`.

The mapping must preserve:

- Source amount accurately
- Source date
- Merchant
- Currency where available

Fields unavailable in the CSV must follow the domain rules.

Do not perform advanced categorisation in this PR.

---

# 7. Persistence

Persist valid transactions through:

```text
TransactionRepository
```

The import layer must never access Room directly.

---

# 8. Duplicate Handling

Prevent accidental duplicate imports.

Define a deterministic duplicate strategy based on the domain model and project requirements.

Do not rely solely on an auto-generated database ID.

Document the chosen strategy.

---

# 9. Import UI

Connect the existing Import feature to the ingestion pipeline.

Implement:

```text
Select file
    ↓
Reading
    ↓
Validating
    ↓
Importing
    ↓
Completed
```

Errors must be actionable.

The completed state should show:

- Rows processed
- Transactions imported
- Invalid rows
- Duplicates

Use the approved Design System.

Do not build analytics screens.

---

# 10. Testing

Add focused tests for:

### Parser
- Valid CSV
- Quoted values
- Empty values
- Invalid CSV
- Supported numeric representations

### Validation
- Missing required values
- Invalid dates
- Invalid amounts
- Invalid columns

### Mapping
- Parsed row → Transaction

### Duplicate handling
- Re-importing the same data does not create unintended duplicates.

### Import flow
- Important state transitions.

Use small fixtures.

---

# Out of Scope

Do NOT implement:

- Analytics calculations
- Machine learning
- LLM classification
- Merchant clustering
- Advanced category classification
- Insights
- Recommendations
- Charts
- Dashboard analytics
- Remote processing
- Backend
- Authentication
- Financial advice

---

# Acceptance Criteria

## Import

- User can select a CSV file.
- CSV is parsed successfully.
- Valid rows become domain transactions.
- Valid transactions are persisted through the repository.

## Validation

- Invalid rows are detected.
- Errors are structured and visible.
- Invalid rows are not silently persisted.

## Duplicates

- Re-importing the same data does not unintentionally duplicate transactions.

## UI

- Import flow uses the approved Design System.
- Progress and completion states are understandable.
- Import summary is visible.

## Testing

- Parser tests pass.
- Validation tests pass.
- Mapping tests pass.
- Duplicate handling tests pass.
- Android project builds successfully.

---

# Documentation Impact

Update documentation if implementation introduces:

- A new CSV format assumption
- A new duplicate strategy
- A domain-model change
- A data-architecture change

Otherwise state:

```text
Documentation impact: None.
```

---

# Pull Request

## Title

```text
feat(import): add csv transaction ingestion
```

## Description

```markdown
## Summary

Implemented CSV transaction ingestion from file selection through local persistence.

## Changes

- Added CSV file selection
- Added CSV parser
- Added validation
- Added transaction mapping
- Added duplicate handling
- Added import result model
- Connected import flow to TransactionRepository
- Added import tests

## Import Format

Describe the supported CSV format and assumptions.

## Duplicate Strategy

Describe how duplicates are identified.

## Testing

List commands executed and results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that analytics and insight generation were not implemented.

## Follow-up

Next PR should establish the Python analytics workspace and data-analysis pipeline.
```

---

# Engineering Reflection

Before opening the Pull Request, answer:

1. Is the parser independent from Android UI?
2. Are CSV assumptions explicit?
3. Are invalid rows handled without silent data loss?
4. Is duplicate detection deterministic?
5. Does the import layer remain independent from Room?
6. Could parser/validation logic be reused outside Android?
7. Did we accidentally introduce analytics logic?
8. Is the ingestion pipeline simple enough to evolve?

---

# Stop Condition

After implementation, tests, documentation updates and PR preparation are complete:

**STOP.**

Do not implement the analytics pipeline.

Wait for human review.
