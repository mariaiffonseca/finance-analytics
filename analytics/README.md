# Finance Analytics — Python Analytics Workspace

Python workspace for analysing transaction data exported from the Finance
Analytics Android application. This is the analytics **foundation**: CSV
loading, schema validation, data-quality reporting, and DuckDB querying. It
does not implement machine learning, forecasting, anomaly detection or any
other insight generation — see [Out of scope](#out-of-scope).

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
│   └── 01_data_quality_and_overview.ipynb
├── src/finance_analytics/
│   ├── io/csv.py             CSV → DataFrame loading
│   ├── validation/transactions.py   Schema validation
│   ├── data/schema.py        Expected transaction columns
│   ├── data/quality.py       Data-quality report
│   └── duckdb_queries.py     DuckDB registration + demo queries
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

## Running the notebook

```bash
cd analytics
uv run jupyter lab
```

Open `notebooks/01_data_quality_and_overview.ipynb` and run all cells. It
loads `data/raw/finance_analytics_test_transactions.csv` — a synthetic
fixture that includes a duplicate transaction and a few deliberately invalid
rows so the validation and data-quality steps have something to report.

To reproduce non-interactively:

```bash
cd analytics
uv run jupyter execute notebooks/01_data_quality_and_overview.ipynb
```

## Reproducibility

```text
uv sync                 install dependencies
uv run pytest           run tests
uv run jupyter lab      open notebook
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
- **`docs/project/02_DOMAIN_MODEL.md` does not exist yet** in this
  repository. `src/finance_analytics/data/schema.py` uses the transaction
  concepts listed directly in PR-007
  (`id, date, amount, currency, description, merchant, category, account`)
  as the schema source of truth instead.

## Out of scope

Not implemented in this workspace (see PR-007 for the full list):
machine learning, anomaly detection, forecasting, clustering, recurring-
payment detection, merchant normalisation, automated categorisation,
LLM-generated insights, recommendations, production dashboard analytics,
Android/Python runtime integration, backend/API, cloud data storage.
