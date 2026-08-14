# Analytics Data

Financial data is sensitive. This directory separates real, private data from
committed sample data.

## `raw/`

Untouched CSV exports as provided by the source (the Android app, a bank
export, etc.).

- Personal/private CSVs placed here are **never committed** — see the
  `analytics/data/raw/` rule in the repository `.gitignore`.
- The one exception is `finance_analytics_test_transactions.csv`, a synthetic
  fixture (including a few deliberately invalid rows) used by the notebook
  and tests. It contains no real financial data and is safe to commit.

## `processed/`

Intermediate or cleaned output written by notebooks/scripts (e.g. a
validated, deduplicated dataset). Never committed — regenerate it by
re-running the pipeline against a raw CSV.

## Rules

- Never commit real transaction data, credentials, or personal banking
  information.
- Treat every file in `raw/` as private unless it is the documented sample
  fixture above.
