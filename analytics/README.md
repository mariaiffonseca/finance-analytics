# Finance Analytics — Python Analytics Workspace

Python workspace for analysing transaction data exported from the Finance
Analytics Android application. This is the analytics **foundation**: CSV
loading, schema validation, data-quality reporting, DuckDB querying, and
exploratory data analysis of transaction behaviour (temporal, category,
merchant, outlier and recurring-transaction candidates). It does not
implement machine learning, forecasting, production anomaly detection or
any other insight generation — see [Out of scope](#out-of-scope).

This workspace is independent from the Android application: no shared code,
no runtime integration. It reads CSV exports, nothing else.

## Stack

| Area | Technology |
|---|---|
| Package/environment management | [uv](https://docs.astral.sh/uv/) |
| DataFrames | Pandas |
| SQL analytics | DuckDB |
| Visualisation | Plotly |
| Experiments | Jupyter (JupyterLab) |
| Tests | pytest |
| Lint/format | Ruff |

Scikit-learn is part of the project's overall analytics stack but is
intentionally **not** a dependency of this workspace yet — no ML is
implemented in this PR, and adding it now would be an unused dependency. It
will be added in the PR that first implements a model.

## Project layout

```text
analytics/
├── pyproject.toml           Dependencies, tool config (uv-managed)
├── notebooks/
│   ├── 01_data_quality_and_overview.ipynb
│   └── 02_exploratory_data_analysis.ipynb
├── src/finance_analytics/
│   ├── io/csv.py             CSV → DataFrame loading
│   ├── validation/transactions.py   Schema validation
│   ├── data/schema.py        Expected transaction columns
│   ├── data/quality.py       Data-quality report
│   ├── duckdb_queries.py     DuckDB registration + demo queries
│   └── analysis/              Exploratory analysis building blocks
│       ├── temporal.py        Calendar feature generation
│       ├── category.py        Category-level aggregation
│       ├── merchant.py        Merchant-level aggregation
│       ├── outliers.py        IQR / robust z-score / category-relative outlier flags
│       └── recurring.py       Recurring-transaction candidate table
├── tests/                    pytest suite (+ tests/fixtures/ CSVs)
└── data/
    ├── raw/                  Untouched CSV exports (gitignored except the sample fixture)
    ├── processed/            Pipeline output (gitignored)
    └── README.md             Data-handling rules
```

## Setup

Requires [uv](https://docs.astral.sh/uv/getting-started/installation/).
Python itself does not need to be pre-installed — `uv sync` provisions the
exact interpreter pinned in `.python-version`.

```bash
cd analytics
uv sync
```

## Running tests

```bash
cd analytics
uv run pytest
```

Or without changing directories, from the repository root:

```bash
uv run --project analytics pytest
```

## Lint / format

```bash
cd analytics
uv run ruff check .
uv run ruff format .
```

## Running the notebooks

```bash
cd analytics
uv run jupyter lab
```

Both notebooks load `data/raw/finance_analytics_test_transactions.csv` — a
synthetic fixture that includes a duplicate transaction and a few
deliberately invalid rows so the validation and data-quality steps have
something to report.

- `notebooks/01_data_quality_and_overview.ipynb` — load, validate,
  data-quality report, first descriptive look.
- `notebooks/02_exploratory_data_analysis.ipynb` — exploratory analysis of
  transaction behaviour (temporal, category, merchant, distribution,
  outliers, recurring-transaction candidates, income/savings) framed as
  research questions with evidence-based findings. Built on the same
  fixture as notebook 01 — a 22-row, 46-day dataset — so most sections
  explicitly caveat what can and can't be concluded at that size; see its
  own "Limitations" section for details.

To reproduce non-interactively:

```bash
cd analytics
uv run jupyter execute --inplace notebooks/01_data_quality_and_overview.ipynb
uv run jupyter execute --inplace notebooks/02_exploratory_data_analysis.ipynb
```

## Reproducibility

```text
uv sync                 install dependencies
uv run pytest           run tests
uv run jupyter lab      open notebooks
                         load data/raw/finance_analytics_test_transactions.csv
                         re-run all cells to reproduce results
```

## Engineering decisions

- **Amounts as float64.** Transaction amounts have at most two decimal
  places; float64 represents these exactly, so no fixed-point/Decimal type
  is needed for analytics (as opposed to accounting-grade ledger code).
- **No silent coercion.** `finance_analytics.io.csv` parses `date` and
  `amount` explicitly; values that don't match become `NaT`/`NaN` rather
  than a guessed or truncated valid value. `finance_analytics.validation`
  and `finance_analytics.data.quality` are what surface and count those
  rows — the loader never drops or hides them.
- **Loading vs. validation vs. quality reporting are separate modules.**
  `io.csv` only parses; `validation.transactions` only checks structure
  against the expected schema; `data.quality` only summarises. Each is
  usable independently of the notebook and of each other.
- **`analysis/*` functions are expense-scoped by default.** `category`,
  `merchant`, `outliers` and `recurring` all analyse spending, so `Income`
  rows (positive amounts) are excluded from their aggregations rather than
  distorting a "what drives spending" view with a fundamentally different
  kind of transaction. Callers who need income included work from the raw
  DataFrame or `duckdb_queries` directly.
- **`docs/project/02_DOMAIN_MODEL.md` does not exist yet** in this
  repository. `src/finance_analytics/data/schema.py` uses the transaction
  concepts listed directly in PR-007
  (`id, date, amount, currency, description, merchant, category, account`)
  as the schema source of truth instead.

## Out of scope

Not implemented in this workspace (see PR-007/PR-008 for the full list):
machine learning, production anomaly detection, forecasting, clustering,
the final recurring-payment detector, merchant normalisation, automated
categorisation, LLM-generated insights, recommendations, production
dashboard analytics, Android/Python runtime integration, backend/API,
cloud data storage.
