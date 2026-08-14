# PR-008 — Exploratory Data Analysis & Transaction Behaviour

| Field | Value |
|---|---|
| Sprint | 03 - Analytics Foundation |
| PR | PR-008 |
| Status | Ready |
| Goal | Perform rigorous exploratory analysis of transaction behaviour |
| Depends On | PR-007 — Python Analytics Workspace |

## Objective

Use the established Python analytics workspace to understand the structure, quality and behavioural patterns of the transaction data before building analytical features or ML models.

This PR is about asking questions of the data and documenting what the data actually shows.

```text
Question
  ↓
Hypothesis
  ↓
Analysis
  ↓
Evidence
  ↓
Interpretation
  ↓
Decision / next question
```

Do not jump directly to machine learning.

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

Also inspect the available test dataset.

## Important Principle

Do not manufacture conclusions to match the UI prototype.

The prototype contains concepts such as spending trends, unusual purchases, recurring subscriptions, category changes and savings rate. These are product concepts, not evidence from the real dataset.

The prototype uses generated deterministic transaction data and predefined behavioural patterns. Treat those concepts as questions to investigate, not expected answers. fileciteturn8file7L307-L338

## 1. EDA Notebook

Create:

```text
notebooks/02_exploratory_data_analysis.ipynb
```

Structure it as a narrative:

```text
1. Research Questions
2. Dataset Overview
3. Data Quality
4. Temporal Behaviour
5. Spending Distribution
6. Category Behaviour
7. Merchant Behaviour
8. Recurring Transactions
9. Outlier Investigation
10. Income / Expense Behaviour
11. Key Findings
12. Implications for Feature Engineering
```

## 2. Research Questions

Investigate at minimum:

### RQ1 — How does spending evolve over time?

Analyse monthly spending, income, net cash flow, transaction volume and month-over-month changes.

Explain what changes mean rather than only plotting totals.

### RQ2 — Which categories drive spending?

Analyse total spending, share of total spending, monthly evolution and volatility.

Classify patterns as appropriate: consistently high, increasing, decreasing or highly variable.

### RQ3 — What does transaction behaviour look like?

Analyse amount distribution, median vs mean, transaction count, small vs large transactions and distributions by category.

Do not rely only on averages.

### RQ4 — Which merchants dominate spending?

Analyse top merchants by total spend and transaction count, plus average transaction amount.

Distinguish many small transactions from few large transactions.

### RQ5 — Are there recurring transactions?

Investigate repeated merchants, similar amounts, similar intervals and repeated monthly occurrences.

Do not build the final recurring-payment detector yet.

### RQ6 — Are there unusual transactions?

Investigate potential outliers using approaches such as IQR, robust z-score and category-relative thresholds.

Do not implement a production anomaly detector yet.

A large transaction is not automatically an anomaly.

### RQ7 — What can we learn about income and savings?

Analyse income, expenses, net cash flow, savings rate and income variability.

Do not provide financial advice.

## 3. Statistical Analysis

Use appropriate descriptive statistics:

- Mean
- Median
- Standard deviation
- Quantiles
- IQR
- Min/max

Where useful, compare mean vs median and explain skewness.

Do not add statistical tests without an analytical reason.

## 4. Temporal Analysis

Create reusable temporal features where appropriate:

```text
year
month
month_period
day_of_week
day_of_month
```

Use them to investigate monthly patterns, weekday/weekend behaviour and transaction frequency.

Do not claim seasonality from a dataset too short to support it.

## 5. Category Analysis

Create a category-level analytical table containing useful metrics such as:

```text
category
transaction_count
total_spend
mean_transaction
median_transaction
share_of_spend
monthly_volatility
```

Use it as a foundation for later feature engineering.

## 6. Merchant Analysis

Create a merchant-level analytical view:

```text
merchant
transaction_count
total_spend
mean_transaction
median_transaction
```

Identify merchants that may be candidates for recurring detection, merchant normalisation and anomaly analysis.

Do not implement those systems yet.

## 7. Outlier Investigation

Investigate whether extreme values are:

- genuine large purchases;
- data-quality problems;
- unusual behaviour;
- category-specific normal behaviour.

Document examples and reasoning.

## 8. Recurring Transaction Investigation

Create an exploratory candidate table where possible:

```text
merchant
occurrences
amount_variation
median_interval_days
interval_variation
```

Use this to assess whether recurring detection is feasible.

Do not build the final detector.

## 9. Visualisation

Use Plotly.

At minimum include:

- Monthly income vs expenses
- Spending by category
- Distribution of transaction amounts
- Top merchants
- Category trend over time
- At least one outlier-focused visualisation

Prefer interactive charts where useful. Avoid decorative charts.

## 10. DuckDB

Use DuckDB where SQL makes analysis clearer.

Demonstrate at least one non-trivial analytical query, such as monthly category aggregation, top merchants, category share, or monthly income vs expenses.

Do not move all analysis into SQL just because DuckDB is available.

## 11. Findings

End the notebook with:

### Observed
What the data directly shows.

### Interpretation
What observations may mean.

### Limitations
What the dataset cannot tell us.

### Next Questions
What should be investigated next.

## 12. Feature Engineering Candidates

Identify and justify potential future features, such as:

```text
transaction_amount
log_transaction_amount
category_share
merchant_frequency
merchant_amount_deviation
days_since_previous_transaction
transaction_interval
monthly_category_spend
monthly_spend_change
income_change
savings_rate
```

Do not implement the full feature pipeline yet.

## 13. Dataset Limitations

Document:

- Dataset size
- Time span
- Category quality
- Merchant consistency
- Missing values
- Synthetic/test-data limitations
- Lack of external context
- Lack of ground-truth anomaly labels

Do not overstate findings.

## 14. Testing

Add tests for reusable analytical functions created during this PR, for example:

- Temporal feature generation
- Category aggregation
- Merchant aggregation
- Outlier calculations
- Recurring-candidate calculations

Move reusable logic into `src/finance_analytics/` where appropriate.

Do not test notebook cells individually unless tooling requires it.

## Out of Scope

Do NOT implement:

- Production anomaly detection
- Machine learning models
- Clustering
- Forecasting
- Automated categorisation
- LLM insights
- Recommendations
- Android integration
- Production analytics API

## Acceptance Criteria

### Analysis

- Explicit research questions are documented.
- Each question is answered with appropriate analysis.
- Findings are evidence-based.
- Limitations are documented.

### EDA

- Temporal behaviour analysed.
- Category behaviour analysed.
- Merchant behaviour analysed.
- Transaction distributions analysed.
- Recurring behaviour investigated.
- Outliers investigated.
- Income/expense behaviour analysed.

### Visualisation

- Plotly is used.
- Charts answer analytical questions.
- No unnecessary visualisations.

### Feature Engineering

- Candidate features are documented.
- Each candidate has a clear analytical motivation.

### Code Quality

- Reusable analytical logic is extracted from the notebook where appropriate.
- Tests cover reusable functions.
- Notebook runs from start to finish.

## Pull Request

### Title

```text
feat(analytics): explore transaction behaviour
```

### Description

```markdown
## Summary

Performed exploratory data analysis to understand transaction behaviour and establish the foundation for analytical features.

## Research Questions

List the questions investigated.

## Key Findings

Summarise the most important evidence-based findings.

## Feature Engineering Candidates

Summarise the strongest candidates identified.

## Limitations

Describe dataset limitations.

## Testing

List commands executed and results.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that no production ML or anomaly-detection system was implemented.

## Follow-up

Next PR should implement the first analytical feature based on evidence from this EDA.
```

## Engineering Reflection

Before opening the Pull Request, answer:

1. Did we start from questions rather than algorithms?
2. Are conclusions supported by evidence?
3. Did we distinguish observation from interpretation?
4. Did we avoid treating every outlier as an anomaly?
5. Did we avoid overclaiming seasonality?
6. Are feature-engineering candidates justified?
7. Did we keep reusable logic outside the notebook?
8. Is there a clear next analytical feature supported by the EDA?

## Stop Condition

After EDA, notebook execution, tests, documentation and PR preparation are complete:

**STOP.**

Do not implement the next analytical feature.

Wait for human review.
