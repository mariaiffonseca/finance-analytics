# PR-011 — Merchant Normalisation & Transaction Categorisation

| Field | Value |
|---|---|
| Sprint | 04 - Analytical Features |
| PR | PR-011 |
| Status | Ready |
| Goal | Establish reliable merchant normalisation and transaction categorisation |
| Depends On | PR-008 — EDA, PR-009 — Anomaly Detection, PR-010 — Recurring Detection |

## Objective

Implement the first reusable transaction-enrichment layer.

```text
Raw transaction
      ↓
Merchant normalisation
      ↓
Category assignment
      ↓
Enriched transaction
```

This layer will support future analytics such as spending trends, recurring payments, anomaly detection and insights.

Do not build a sophisticated ML categorisation system unless the data demonstrates that deterministic rules are insufficient.

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

Also inspect the current analytics code and test dataset.

## Critical Rule

Do not create an unnecessarily complex categorisation model.

First determine what the dataset actually requires. If merchant/category values are already reliable, preserve them and focus on a clean reusable enrichment pipeline. If inconsistencies exist, fix only those supported by evidence.

## 1. Merchant Normalisation

Create a deterministic merchant-normalisation pipeline.

Potential examples:

```text
NETFLIX
Netflix
netflix.com
→ Netflix
```

Do not assume similar strings represent the same merchant without evidence. Avoid fuzzy matching unless explicitly justified by the EDA.

Preserve the original raw merchant.

## 2. Categorisation

Define a category taxonomy consistent with the existing product/domain model.

Categorisation should initially be deterministic.

Possible signals:

- normalised merchant;
- raw merchant;
- transaction description;
- existing category;
- amount/context only when justified.

Do not categorise using amount alone without strong justification.

## 3. Unknown / Uncertain Transactions

Do not force every transaction into a category.

Use an explicit fallback consistent with the domain model, such as `Unknown`, `Other`, or `Uncategorised`.

Where useful expose:

```text
category_confidence
categorisation_method
```

Do not call confidence a probability unless calibrated.

## 4. Categorisation Strategy

Implement and document a clear priority order, for example:

```text
Known merchant rule
        ↓
Known description rule
        ↓
Existing trusted category
        ↓
Fallback
```

Use the actual data and requirements to determine the final order.

## 5. Data Quality

Analyse and document:

- missing merchants;
- blank descriptions;
- casing inconsistencies;
- punctuation differences;
- obvious merchant aliases;
- unknown categories;
- conflicting category assignments.

Do not silently discard problematic rows.

## 6. Enriched Transaction Model

Create a reusable representation containing, as appropriate:

```text
transaction_id
raw_merchant
normalised_merchant
category
category_confidence
categorisation_method
```

Preserve all raw values.

## 7. Evaluation

Create deterministic validation cases covering:

- casing differences;
- whitespace differences;
- known aliases;
- already-normalised merchant;
- unknown merchant;
- clearly known category;
- ambiguous category;
- missing merchant/description;
- conflicting signals.

The same input must always produce the same output.

## 8. Dataset Evaluation

Calculate and document:

```text
% transactions with normalised merchant
% transactions categorised
% transactions remaining unknown
number of merchant aliases
number of unique raw merchants
number of unique normalised merchants
```

Where appropriate compare before and after normalisation.

Do not claim model accuracy without labelled ground truth.

## 9. Notebook

Create:

```text
notebooks/05_merchant_and_category_analysis.ipynb
```

Include:

1. Data quality assessment
2. Merchant inconsistencies
3. Normalisation strategy
4. Before/after merchant statistics
5. Category distribution
6. Unknown/uncertain transactions
7. Categorisation strategy
8. Validation examples
9. Limitations
10. Implications for future analytics

Use Plotly where useful.

## 10. Implementation

Add reusable code under:

```text
analytics/src/finance_analytics/
```

A reasonable structure is:

```text
enrichment/
├── __init__.py
├── merchants.py
├── categories.py
└── models.py
```

Adapt to the existing codebase and avoid an oversized rules engine.

Keep mappings/configuration separate from processing logic where practical.

## 11. Relationship With Previous PRs

PR-009 and PR-010 must remain valid.

If normalisation improves their results, do not silently rewrite previous work. Implement the enrichment layer, validate its impact, and document whether future versions should consume normalised data.

Only refactor previous detectors if a clear correctness issue is discovered.

## 12. Testing

Add tests for:

### Merchant normalisation
- aliases;
- casing;
- whitespace;
- unknown values;
- null/empty values.

### Categorisation
- deterministic rules;
- fallback;
- conflicting signals;
- unknown values.

### Enrichment
- raw values preserved;
- normalised values populated correctly;
- category information deterministic.

### Regression

Run the existing analytics test suite. Do not break PR-009 or PR-010 behaviour.

## Product Boundary

This PR creates an analytical enrichment layer.

Do NOT yet:

- build a production ML categorisation model;
- call an LLM;
- display categories in Android;
- persist enriched data in Room;
- expose an API;
- generate recommendations.

## Out of Scope

Do NOT implement:

- LLM categorisation
- embedding-based merchant matching
- complex fuzzy matching
- recommendations
- financial advice
- Android integration
- API
- Room persistence
- forecasting

## Acceptance Criteria

- Raw merchant values are preserved.
- Normalised merchant values are deterministic.
- Known aliases are handled.
- Unknown merchants remain safe and identifiable.
- Normalisation does not incorrectly merge unrelated merchants.
- Category taxonomy is consistent with the project.
- Categorisation is deterministic.
- Unknown/uncertain transactions are supported.
- Every rule is explainable.
- Before/after data-quality metrics are documented.
- No data is silently discarded.
- Unit and regression tests pass.
- Notebook runs from start to finish.
- Documentation and limitations are explicit.

## Pull Request

### Title

```text
feat(analytics): add merchant normalisation and categorisation
```

### Description

```markdown
## Summary

Implemented deterministic merchant normalisation and transaction categorisation.

## Motivation

Describe the data-quality problems identified in the EDA.

## Merchant Normalisation

Describe the normalisation strategy and known aliases.

## Categorisation

Describe the taxonomy, rule priority and fallback behaviour.

## Evaluation

Include before/after metrics and validation cases.

## Limitations

Describe unknown merchants, ambiguous categories and dataset limitations.

## Testing

List commands executed and results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that LLM categorisation, complex ML, Android integration and API work were not implemented.

## Follow-up

Use the enriched transaction layer as an input to the future Insights Engine.
```

## Engineering Reflection

Before opening the Pull Request, answer:

1. Did we preserve all raw transaction information?
2. Are normalisation rules deterministic?
3. Did we avoid incorrectly merging merchants?
4. Can uncertain transactions remain uncertain?
5. Is the category taxonomy consistent with the product?
6. Did we avoid unnecessary ML?
7. Did we quantify the improvement?
8. Did existing anomaly and recurring tests remain valid?
9. Is this enrichment layer reusable by future analytics?

## Stop Condition

After implementation, tests, notebook execution, evaluation, documentation and PR preparation are complete:

**STOP.**

Do not implement the Insights Engine, API or Android integration.

Wait for human review.
