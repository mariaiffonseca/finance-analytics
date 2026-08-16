# Product Principles

| Field | Value |
|--------|-------|
| Project | Finance Analytics |
| Version | 1.0.0 |
| Status | Living Document |
| Last Updated | 2026-08-08 |

---

# Purpose

This document defines the product philosophy behind Finance Analytics.

Whenever a new feature, screen or insight is proposed, it should be evaluated against these principles before implementation.

---

# Product Vision

Finance Analytics is not an expense tracker.

It is a decision-support tool that helps users understand their financial behaviour through data.

---

# Core Principles

## 1. Insight First

Every feature should help users discover something meaningful.

If it doesn't generate or support an insight, question its value.

---

## 2. Analytics over CRUD

Managing transactions is secondary.

Understanding transactions is the primary goal.

---

## 3. Explain, Don't Just Show

Charts alone are not enough.

Whenever possible, accompany visualisations with natural-language explanations.

Example:

❌ "Restaurants: €420"

✅ "Restaurant spending increased by 24% compared to last month."

---

## 4. Simplicity Wins

Avoid overwhelming users with metrics.

Show the few insights that matter most.

---

## 5. Privacy by Default

Financial data remains on-device by default.

Cloud synchronisation is optional and never required for core functionality.

---

## 6. Actionable Insights

Every insight should encourage an action or reflection.

Good:
- "Subscriptions increased this month."

Better:
- "Three subscriptions haven't been used in over 60 days."

---

## 7. Progressive Disclosure

Start simple.

Allow users to explore deeper analytics only when they want to.

---

# Design Constraints

The application should never feel like:

- a banking application
- an accounting system
- a spreadsheet

It should feel like a modern analytics product.

---

# Decision Framework

Before implementing a feature, ask:

1. Does it help users understand their finances?
2. Does it support one of the product goals?
3. Is it more valuable than improving an existing insight?
4. Can it be explained in one sentence?

If most answers are "No", reconsider the feature.

---

# Examples

## High Priority

- Spending trends
- Category analysis
- Recurring subscriptions
- Monthly summaries
- Anomaly detection

## Low Priority

- Themes
- Social features
- Account management
- Complex settings

---

# North Star

The user should receive their first meaningful insight within seconds of importing data.

---

# Related Documents

- 00_PROJECT_CHARTER.md
- 01_PRODUCT_REQUIREMENTS.md
- 00_ENGINEERING_STACK.md

---

# Changelog

## 1.0.0

Initial version.
