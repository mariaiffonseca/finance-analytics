# PR-009 — Transaction Anomaly Detection

| Field | Value |
|---|---|
| Sprint | 04 - Analytical Features |
| PR | PR-009 |
| Status | Ready |
| Goal | Implement a defensible transaction anomaly detection feature |
| Depends On | PR-008 — Exploratory Data Analysis & Transaction Behaviour |

## Objective

Implement the first analytical feature of Finance Analytics: detection of potentially unusual transactions.

This PR must be driven by the findings from PR-008. An anomaly means a transaction whose characteristics are unusually different from the user's historical behaviour. It does **not** mean fraud, incorrect spending, or financial harm.

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

Also inspect the completed EDA notebook and findings.

## Critical Rule

Do not blindly implement an algorithm because it appears in this document.

First review the EDA, identify the strongest evidence for detecting unusual transactions, select a method based on that evidence, and document the decision.

If the data is insufficient for a reliable detector, implement the strongest defensible baseline and document its limitations.

## 1. Detection Strategy

Evaluate approaches identified during EDA, as appropriate:

- IQR
- Robust z-score / MAD
- Category-relative thresholds
- Merchant-relative thresholds
- `sklearn.ensemble.IsolationForest` if justified by data volume and EDA

Do not use ML merely because Scikit-learn is available.

Prefer the simplest method that is sufficiently effective and explainable.

Compare methods on:

- Interpretability
- Stability
- Skewed spending
- Small sample sizes
- Category differences
- Explainability

## 2. Historical Context

When evaluating a transaction, use only information available before that transaction where feasible.

Do not let the transaction being scored define its own baseline.

Define explicit minimum-history rules. If merchant/category history is insufficient, fall back to a broader baseline or leave the transaction unscored according to the documented strategy.

## 3. Feature Engineering

Use only features justified by the EDA.

Potential candidates:

```text
amount
log_amount
category-relative amount
merchant-relative amount
days_since_previous_transaction
merchant_frequency
category_frequency
```

Avoid future-data leakage.

## 4. Result Model

Create a structured result containing at minimum:

```text
transaction_id
anomaly_score
is_anomaly
method
reason
reference_context
```

Where useful include:

```text
category_median
category_iqr
merchant_median
merchant_transaction_count
```

Scores must be deterministic.

## 5. Explainability

Every flagged transaction must have a deterministic explanation, for example:

```text
Amount is 3.8× higher than your typical Shopping transaction.
```

Do not use an LLM for explanations in this PR.

## 6. Evaluation

There may be no labelled anomaly dataset.

If no ground truth exists:

- Do not claim accuracy.
- Do not invent precision/recall.
- Use controlled synthetic cases.
- Inspect known injected anomalies in the test data.
- Inspect false positives.

If the supplied test dataset contains an intentionally injected anomaly, use it as a validation case, not as proof of general performance.

## 7. Test Cases

Cover at least:

- Normal transaction
- Large transaction
- Category-specific large transaction
- Merchant-specific deviation
- Insufficient history

The detector should not flag a transaction simply because it is large if that amount is normal for its category/context.

## 8. Implementation

Add reusable analytical code under:

```text
analytics/src/finance_analytics/
```

A reasonable conceptual structure is:

```text
anomalies/
├── __init__.py
├── features.py
├── detectors.py
├── scoring.py
└── explanations.py
```

Use a simpler structure if the existing codebase does not need all these files.

Avoid over-engineering.

## 9. Notebook

Create:

```text
notebooks/03_anomaly_detection.ipynb
```

It must document:

1. Why anomaly detection is useful.
2. What the EDA revealed.
3. Methods considered.
4. Selected method and rationale.
5. Feature engineering.
6. Detection results.
7. Known examples.
8. False-positive inspection.
9. Limitations.
10. Next steps.

Include useful Plotly visualisations such as transaction amount vs score, category distributions with flagged transactions, or a transaction timeline.

## 10. Testing

Add tests for:

### Feature engineering
- Historical features are correct.
- No future information leaks.

### Detection
- Controlled normal cases behave correctly.
- Known synthetic anomalies are detected where expected.
- Insufficient history is handled correctly.

### Scoring
- Scores are deterministic.
- Controlled cases have sensible score ordering.

### Explanations
- Every flagged transaction gets a deterministic explanation.
- The explanation references the correct context.

Do not rely only on notebook output.

## 11. Product Boundary

This PR produces an analytical result only.

Do NOT yet:

- display anomalies in Android;
- generate natural-language insights;
- generate recommendations;
- persist anomaly results in Room;
- call an API.

## Out of Scope

Do NOT implement:

- Fraud detection
- Financial advice
- LLM explanations
- Recommendations
- Recurring-payment detection
- Forecasting
- Automated categorisation
- Android integration
- Production analytics API

## Acceptance Criteria

- Method selection is justified by PR-008.
- At least one defensible baseline is implemented.
- ML is used only if justified.
- Future transactions are not used for historical context.
- Minimum-history rules are explicit.
- Structured anomaly results are produced.
- Flagged transactions have deterministic explanations.
- Controlled test cases exist.
- False positives are inspected.
- No unsupported accuracy claims are made.
- `03_anomaly_detection.ipynb` runs from start to finish.
- Tests pass.

## Pull Request

### Title

```text
feat(analytics): add transaction anomaly detection
```

### Description

```markdown
## Summary

Implemented transaction anomaly detection based on findings from the EDA.

## Motivation

Explain the behavioural pattern discovered in PR-008.

## Method

Describe methods considered, selected method and thresholds.

## Features

List features used and why.

## Evaluation

Describe evaluation strategy and controlled test cases.

## Results

Summarise important findings.

## Limitations

Describe limitations and ground-truth availability.

## Testing

List commands executed and results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that fraud detection, recommendations, LLMs and Android integration were not implemented.

## Follow-up

Next PR should implement the next analytical capability based on the remaining requirements and EDA findings.
```

## Engineering Reflection

Before opening the PR, answer:

1. Did the EDA justify this feature?
2. Is "anomaly" clearly defined?
3. Are we detecting unusual behaviour rather than fraud?
4. Did we avoid data leakage?
5. Are minimum-history rules defensible?
6. Can a user understand why a transaction was flagged?
7. Did we avoid unnecessary ML?
8. Are limitations honest?
9. Could this work on another user's history without hard-coded assumptions?

## Stop Condition

After implementation, tests, notebook execution, evaluation, documentation and PR preparation are complete:

**STOP.**

Do not implement recurring detection, recommendations, LLM insights or Android integration.

Wait for human review.
