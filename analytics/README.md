# Finance Analytics — Python Analytics Workspace

Python workspace for analysing transaction data exported from the Finance
Analytics Android application. This is the analytics **foundation**: CSV
loading, schema validation, data-quality reporting, DuckDB querying,
exploratory data analysis of transaction behaviour (temporal, category,
merchant, outlier and recurring-transaction candidates), three analytical
features — transaction anomaly detection (`src/finance_analytics/anomalies/`),
recurring-transaction detection (`src/finance_analytics/recurring/`), both
non-ML, deterministic baselines — and a reusable merchant-normalisation and
transaction-categorisation enrichment layer
(`src/finance_analytics/enrichment/`), also deterministic and non-ML. It
does not implement machine learning, forecasting, or any other insight
generation — see [Out of scope](#out-of-scope).

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
implemented, and adding it now would be an unused dependency.
`sklearn.ensemble.IsolationForest` was considered for anomaly detection and
rejected: the available fixture (18 clean transactions) is far too small to
train or validate a model against. Recurring-transaction detection (PR-010)
made the same call for the same reason — no merchant in the fixture has
more than 2 occurrences. Scikit-learn will be added in the PR that first
has enough data to justify a model.

## Project layout

```text
analytics/
├── pyproject.toml           Dependencies, tool config (uv-managed)
├── notebooks/
│   ├── 01_data_quality_and_overview.ipynb
│   ├── 02_exploratory_data_analysis.ipynb
│   ├── 03_anomaly_detection.ipynb
│   ├── 04_recurring_transactions.ipynb
│   └── 05_merchant_and_category_analysis.ipynb
├── src/finance_analytics/
│   ├── io/csv.py             CSV → DataFrame loading
│   ├── validation/transactions.py   Schema validation
│   ├── data/schema.py        Expected transaction columns
│   ├── data/quality.py       Data-quality report
│   ├── duckdb_queries.py     DuckDB registration + demo queries
│   ├── analysis/              Exploratory analysis building blocks
│   │   ├── temporal.py        Calendar feature generation
│   │   ├── category.py        Category-level aggregation
│   │   ├── merchant.py        Merchant-level aggregation
│   │   ├── outliers.py        IQR / robust z-score / category-relative outlier flags
│   │   └── recurring.py       Recurring-transaction candidate table (exploratory, PR-008)
│   ├── anomalies/              Transaction anomaly detection (PR-009)
│   │   ├── features.py         Historical, leakage-free merchant/category/global stats
│   │   ├── detector.py         AnomalyResult + hierarchical robust-z detection
│   │   └── explanations.py     Deterministic, template-based explanation text
│   ├── recurring/               Recurring-transaction detection (PR-010)
│   │   ├── features.py         Per-merchant candidate aggregation (occurrences, amount/interval stats)
│   │   ├── scoring.py           Confidence-score signals + frequency banding
│   │   ├── detector.py          RecurringResult + classification thresholds
│   │   └── explanations.py     Deterministic, template-based explanation text
│   └── enrichment/              Merchant normalisation + categorisation (PR-011)
│       ├── merchants.py         normalise_merchant() + curated (currently empty) alias table
│       ├── categories.py        Taxonomy, merchant/description rule tables, categorise()
│       ├── explanations.py     Deterministic, template-based explanation text
│       └── models.py            EnrichedTransaction + enrich_transactions() entry point
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

All five notebooks load `data/raw/finance_analytics_test_transactions.csv`
— a synthetic fixture that includes a duplicate transaction and a few
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
- `notebooks/03_anomaly_detection.ipynb` — applies notebook 02's findings
  to select and justify a transaction anomaly detection method, then runs
  it on the clean fixture with controlled synthetic validation cases and a
  false-positive inspection. See its "Limitations" section — the same
  18-row dataset limits what this result generalises to.
- `notebooks/04_recurring_transactions.ipynb` — applies notebook 02's
  recurring-transaction findings (RQ5) to a deterministic recurring-payment
  classifier (`finance_analytics.recurring`), with controlled synthetic
  validation cases (a clear subscription, a frequent-but-irregular
  merchant, a coincidental-amount stress test) and a false-positive
  inspection. See its "Limitations" section — the same 18-row dataset means
  no real merchant here reaches the "Recurring" tier; that tier is only
  demonstrated synthetically.
- `notebooks/05_merchant_and_category_analysis.ipynb` — builds and
  evaluates the merchant-normalisation and transaction-categorisation
  enrichment layer (`finance_analytics.enrichment`). Unlike notebooks 02-04,
  it does **not** drop the duplicate/invalid/QA-fixture rows first — PR-011
  is specifically about handling that kind of row safely. Finds that this
  fixture's raw `merchant`/`category` values are already reliable (no
  casing, punctuation or aliasing problems), so the pipeline's contribution
  here is a reusable, tested, explainable priority chain rather than a data
  cleanup. See its "Limitations" section for what remains unvalidated
  (real aliasing, a larger merchant vocabulary, production-scale data).

To reproduce non-interactively:

```bash
cd analytics
uv run jupyter execute --inplace notebooks/01_data_quality_and_overview.ipynb
uv run jupyter execute --inplace notebooks/02_exploratory_data_analysis.ipynb
uv run jupyter execute --inplace notebooks/03_anomaly_detection.ipynb
uv run jupyter execute --inplace notebooks/04_recurring_transactions.ipynb
uv run jupyter execute --inplace notebooks/05_merchant_and_category_analysis.ipynb
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
- **Anomaly detection is a hierarchical, historical robust z-score, not
  ML.** `anomalies/detector.py` scores each transaction against the most
  specific baseline with enough prior history to trust it — merchant, then
  category, then a global fallback, then `insufficient_history` if none
  qualify — using the same median/MAD estimator PR-008's EDA validated for
  this data's skew. Every baseline is built only from transactions strictly
  before the one being scored (`anomalies/features.py`), so a transaction
  never influences its own baseline. See
  `notebooks/03_anomaly_detection.ipynb` for the full method comparison and
  rationale.
- **No LLM in anomaly explanations.** `anomalies/explanations.py` is
  template-based: the same input always produces the same sentence, and the
  sentence always names the specific baseline (merchant/category/global)
  the score came from.
- **Recurring detection is rule-based, not ML.** `recurring/detector.py`
  classifies each merchant (Recurring / Possible recurring / Not recurring /
  Insufficient history) from a confidence score that blends interval
  consistency, amount consistency, occurrence count and history span
  (`recurring/scoring.py`) against explicit, EDA-justified thresholds. A
  candidate can only reach "Recurring" with at least 3 occurrences — with
  exactly 2, interval *variation* is structurally uncomputable (one
  interval), so PR-008's EDA finding that "consistency needs at least 3
  occurrences" is enforced as a hard floor, not just a scoring input. See
  `notebooks/04_recurring_transactions.ipynb` for the full rationale and a
  documented false-positive limitation.
- **`recurring/` and `anomalies/` are independent.** Neither module imports
  the other. PR-010 explicitly defers the combined "recurring payment with
  an anomalous amount" insight to future work.
- **No LLM in recurring explanations.** `recurring/explanations.py` is
  template-based, same convention as `anomalies/explanations.py`.
- **Merchant normalisation is deterministic and evidence-driven, not
  fuzzy.** `enrichment/merchants.py` only trims/collapses whitespace and
  resolves an explicit, curated alias table by casefolded lookup — no edit
  distance, no embeddings. The alias table (`MERCHANT_ALIASES`) is empty:
  the project's fixture has no casing/punctuation merchant variants to seed
  it with (see `notebooks/05_merchant_and_category_analysis.ipynb`,
  sections 1-2). Real aliases should be added only with the same kind of
  observed evidence, never speculatively.
- **Categorisation is a four-tier deterministic priority chain**, first
  match wins: known merchant rule -> known description rule -> existing
  trusted category -> fallback (`"Uncategorised"`). `enrichment/categories.py`
  documents why each rule table is populated (or deliberately left thin) —
  every entry traces back to observed, unambiguous evidence in the fixture.
  A transaction is never forced into a guessed category.
- **`category_confidence` is a fixed score per method tier, not a
  calibrated probability** — same convention as `recurring/scoring.py`'s
  confidence score.
- **`enrichment/` does not modify `anomalies/` or `recurring/`.**
  Normalisation is a verified no-op on the current fixture (notebook 05,
  section 4), so there is no evidence of a correctness issue to justify
  refactoring either module to consume normalised merchant names yet — see
  notebook 05's "Implications for Future Analytics" for what a future PR
  should do once real aliasing evidence exists.

## Out of scope

Not implemented in this workspace (see PR-007/PR-008/PR-009/PR-010/PR-011
for the full list): general machine learning, forecasting, clustering,
fraud detection, financial advice, ML/LLM categorisation, fuzzy/embedding-based
merchant matching, recommendations, LLM-generated insights, production
dashboard analytics, Android/Python runtime integration, backend/API, cloud
data storage, and the combined recurring+anomaly insight. Anomaly detection,
recurring-transaction detection and merchant normalisation/categorisation
are all implemented, but only as analytical results — see
`anomalies/detector.py` / `recurring/detector.py` / `enrichment/models.py`'s
docstrings and notebooks 03 / 04 / 05's "Out of Scope — Confirmation"
sections for what they deliberately do not do (persistence, Android
display, production API).
