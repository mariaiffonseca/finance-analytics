# PR-010 — Recurring Transaction Detection

| Field | Value |
|---|---|
| Sprint | 04 - Analytical Features |
| PR | PR-010 |
| Status | Ready |
| Goal | Detect recurring transaction patterns from historical behaviour |
| Depends On | PR-008 — Exploratory Data Analysis & Transaction Behaviour |

## Objective

Implement a deterministic, explainable recurring-transaction detector.

It should identify patterns such as subscriptions or regular payments while distinguishing them from frequent but irregular merchant activity.

This is pattern detection, not financial advice.

The visual prototype includes recurring transactions and marks recognised recurring transactions in transaction detail. Treat that as product reference, not analytical ground truth. fileciteturn10file0L73-L79 fileciteturn10file9L394-L399

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

Also inspect the completed EDA and anomaly-detection work.

## Critical Rule

Do not assume repeated merchant activity is recurring.

Before implementation:

1. Review EDA findings.
2. Identify recurring patterns supported by the data.
3. Define detection rules.
4. Validate them against controlled examples.
5. Document limitations.

Do not introduce ML unless EDA demonstrates deterministic rules are insufficient.

## 1. Recurring Definition

A recurring candidate should generally combine signals such as:

- repeated merchant;
- repeated or similar amount;
- relatively stable interval;
- sufficient occurrences;
- sufficient historical coverage.

Consider:

```text
Weekly
Biweekly
Monthly
Quarterly
Other regular interval
```

Do not require exact dates or amounts.

## 2. Candidate Generation

For each merchant candidate, calculate:

```text
occurrences
total_amount
median_amount
amount_variation
median_interval_days
interval_variation
first_seen
last_seen
```

Only use expense transactions unless requirements explicitly justify recurring income.

## 3. Merchant Normalisation

Do not build a full merchant-normalisation system.

If EDA reveals obvious formatting variations, introduce only the minimum deterministic normalisation needed.

Do not use fuzzy matching automatically. Document any rules.

## 4. Interval Analysis

Calculate intervals between consecutive transactions.

Consider:

```text
median_interval
interval_variation
```

Use robust statistics.

Allow reasonable date tolerance; do not assume `30 days == monthly` exactly.

## 5. Amount Analysis

Recurring payments may have equal amounts or small variations.

Calculate:

```text
median_amount
amount_deviation
relative_amount_variation
```

Small variations should be tolerated; large variation should reduce confidence.

## 6. Minimum History

Define explicit minimum-history requirements.

Justify thresholds using EDA, expected frequency and false-positive risk.

## 7. Confidence Score

Create a deterministic score combining appropriate signals such as:

```text
occurrence consistency
interval consistency
amount consistency
history length
```

Do not call it a calibrated probability unless it actually is.

## 8. Result Model

Create a structured result containing:

```text
merchant
is_recurring
confidence_score
frequency
occurrences
median_amount
amount_variation
median_interval_days
interval_variation
first_seen
last_seen
reason
```

Optionally include transaction IDs.

Do not persist results in Room yet.

## 9. Explainability

Every candidate must have a deterministic explanation.

Examples:

```text
Spotify appears every ~30 days with a consistent amount of €29.99.

Netflix appears 4 times with a stable monthly interval.

This merchant occurs frequently, but timing and amounts are too variable to classify as recurring.
```

Do not use an LLM.

## 10. Classification

Support:

```text
Recurring
Possible recurring
Not recurring
```

Use explicit documented thresholds.

## 11. Evaluation

If no labelled dataset exists:

- Do not claim accuracy.
- Create controlled synthetic examples.
- Validate known recurring patterns.
- Validate frequent-but-non-recurring merchants.
- Validate irregular merchants.
- Inspect false positives.

The supplied prototype deliberately generates repeated subscription transactions; use them only as validation examples, not as proof of general performance. fileciteturn10file0L75-L79

## 12. Notebook

Create:

```text
notebooks/04_recurring_transactions.ipynb
```

Include:

1. Motivation
2. EDA findings
3. Recurring definition
4. Candidate generation
5. Interval analysis
6. Amount analysis
7. Scoring
8. Examples
9. False positives
10. Limitations
11. Next steps

Use Plotly where useful.

## 13. Implementation

Add reusable code under:

```text
analytics/src/finance_analytics/
```

A reasonable structure:

```text
recurring/
├── __init__.py
├── features.py
├── detector.py
├── scoring.py
└── explanations.py
```

Adapt this to the existing codebase; avoid unnecessary files.

## 14. Testing

Add focused tests for:

### Candidate generation
- Repeated merchant creates a candidate.
- Insufficient history is handled correctly.

### Interval analysis
- Stable intervals increase recurrence evidence.
- Variable intervals reduce confidence.

### Amount analysis
- Stable amounts increase evidence.
- Small variations are tolerated.
- Large variations reduce confidence.

### Classification
- Clear recurring subscription.
- Possible recurring payment.
- Frequent but irregular merchant.
- One-off transaction.
- Insufficient history.

### Explainability
Every classified candidate has a deterministic explanation.

## 15. Relationship with Anomaly Detection

Keep PR-010 independent from PR-009.

The outputs may later complement each other:

```text
Recurring payment
       +
Unexpected amount
       ↓
Potential anomaly
```

Do not implement that combined insight yet.

## Product Boundary

This PR produces analytical recurring-payment results only.

Do NOT yet:

- display recurring payments in Android;
- create recommendations;
- generate natural-language insights;
- persist recurring classifications in Room;
- expose an API.

## Out of Scope

Do NOT implement:

- Full merchant-normalisation system
- ML classification
- LLM classification
- Recommendations
- Financial advice
- Android integration
- API
- Room persistence
- Forecasting

## Acceptance Criteria

- Candidates use multiple signals.
- Minimum-history rules are explicit.
- Stable intervals increase confidence.
- Amount variation is handled sensibly.
- Frequent-but-irregular merchants are not automatically recurring.
- Results contain deterministic reasons.
- Confidence is presented as a score, not unsupported probability.
- Controlled recurring and non-recurring examples exist.
- False positives are inspected.
- `04_recurring_transactions.ipynb` runs end-to-end.
- Tests pass.

## Pull Request

### Title

```text
feat(analytics): detect recurring transactions
```

### Description

```markdown
## Summary

Implemented deterministic recurring transaction detection based on merchant, amount and interval behaviour.

## Motivation

Describe the EDA findings that motivated the feature.

## Detection Strategy

Describe candidate generation, interval analysis, amount analysis, minimum-history rules and scoring.

## Evaluation

Describe controlled test cases and false-positive inspection.

## Results

Summarise the strongest recurring patterns.

## Limitations

Describe dataset and methodology limitations.

## Testing

List commands executed and results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that ML, LLMs, recommendations, Android integration and API work were not implemented.

## Follow-up

Next PR should focus on the next analytical capability based on the EDA and product requirements.
```

## Engineering Reflection

Before opening the PR, answer:

1. Are we detecting recurring behaviour rather than frequent merchants?
2. Are interval and amount consistency both considered?
3. Are thresholds justified?
4. Are small amount variations handled appropriately?
5. Are false positives investigated?
6. Is the score explainable?
7. Could this work on another user's history?
8. Did we avoid unnecessary ML?
9. Is the result useful for a future Insights layer?

## Stop Condition

After implementation, tests, notebook execution, evaluation, documentation and PR preparation are complete:

**STOP.**

Do not implement recommendations, LLM insights, API integration or Android integration.

Wait for human review.
