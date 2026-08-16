# Product Requirements

| Field | Value |
|--------|-------|
| Project | Finance Analytics |
| Version | 1.0.0 |
| Status | Draft |
| Last Updated | 2026-08-08 |

## Purpose

Defines the functional scope of the MVP and serves as the implementation reference.

## Product Goal

Enable users to import financial transactions and receive meaningful insights in less than one minute.

## MVP

### Epic 1 — Data Import
- Import CSV
- Validate file
- Preview data
- Handle import errors

Acceptance:
- Supported CSV imports successfully.
- Clear validation errors.
- UI remains responsive.

### Epic 2 — Data Preparation
- Normalize merchants
- Categorize transactions
- Handle unknown categories
- Store locally

Acceptance:
- Every transaction has a category.
- Users can manually edit categories.

### Epic 3 — Dashboard
Display:
- Income
- Expenses
- Savings
- Category breakdown
- Monthly trend

Acceptance:
- Dashboard loads quickly.
- Charts are readable.

### Epic 4 — Insights
Generate:
- Highest spending category
- Month-over-month changes
- Unusual spending
- Recurring merchants

Acceptance:
- At least five useful insights.
- Every insight contains an explanation.

## Out of Scope (v1)

- Authentication
- Cloud sync
- Notifications
- Budget planner
- Investments
- AI Chat
- Receipt scanning

## Priorities

P1
- CSV Import
- Local persistence
- Dashboard
- Insights

P2
- Manual categorization
- Search
- Export

P3
- Forecasting
- Recommendations
- AI summaries

## Non-functional Requirements

- Offline-first
- Modular architecture
- Unit-testable
- Responsive
- Maintainable

## Success Criteria

- User imports data.
- Dashboard is populated.
- Insights are generated.
- Core flows are tested.

## Open Questions

- Initial CSV formats?
- Duplicate detection?
- Incremental imports?

## Related Documents

- 00_PROJECT_CHARTER.md
- 01_PRODUCT_PRINCIPLES.md
- 02_DOMAIN_MODEL.md

## Changelog

### 1.0.0
Initial version.
