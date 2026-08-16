# Finance Analytics — Python Analytics Workspace

Python workspace for analysing transaction data exported from the Finance
Analytics Android application. This is the analytics **foundation**: CSV
loading, schema validation, data-quality reporting, DuckDB querying,
exploratory data analysis of transaction behaviour (temporal, category,
merchant, outlier and recurring-transaction candidates), three analytical
features — transaction anomaly detection (`src/finance_analytics/anomalies/`),
recurring-transaction detection (`src/finance_analytics/recurring/`), both
non-ML, deterministic baselines — a reusable merchant-normalisation and
transaction-categorisation enrichment layer
(`src/finance_analytics/enrichment/`), also deterministic and non-ML — a
deterministic **Insights Engine** (`src/finance_analytics/insights/`)
that combines all three into structured, evidence-backed `Insight` objects —
and a stateless **FastAPI service** (`src/finance_analytics/api/`) that
exposes the whole pipeline over HTTP. It does not implement machine
learning, forecasting, LLM-generated insights, financial advice,
recommendations, authentication, persistence, or Android integration — see
[Out of scope](#out-of-scope).

This workspace is independent from the Android application: no shared code,
no runtime integration (the API is HTTP-only; nothing here imports or is
imported by Android code).

## Stack

| Area | Technology |
|---|---|
| Package/environment management | [uv](https://docs.astral.sh/uv/) |
| DataFrames | Pandas |
| SQL analytics | DuckDB |
| Visualisation | Plotly |
| API | FastAPI, Pydantic, Uvicorn |
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
│   ├── 05_merchant_and_category_analysis.ipynb
│   └── 06_insights_engine.ipynb
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
│   ├── enrichment/              Merchant normalisation + categorisation (PR-011)
│   │   ├── merchants.py         normalise_merchant() + curated (currently empty) alias table
│   │   ├── categories.py        Taxonomy, merchant/description rule tables, categorise()
│   │   ├── explanations.py     Deterministic, template-based explanation text
│   │   └── models.py            EnrichedTransaction + enrich_transactions() entry point
│   └── insights/                 Financial Behaviour & Insights Engine (PR-012)
│       ├── models.py             Insight model (id/type/severity/confidence/metadata) + to_dict()
│       ├── periods.py            Comparable current-vs-previous month comparison windows
│       ├── rules.py              Spending trend / category change / income-expense / savings-rate rules
│       ├── conversions.py        Adapts PR-009/PR-010 results into Insight objects
│       ├── ranking.py            Deterministic (severity, confidence, recency, id) ranking
│       ├── deduplication.py      Same-fact insight deduplication
│       └── engine.py             generate_insights() — the single entry point
│   └── api/                       Analytics API (PR-013) — transport only, no analytics logic
│       ├── app.py                 FastAPI application factory (`app = create_app()`)
│       ├── schemas.py             Pydantic request/response models (the HTTP contract)
│       ├── service.py             Application/service layer: request -> analytics engine -> response
│       ├── errors.py              Exception -> HTTP status mapping (400 / 422 / 500)
│       └── routes/
│           ├── health.py          GET /health
│           └── analytics.py       POST /analytics/analyse
├── tests/                    pytest suite (+ tests/fixtures/ CSVs, tests/api/ for the API)
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

## Running the API locally

```bash
cd analytics
uv run uvicorn finance_analytics.api.app:app --reload
```

Then, with the server running:

- Interactive docs (Swagger UI): <http://127.0.0.1:8000/docs>
- Raw OpenAPI schema: <http://127.0.0.1:8000/openapi.json>
- Liveness check: `curl http://127.0.0.1:8000/health` → `{"status": "ok"}`

Example request to `POST /analytics/analyse`:

```bash
curl -X POST http://127.0.0.1:8000/analytics/analyse \
  -H "Content-Type: application/json" \
  -d '{
    "transactions": [
      {"id": "1", "date": "2026-01-05", "amount": -12.50, "currency": "EUR",
       "description": "Coffee", "merchant": "Coffee Corner", "category": "Food & Dining",
       "account": "Main Account"},
      {"id": "2", "date": "2026-01-10", "amount": 2000.00, "currency": "EUR",
       "description": "Salary", "merchant": "Employer Payroll", "category": "Income",
       "account": "Main Account"}
    ]
  }'
```

The response contains `summary` (income/expenses/savings totals), `insights`
(from the PR-012 Insights Engine), `anomalies` (flagged `AnomalyResult`s from
PR-009), `recurring` (`"Recurring"`/`"Possible recurring"` merchants from
PR-010) and `metadata` (dataset diagnostics — see `api/schemas.py` for the
full field list, or just read `/docs`).

**Local development only.** No authentication, transport security (TLS) or
production hardening is implemented — do not expose this service outside a
trusted local environment, and do not send real financial data to it. A
production deployment would need authentication, HTTPS, rate limiting and a
privacy review before handling real transactions (PR-013 §16 explicitly
defers all of this).

### Error responses

| Status | Meaning | Body shape |
|---|---|---|
| `400` | Request parsed but is structurally invalid (e.g. a `NaN`/`Infinity` amount) | `{"detail": "<message>"}` |
| `422` | Request failed Pydantic schema validation (missing field, wrong type, invalid date) | FastAPI's default `{"detail": [...]}` |
| `500` | An unexpected analytics-engine failure | `{"detail": "Internal analytics failure."}` — no stack trace or transaction data |

An empty `transactions` list is rejected as `422` — there is nothing to
analyse, so it is treated as an invalid request rather than a request that
produces an empty response.

## Running the notebooks

```bash
cd analytics
uv run jupyter lab
```

All six notebooks load `data/raw/finance_analytics_test_transactions.csv`
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
- `notebooks/06_insights_engine.ipynb` — builds and evaluates the
  Insights Engine (`finance_analytics.insights`), which combines PR-009's
  anomaly results, PR-010's recurring results and PR-011's enrichment
  output with two new families of period-comparison rules
  (spending trend, category change, income/expense change, savings rate
  change) into structured `Insight` objects. On this project's own
  18-row fixture, `category_change`/`income_expense_change`/
  `savings_rate_change` are all evidence-suppressed (too few transactions
  per category window; no income before day 15 of either comparable
  month) and demonstrated only synthetically — the same kind of
  data-size limitation notebooks 03/04 already documented for their own
  detectors, not a bug. See its "Limitations" section.

To reproduce non-interactively:

```bash
cd analytics
uv run jupyter execute --inplace notebooks/01_data_quality_and_overview.ipynb
uv run jupyter execute --inplace notebooks/02_exploratory_data_analysis.ipynb
uv run jupyter execute --inplace notebooks/03_anomaly_detection.ipynb
uv run jupyter execute --inplace notebooks/04_recurring_transactions.ipynb
uv run jupyter execute --inplace notebooks/05_merchant_and_category_analysis.ipynb
uv run jupyter execute --inplace notebooks/06_insights_engine.ipynb
```

## Reproducibility

```text
uv sync                                                      install dependencies
uv run pytest                                                run tests
uv run jupyter lab                                           open notebooks
                                                               load data/raw/finance_analytics_test_transactions.csv
                                                               re-run all cells to reproduce results
uv run uvicorn finance_analytics.api.app:app --reload        run the API locally
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
- **The Insights Engine consumes, never reimplements.**
  `insights/conversions.py` adapts `anomalies.detect_anomalies` /
  `recurring.detect_recurring_transactions` results directly — every
  score, threshold and explanation comes from PR-009/PR-010 unchanged.
  `insights/engine.py` calls `enrichment.enrich_transactions` once and
  uses its `category` field for `category_change_insights` — the "future
  analytics" PR-011 built its enrichment layer for — while
  `detect_anomalies`/`detect_recurring_transactions` keep consuming the
  original raw `category`/`merchant` columns exactly as those PRs
  validated them.
- **Month-over-month comparison uses a comparable window, never a raw
  full-vs-partial one.** `insights/periods.py` resolves the latest two
  calendar months present in the data and compares them either as full
  months, or — when the current month's last transaction isn't on that
  month's actual last calendar day — both months truncated to the same
  day-of-month cutoff. This generalises the qualitative judgement PR-008's
  EDA made about this project's own fixture (Feb is a full month, Mar
  isn't) into a reusable rule that doesn't hard-code any specific month.
- **Four new insight rules (`insights/rules.py`) are deterministic and
  threshold-based, not ML** — spending trend (±15%), category change
  (±25%, requiring >=2 transactions per comparison window), income/expense
  divergence, and savings-rate change (±10 percentage points). None of
  the thresholds are statistically fitted — like every other threshold in
  this codebase, they are documented, evidence-motivated choices, not
  values tuned against labelled outcomes (none exist for this product).
- **Confidence, severity and ranking are heuristics, not calibrated
  probabilities or a recommendation model.** `insights/periods.py`'s
  `comparison_confidence` blends period comparability with evidence
  volume; anomaly-insight confidence is a fixed score per detection tier;
  recurring-insight confidence is PR-010's own `confidence_score`,
  unchanged. `insights/ranking.py` only orders insights that already
  exist (severity, then confidence, then recency, then id) — it does not
  decide which insights to generate.
- **No LLM anywhere in the Insights Engine.** Every `Insight.description`
  is either a fixed template (`rules.py`) or PR-009/PR-010's own
  deterministic explanation text, passed through unchanged
  (`conversions.py`).
- **The API is transport only; `api/service.py` is the one place that
  talks to both Pydantic and pandas.** `api/routes/analytics.py`'s handler
  is a single line — `return run_analysis(request)` — so no anomaly
  threshold, recurring threshold, feature-engineering step or insight rule
  can end up inside a route (PR-013, Critical Architecture Rule).
  `service.run_analysis` calls `generate_insights` (PR-012),
  `detect_anomalies` (PR-009) and `detect_recurring_transactions` (PR-010)
  unchanged, and reuses two PR-007 building blocks
  (`duckdb_queries.total_income`/`total_expenses`,
  `data.quality.build_quality_report`) for the response's `summary`/
  `metadata` sections instead of writing new aggregation logic.
- **`anomalies`/`recurring` in the response are filtered, not raw.** Only
  `AnomalyResult`s with `is_anomaly=True` and `RecurringResult`s classified
  `"Recurring"`/`"Possible recurring"` are returned — the same filter
  `insights/conversions.py` already applies when turning those results into
  `Insight`s, so the three response sections agree with each other about
  what counts as worth surfacing.
- **Two validation layers, not one.** Pydantic (`api/schemas.py`) rejects
  malformed requests at the transport boundary (422) — missing fields,
  wrong types, unparseable dates. `service.run_analysis` then re-runs
  PR-007's own `validation.transactions.validate_transactions` on the
  constructed DataFrame and maps a failure to 400
  (`service.TransactionValidationError`, mapped in `api/errors.py`). This
  is not redundant: a JSON body can encode `"amount": NaN`, which
  Pydantic's `float` field accepts (JSON's non-standard NaN extension) but
  which `validate_transactions` correctly flags as an invalid amount — see
  `tests/api/test_analytics.py::test_nan_amount_passes_schema_but_fails_structural_validation`.
- **The API is stateless by construction, not by convention.** There is no
  database, ORM, session or cache anywhere in `api/` — every request
  reconstructs its DataFrame from the request body and discards it after
  the response is built. Nothing about a request (not even that it
  happened) is logged; only the *type* of an unexpected exception is
  logged server-side on a 500, via `logging.Logger.exception`, and never
  the request body (`api/errors.py`).

## Out of scope

Not implemented in this workspace (see PR-007 through PR-013 for the full
list): general machine learning, forecasting, clustering, fraud detection,
financial advice, ML/LLM categorisation, fuzzy/embedding-based merchant
matching, recommendations, LLM-generated insights, production dashboard
analytics, and Android/Python runtime integration. Anomaly detection,
recurring-transaction detection, merchant normalisation/categorisation, the
Insights Engine and a FastAPI service exposing all of them over HTTP
(`src/finance_analytics/api/`, PR-013) are all implemented — see
`anomalies/detector.py` / `recurring/detector.py` / `enrichment/models.py` /
`insights/engine.py` / `api/service.py`'s docstrings and notebooks
03 / 04 / 05 / 06's "Out of Scope — Confirmation" sections for what they
deliberately do not do.

The API specifically does **not** implement: authentication, user accounts,
server-side transaction persistence, a database, sessions, cloud
deployment, Docker deployment, Android integration, LLM-generated insights,
recommendations, background jobs, or push notifications (PR-013 §16/Out of
Scope). It is a local-development, stateless HTTP layer only — see
"Running the API locally" above for the privacy caveats that follow from
that.
