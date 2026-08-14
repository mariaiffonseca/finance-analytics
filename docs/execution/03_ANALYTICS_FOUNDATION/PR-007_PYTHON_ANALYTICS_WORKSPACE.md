# PR-007 — Python Analytics Workspace

| Field | Value |
|---|---|
| Sprint | 03 - Analytics Foundation |
| PR | PR-007 |
| Status | Ready |
| Goal | Establish the Python analytics workspace and reproducible data pipeline |
| Depends On | PR-006 — CSV Import & Ingestion |

## Objective

Create the Python analytics workspace used to analyse transaction data produced by the Android application.

This PR establishes the analytics foundation only. Do not implement production ML, advanced insight generation, or Android/Python runtime integration.

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
9. `docs/execution/02_DATA_FOUNDATION/PR-006_CSV_IMPORT_INGESTION.md`

Also inspect `finance_analytics_test_transactions.csv`.

## Analytics Stack

| Area | Technology |
|---|---|
| DataFrames | Pandas |
| SQL Analytics | DuckDB |
| ML | Scikit-learn |
| Visualization | Plotly |
| Experiments | Jupyter |

Do not replace these technologies without a documented reason.

## 1. Project Structure

Create a dedicated analytics workspace separate from Android:

```text
analytics/
├── README.md
├── pyproject.toml
├── notebooks/
│   └── 01_data_quality_and_overview.ipynb
├── src/
│   └── finance_analytics/
│       ├── __init__.py
│       ├── io/
│       │   └── csv.py
│       ├── validation/
│       │   └── transactions.py
│       └── data/
│           └── schema.py
├── tests/
│   ├── test_csv.py
│   └── test_validation.py
└── data/
    ├── raw/
    ├── processed/
    └── README.md
```

Keep this independent from the Android application. Do not create a shared Kotlin/Python module.

## 2. Python Environment

Configure a reproducible Python environment with:

- Explicit Python version
- Runtime dependencies
- Development/test dependencies
- A modern `pyproject.toml`
- Tests runnable from the repository root

Do not add unnecessary dependencies.

## 3. Data Pipeline

Establish:

```text
Raw CSV
  ↓
Input validation
  ↓
Pandas DataFrame
  ↓
Data quality checks
  ↓
Clean transaction dataset
  ↓
DuckDB / analytical queries
  ↓
EDA / feature engineering
  ↓
ML / insights
```

Only implement the first stages in this PR.

## 4. CSV Loading

Implement a small reusable Pandas CSV loader.

Requirements:

- Parse dates explicitly.
- Preserve monetary precision appropriately.
- Do not silently coerce malformed values into valid values.
- Keep loading separate from analytics logic.

## 5. Schema Validation

Validate the transaction structure against `docs/project/02_DOMAIN_MODEL.md`.

Expected concepts:

```text
id
date
amount
currency
description
merchant
category
account
```

Detect:

- Missing columns
- Structural problems
- Invalid dates
- Invalid amounts
- Missing required values

Validation must be independent from notebooks.

## 6. Data Quality Report

Create a reusable report containing at least:

- Row/column counts
- Missing values by column
- Duplicate rows
- Duplicate transaction IDs
- Invalid dates
- Invalid amounts
- Unique merchants
- Unique categories
- Date range
- Income vs expense counts

This is a **data quality report**, not an analytics report.

## 7. DuckDB Foundation

Add DuckDB and allow the transaction DataFrame to be queried through it.

Demonstrate:

- Total transaction count
- Total income
- Total expenses
- Transactions by category
- Transactions by month

Keep SQL separate from notebooks where practical. Do not create a complex data warehouse.

## 8. First Notebook

Create:

```text
notebooks/01_data_quality_and_overview.ipynb
```

It must:

1. Load the test CSV.
2. Run schema validation.
3. Display the data-quality report.
4. Show basic descriptive statistics.
5. Inspect the date range.
6. Inspect category distribution.
7. Compare income and expenses.
8. Run a few DuckDB queries.
9. Create at least one useful Plotly visualisation.

The notebook should be readable as a portfolio artefact. Avoid unnecessary charts.

## 9. Testing

Add focused tests for:

### CSV loading
- Valid CSV loads successfully.
- Dates are parsed correctly.
- Malformed amounts are detected.
- Malformed dates are detected.

### Schema validation
- Missing required columns are detected.
- Required fields are checked.
- Structural problems are handled.

### Data quality
- Duplicate rows are detected.
- Duplicate IDs are detected.
- Missing values are counted.

### DuckDB
- DataFrame can be queried.
- Basic aggregations return expected results.

Keep tests deterministic.

## 10. Reproducibility

A new developer should be able to:

```text
install dependencies
    ↓
run tests
    ↓
open notebook
    ↓
load test data
    ↓
reproduce results
```

Document exact commands in `analytics/README.md`.

## 11. Data Handling

Financial data is sensitive.

- Do not commit private financial data.
- Keep raw-data rules explicit.
- Add appropriate `.gitignore` rules.
- Never add credentials or personal banking information.
- The supplied synthetic/test CSV may be committed as a fixture if useful.

## Out of Scope

Do NOT implement:

- Machine learning models
- Anomaly detection
- Forecasting
- Clustering
- Recurring-payment detection
- Merchant normalisation
- Automated categorisation
- LLM-generated insights
- Recommendations
- Production dashboard analytics
- Android/Python runtime integration
- Backend/API
- Cloud data storage

## Acceptance Criteria

### Workspace
- Python analytics workspace exists.
- `pyproject.toml` is configured.
- Dependencies are reproducible.
- Tests run from the repository root.

### Data
- Test CSV loads successfully.
- Schema validation works.
- Invalid values are detected.
- Data-quality report is generated.

### DuckDB
- Pandas data can be queried through DuckDB.
- Basic analytical queries work.

### Notebook
- `01_data_quality_and_overview.ipynb` exists.
- Notebook runs from start to finish.
- Results are reproducible.
- At least one Plotly visualisation is included.

### Testing
- Unit tests pass.
- Android project remains unaffected.

## Pull Request

### Title

```text
feat(analytics): establish python analytics workspace
```

### Description

```markdown
## Summary

Established the Python analytics workspace and reproducible transaction data pipeline.

## Changes

- Added Python analytics workspace
- Added Pandas CSV ingestion
- Added transaction schema validation
- Added data-quality reporting
- Added DuckDB integration
- Added initial Jupyter notebook
- Added Plotly visualisation
- Added analytics tests

## Data Source

Describe the test CSV used.

## Testing

List commands executed and results.

## Reproducibility

Describe how to install dependencies and run the notebook.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that ML, anomaly detection, forecasting, categorisation and insight generation were not implemented.

## Follow-up

Next PR should focus on exploratory data analysis and transaction behaviour.
```

## Engineering Reflection

Before opening the PR, answer:

1. Is the analytics workspace independent from Android?
2. Is CSV loading separated from validation?
3. Are malformed values detected rather than silently coerced?
4. Can the notebook be reproduced from a clean environment?
5. Is DuckDB used where SQL improves analytical clarity?
6. Is the notebook useful as a portfolio artefact?
7. Have we avoided premature ML?
8. Could this structure support future feature engineering without becoming over-engineered?

## Stop Condition

After implementation, tests, notebook execution, documentation updates and PR preparation are complete:

**STOP.**

Do not implement anomaly detection, forecasting, categorisation or ML.

Wait for human review.
