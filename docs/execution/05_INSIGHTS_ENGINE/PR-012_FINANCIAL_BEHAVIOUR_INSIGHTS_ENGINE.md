# PR-012 — Financial Behaviour & Insights Engine

| Field | Value |
|---|---|
| Sprint | 05 - Insights Engine |
| PR | PR-012 |
| Status | Ready |
| Goal | Combine validated analytical signals into deterministic, user-facing financial insights |
| Depends On | PR-009 — Anomaly Detection; PR-010 — Recurring Detection; PR-011 — Merchant Normalisation & Categorisation |

## Objective

Build the first **Insights Engine** for Finance Analytics.

The engine combines analytical outputs into concise, evidence-based insights about transaction behaviour.

```text
Transactions
     ↓
Enrichment
     ↓
Analytical Features
     ↓
Anomaly Detection ─────┐
Recurring Detection ───┤
Spending Behaviour ────┤
Category Behaviour ────┤
Income / Expense ──────┤
                       ↓
                 Insights Engine
                       ↓
               Structured Insights
```

This PR creates the analytical layer later consumed by the API and Android application. Do not implement API or Android integration yet.

## Required Context

Read:

1. `docs/foundation/00_ENGINEERING_STACK.md`
2. `docs/foundation/06_AI_CONTEXT.md`
3. `docs/foundation/07_REPOSITORY_CONVENTIONS.md`
4. `docs/project/00_PROJECT_CHARTER.md`
5. `docs/project/01_PRODUCT_PRINCIPLES.md`
6. `docs/project/02_DOMAIN_MODEL.md`
7. `docs/project/03_PRODUCT_REQUIREMENTS.md`
8. `docs/project/05_DESIGN_SYSTEM.md`
9. `docs/execution/03_ANALYTICS_FOUNDATION/PR-007_PYTHON_ANALYTICS_WORKSPACE.md`
10. `docs/execution/03_ANALYTICS_FOUNDATION/PR-008_EDA_TRANSACTION_BEHAVIOUR.md`
11. `docs/execution/04_ANALYTICAL_FEATURES/PR-009_TRANSACTION_ANOMALY_DETECTION.md`
12. `docs/execution/04_ANALYTICAL_FEATURES/PR-010_RECURRING_TRANSACTION_DETECTION.md`
13. `docs/execution/04_ANALYTICAL_FEATURES/PR-011_MERCHANT_NORMALISATION_CATEGORISATION.md`

Also inspect the completed implementations and notebooks from PR-009, PR-010 and PR-011.

## Critical Principle

Insights must be **evidence-driven**.

Every insight must follow:

```text
Evidence
   ↓
Analytical rule
   ↓
Insight
```

Never invent facts.

## 1. Insight Model

Create a structured insight model containing at minimum:

```text
id
type
title
description
severity
confidence
related_transaction_ids
metadata
```

Where appropriate:

```text
category
merchant
amount
comparison_period
```

## 2. Insight Types

Implement a small initial set supported by the existing analytical capabilities.

### Spending Trend

Example:

```text
Your spending increased 18% compared with last month.
```

Only generate with sufficient history and a meaningful comparison.

### Category Change

Example:

```text
Your Food & Dining spending increased 24% this month.
```

### Unusual Transaction

Consume PR-009 results. Do not duplicate anomaly logic.

### Recurring Payment

Consume PR-010 results. Do not duplicate recurring logic.

### Income / Expense Change

Example:

```text
Your expenses increased while income remained stable this month.
```

### Savings Rate Change

If supported by the domain and available data:

```text
Your savings rate decreased from 24% to 17%.
```

Do not provide financial advice.

## 3. Insight Rules

Each rule must define:

```text
Input data
Minimum history
Condition
Comparison
Output
```

Avoid arbitrary thresholds. Justify thresholds using EDA or analytical behaviour.

## 4. Confidence

Every insight must have a confidence indicator reflecting evidence quality, such as history length, pattern consistency and signal strength.

Do not call confidence a calibrated probability unless calibrated.

## 5. Severity

Use a small controlled vocabulary, for example:

```text
INFO
NOTICE
IMPORTANT
```

Severity must not imply financial danger.

## 6. Insight Ranking

Rank insights deterministically using appropriate factors such as:

```text
evidence strength
confidence
recency
user relevance
```

Do not create a complex recommendation model.

## 7. Deduplication

Prevent multiple insights communicating the same fact.

Use a simple deterministic deduplication strategy.

## 8. Temporal Comparisons

Define comparison periods explicitly, for example:

```text
current month vs previous month
current month vs previous 3-month average
current category spend vs historical category median
```

Do not compare non-comparable periods. For partial periods, either use comparable partial-period analysis or suppress the insight. Document the choice.

## 9. Explainability

Every insight must retain enough metadata to explain how it was produced, such as:

```text
current_value
previous_value
change_percent
comparison_period
threshold
source_feature
```

## 10. No LLM Yet

Do NOT use an LLM to generate insights in this PR.

The engine must produce deterministic structured insights for reproducibility, testing and future API contracts.

## 11. Implementation

Create reusable code under:

```text
analytics/src/finance_analytics/
```

A reasonable structure:

```text
insights/
├── __init__.py
├── models.py
├── rules.py
├── engine.py
├── ranking.py
└── deduplication.py
```

Adapt to the existing architecture and avoid unnecessary abstraction.

## 12. Notebook

Create:

```text
notebooks/06_insights_engine.ipynb
```

Demonstrate:

1. Input analytical signals
2. Insight rules
3. Generated structured insights
4. Ranking
5. Deduplication
6. Example explanations
7. Limitations
8. Example output suitable for an API

## 13. Testing

Add tests for:

### Spending trends
- Increase
- Decrease
- No meaningful change
- Insufficient history

### Category changes
- Significant increase
- Significant decrease
- Stable category
- Insufficient data

### Anomalies
- Converts an anomaly result into an insight.
- Does not reimplement anomaly detection.

### Recurring
- Converts recurring results into an insight.
- Does not reimplement recurring detection.

### Ranking
- Stronger/recent insights rank appropriately.

### Deduplication
- Equivalent insights are not duplicated.

### Confidence
- Stronger evidence produces stronger confidence.
- Insufficient evidence lowers confidence or suppresses the insight.

## 14. Evaluation

Create controlled scenarios:

```text
Scenario A — stable spending
→ no spending-change insight

Scenario B — significant spending increase
→ spending trend insight

Scenario C — unusual transaction
→ anomaly insight

Scenario D — recurring subscription
→ recurring insight

Scenario E — insufficient history
→ no unsupported insight
```

Do not evaluate only on synthetic demo data. Use small controlled fixtures.

## 15. Product Safety

The engine must never produce:

- financial advice;
- investment recommendations;
- claims of fraud;
- claims that spending is objectively bad;
- unsupported predictions.

Use neutral language.

Good:

```text
Your spending increased 22% compared with last month.
```

Bad:

```text
You are spending too much.
```

## 16. Future API Boundary

Design the structured insight model so it can later be serialised into an API response.

Do not implement FastAPI in this PR.

```text
Python Analytics
      ↓
Insights Engine
      ↓
Structured Insight
      ↓
FastAPI
      ↓
Android
```

## Out of Scope

Do NOT implement:

- FastAPI
- REST endpoints
- Android integration
- Room persistence
- LLM-generated insights
- Financial advice
- Recommendations
- Conversational AI
- Push notifications
- Personalised financial coaching

## Acceptance Criteria

- Insights are generated from analytical evidence.
- Rules are deterministic.
- Existing anomaly and recurring results are consumed rather than duplicated.
- Insights can be ranked.
- Duplicate insights are removed.
- Every insight has supporting metadata.
- Confidence and comparison periods are explicit.
- No financial advice, fraud claims or unsupported predictions.
- Controlled scenarios pass.
- Insufficient-history cases are handled.
- Existing analytics tests remain green.
- `06_insights_engine.ipynb` runs from start to finish.

## Pull Request

### Title

```text
feat(analytics): build financial insights engine
```

### Description

```markdown
## Summary

Built a deterministic Insights Engine that combines validated analytical signals into structured financial insights.

## Insight Types

List implemented insight types.

## Rules

Describe the main analytical rules and thresholds.

## Ranking

Describe how insights are prioritised.

## Deduplication

Describe how duplicate insights are prevented.

## Evaluation

Describe controlled scenarios and results.

## Safety

Confirm that the engine does not provide financial advice or fraud claims.

## Testing

List commands executed and results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that FastAPI, Android integration and LLM-generated insights were not implemented.

## Follow-up

Next PR should establish the Analytics API contract and FastAPI service.
```

## Engineering Reflection

Before opening the Pull Request, answer:

1. Is every insight backed by evidence?
2. Are we consuming analytical results rather than duplicating their logic?
3. Are thresholds justified?
4. Is confidence meaningful and explainable?
5. Are comparison periods valid?
6. Are insights deterministic?
7. Are duplicate insights controlled?
8. Is the output suitable for an API?
9. Is the language neutral and free from financial advice?
10. Could the same engine work for another user's transaction history?

## Stop Condition

After implementation, tests, notebook execution, controlled evaluation, documentation and PR preparation are complete:

**STOP.**

Do not implement FastAPI, Android integration, LLM-generated insights or recommendations.

Wait for human review.
