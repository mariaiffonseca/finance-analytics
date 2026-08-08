# Project Charter

| Field | Value |
|--------|-------|
| Project | Finance Analytics |
| Version | 1.0.0 |
| Status | Draft |
| Owner | Maria Ines Fonseca |
| Last Updated | 2026-08-08 |

## Purpose

Finance Analytics is a personal analytics platform focused on understanding financial behaviour through insights instead of transaction management.

## Vision

Turn raw financial data into clear, actionable insights.

## Product Statement

Users already have access to their transactions.
They lack meaningful analytics.

The product focuses on:
- spending patterns
- anomalies
- recurring behaviour
- trends
- insights

## Target Users

Primary:
- People exporting bank transactions
- Data-driven users

Secondary:
- Recruiters evaluating this portfolio

## North Star Metric

**Time to First Insight**

## Design Pillars

1. Insight First
2. Analytics over CRUD
3. Privacy by Default
4. Offline First
5. Simplicity over Complexity

## MVP

- Import CSV
- Clean data
- Categorise transactions
- Dashboard
- Monthly insights

## Future

- Forecasting
- Anomaly detection
- Subscription detection
- AI summaries
- Recommendations

## Non Goals

This is NOT:
- a banking app
- a budgeting app
- an accounting system

## Success Criteria

- Useful insights
- Minimal setup
- Responsive
- Strong engineering

## High-Level Architecture

Android App
↓
Local Database
↓
Analytics Engine
↓
Insight Generation
↓
Dashboard

## Engineering Principles

- Pragmatic MVVM
- Feature-first
- Repository Pattern
- Testability
- Maintainability

## Open Questions

- CSV formats?
- Rule-based or ML categorisation?
- First insight types?

## Related Documents

- 00_ENGINEERING_STACK.md
- 01_ANDROID_BLUEPRINT.md
- 02_ANDROID_ARCHITECTURE.md

## Changelog

### 1.0.0

Initial version.
