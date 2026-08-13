# PR-005 — Transaction Data Foundation

| Field | Value |
|---|---|
| Sprint | 02 - Data Foundation |
| PR | PR-005 |
| Status | Ready |
| Goal | Establish the transaction data model and local persistence foundation |
| Depends On | PR-004 — Analytics Workspace |

---

# Objective

Create the core transaction data model and local persistence foundation required by Finance Analytics.

This PR establishes how transaction data is represented and stored locally.

It does **not** implement CSV parsing, file import, categorisation, analytics, insights, or production transaction screens.

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
10. `docs/project/02_DOMAIN_MODEL.md`
11. `docs/project/03_PRODUCT_REQUIREMENTS.md`
12. `docs/project/05_DESIGN_SYSTEM.md`

---

# Implementation Rules

- Follow MVVM and the Repository Pattern.
- Keep the domain model independent from Room.
- Use Room only for local persistence.
- Do not introduce Clean Architecture.
- Do not create UseCases unless explicitly required.
- Keep the data layer simple.
- Do not introduce a remote data source or backend.
- Do not add CSV parsing in this PR.
- Do not create fake production data.
- Store monetary values safely; do not use floating-point values for persisted money.

---

# 1. Transaction Domain Model

Create the transaction model required by the project.

The model must follow:

`docs/project/02_DOMAIN_MODEL.md`

Do not add fields that are not justified by the domain model.

---

# 2. Money Representation

Money must not be persisted using `Float` or `Double`.

Choose an appropriate representation that:

- avoids floating-point precision problems;
- supports positive and negative amounts;
- preserves the original transaction value;
- is suitable for aggregation.

Document the choice if it is not already defined.

---

# 3. Room Entity

Create the Room entity representing persisted transactions.

Rules:

- The Room entity is a persistence model.
- Do not expose Room annotations through the domain model.
- Use explicit domain/entity mapping.
- Add indexes only where justified by current queries.

Potential query dimensions include:

- Date
- Category
- Merchant

Do not create speculative indexes.

---

# 4. DAO

Create the transaction DAO with only queries currently justified by the product requirements.

The persistence layer should support the future needs of:

- Observing transactions.
- Filtering by date.
- Looking up transactions.
- Counting transactions.

Do not implement analytics aggregation queries yet.

---

# 5. Database

Configure the Room database using the existing database foundation.

The database should:

- contain the transaction entity;
- expose the transaction DAO;
- be provided through Koin;
- follow the project's database naming/versioning conventions.

Do not add unrelated entities.

---

# 6. Repository

Create:

```text
TransactionRepository
```

The repository must hide Room implementation details.

Expose domain-level models, not Room entities.

Implement only the operations required by the current requirements.

Possible operations include:

```text
observeTransactions()
getTransaction()
insertTransactions()
deleteAllTransactions()
```

Do not add operations speculatively.

Do not put analytics logic in the repository.

---

# 7. Mapping

Create explicit mappings:

```text
Transaction
    ↕
TransactionEntity
```

Keep mappings deterministic and testable.

Do not put mapping logic inside ViewModels.

---

# 8. Dependency Injection

Register through Koin where required:

- Room database
- Transaction DAO
- Transaction repository

Keep the dependency graph minimal.

---

# 9. Testing

Add focused tests for:

### Mapping

- Domain → Entity
- Entity → Domain

### Repository

Test relevant repository behaviour.

### Database

Test:

- Insert
- Read
- Delete where implemented
- Relevant filtering

Use isolated test data.

Do not create a large test fixture framework.

---

# 10. Transactions Feature Integration

Connect the existing Transactions feature foundation to the repository only enough to prove the data flow:

```text
Transaction UI
      ↓
TransactionsViewModel
      ↓
TransactionRepository
      ↓
Room
```

Do not build the final Transactions UI.

A minimal state-driven integration test is sufficient.

---

# Out of Scope

Do NOT implement:

- CSV parser
- File picker
- Import UI
- Import validation
- Merchant normalisation
- Category classification
- Analytics calculations
- Insight generation
- Dashboard metrics
- Charts
- Recommendations
- Remote APIs
- Backend
- Authentication
- Real financial data
- Demo data in production code

---

# Acceptance Criteria

## Domain

- Transaction domain model matches `02_DOMAIN_MODEL.md`.
- Money representation avoids floating-point persistence.
- Domain model has no Room dependencies.

## Persistence

- Transaction entity exists.
- DAO exists with only justified queries.
- Room database contains the transaction entity.
- Database is provided through Koin.

## Repository

- Repository hides Room details.
- Repository exposes domain models.
- Mapping is explicit and tested.

## Integration

- Transactions feature can obtain transaction state through the repository.
- No database details leak into the ViewModel or UI.

## Testing

- Mapping tests pass.
- Repository tests pass.
- Room tests pass.
- Android project builds successfully.

---

# Documentation Impact

If implementation requires a change to:

- Domain model
- Architecture
- Repository conventions
- Database strategy

update the relevant document and mention it in the PR.

Otherwise state:

```text
Documentation impact: None.
```

---

# Pull Request

## Title

```text
feat(data): establish transaction persistence
```

## Description

Use:

```markdown
## Summary

Established the transaction domain and local persistence foundation.

## Changes

- Added transaction domain model
- Added Room entity
- Added transaction DAO
- Added database configuration
- Added repository
- Added entity/domain mappings
- Added Koin bindings
- Added persistence tests
- Connected the Transactions feature to the repository foundation

## Architecture Decisions

List decisions made during implementation.

## Testing

List commands executed and results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that CSV import and analytics logic were not implemented.

## Follow-up

Next PR should focus on CSV ingestion/import.
```

---

# Engineering Reflection

Before opening the Pull Request, answer:

1. Is the domain model free from persistence concerns?
2. Is the money representation safe?
3. Are the DAO queries minimal?
4. Did we add any speculative database structure?
5. Does the repository provide useful abstraction without unnecessary layers?
6. Is Room completely hidden from the feature layer?
7. Could any part of the implementation be simpler?
8. Is the data model ready for CSV ingestion?

---

# Stop Condition

After:

1. Implementation is complete.
2. Tests pass.
3. Documentation is updated if required.
4. Pull Request is prepared.

**STOP.**

Do not implement CSV import or analytics.

Wait for human review.
