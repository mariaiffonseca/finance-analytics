# PR-013 — Analytics API with FastAPI

| Field | Value |
|---|---|
| Sprint | 06 - Analytics Service |
| PR | PR-013 |
| Status | Ready |
| Goal | Expose the validated analytics and insights engine through a clean Python API |
| Depends On | PR-012 — Financial Behaviour & Insights Engine |

## Objective

Create the first backend/API layer for Finance Analytics.

Architecture:

```text
Android App
     │ HTTP
     ▼
  FastAPI
     │
     ▼
Analytics Service
     │
     ├── Enrichment
     ├── Anomaly Detection
     ├── Recurring Detection
     ├── Behaviour Analysis
     └── Insights Engine
```

The API is an orchestration and transport layer. It must not become the place where Data Science logic is implemented.

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
10. `docs/execution/04_ANALYTICAL_FEATURES/PR-009_TRANSACTION_ANOMALY_DETECTION.md`
11. `docs/execution/04_ANALYTICAL_FEATURES/PR-010_RECURRING_TRANSACTION_DETECTION.md`
12. `docs/execution/04_ANALYTICAL_FEATURES/PR-011_MERCHANT_NORMALISATION_CATEGORISATION.md`
13. `docs/execution/05_INSIGHTS_ENGINE/PR-012_FINANCIAL_BEHAVIOUR_INSIGHTS_ENGINE.md`

Also inspect the completed Python implementation before designing endpoints.

## Critical Architecture Rule

Keep this separation:

```text
API Layer
    ↓
Application / Service Layer
    ↓
Analytics Engine
    ↓
Domain / Data
```

Do not put Pandas logic or ML algorithms inside FastAPI endpoints.

## 1. Technology

Use:

```text
FastAPI
Pydantic
Uvicorn
```

Use the existing Python dependency management. Do not add unnecessary backend frameworks.

## 2. API Structure

Create a dedicated API layer within the analytics workspace.

A reasonable structure:

```text
analytics/
├── src/
│   └── finance_analytics/
│       ├── api/
│       │   ├── __init__.py
│       │   ├── app.py
│       │   ├── routes/
│       │   │   ├── health.py
│       │   │   └── analytics.py
│       │   └── schemas.py
│       └── ...
└── tests/
    └── api/
        ├── test_health.py
        └── test_analytics.py
```

Adapt this to the existing codebase and avoid duplicate domain models.

## 3. Health Endpoint

Create:

```http
GET /health
```

Response:

```json
{
  "status": "ok"
}
```

## 4. Analytics Endpoint

Create:

```http
POST /analytics/analyse
```

The endpoint accepts a clearly defined transaction dataset and orchestrates the existing analytics services.

Do not expose Pandas DataFrames or internal Python objects.

## 5. Request Schema

Define explicit Pydantic schemas.

Conceptually:

```text
AnalysisRequest
    └── transactions[]
```

Transactions should correspond to the project domain model and expose only fields required by the analytics engine.

## 6. Response Schema

Return structured analytical results:

```text
AnalysisResponse
├── summary
├── insights[]
├── anomalies[]
├── recurring[]
└── metadata
```

Reuse structured models from previous PRs where appropriate.

## 7. API Contract

Document:

- Required and optional fields
- Validation rules
- Response structure
- Error responses

At minimum support:

```text
400 — Invalid request
422 — Validation error
500 — Internal analytics failure
```

Do not expose Python stack traces.

## 8. Analytics Orchestration

Create an application/service layer:

```text
transactions
    ↓
enrichment
    ↓
analytics
    ↓
insights
    ↓
response
```

Reuse existing implementations.

Do not rewrite anomaly detection, recurring detection, categorisation, or insight rules.

## 9. Statelessness

The initial API must be stateless.

Do not introduce:

- user accounts;
- authentication;
- server-side transaction persistence;
- databases;
- sessions.

The client sends the data required for analysis.

## 10. Error Handling

Handle:

- Invalid transaction payloads
- Empty datasets
- Invalid dates
- Invalid amounts
- Analytics failures

Return structured errors without implementation details.

## 11. API Documentation

Verify:

```text
/docs
/openapi.json
```

Endpoint schemas and descriptions must be understandable without reading the implementation.

## 12. Testing

Use FastAPI's test client.

Test:

### Health
- `GET /health` returns 200.
- Response contains `status = ok`.

### Validation
- Valid request accepted.
- Missing required fields rejected.
- Invalid dates rejected.
- Invalid amounts rejected.
- Empty transaction handling behaves as documented.

### Analytics
- Valid dataset produces a structured response.
- Insights are returned when supported.
- Anomalies are returned when detected.
- Recurring results are returned when detected.

### Errors
- Analytics failures produce controlled responses.
- Internal exceptions are not leaked.

## 13. Contract Test

Create at least one end-to-end API test:

```text
HTTP request
   ↓
FastAPI
   ↓
Analytics service
   ↓
Insights Engine
   ↓
Structured response
```

Use a small deterministic fixture. Do not rely exclusively on mocked analytics components.

## 14. Local Development

Document how to run the API locally.

For example:

```bash
cd analytics
pip install -e ".[dev]"
uvicorn finance_analytics.api.app:app --reload
```

Use the actual project command if different.

Document the local `/docs` URL and do not commit secrets.

## 15. API / Analytics Boundary

The API must not contain:

- Pandas transformations;
- feature engineering;
- ML algorithms;
- anomaly thresholds;
- recurring thresholds;
- insight rules.

Those remain in the analytics layer.

## 16. Security and Privacy

For this local development version:

- Do not use real financial data.
- Do not log transaction payloads.
- Do not log sensitive request bodies.
- Do not commit credentials.
- Do not add external telemetry.
- Document that production deployment would require authentication, transport security and additional privacy controls.

Do not implement production authentication in this PR.

## Out of Scope

Do NOT implement:

- Authentication
- User accounts
- Database persistence
- Cloud deployment
- Docker deployment unless already required
- Android integration
- LLM insights
- Recommendations
- Background jobs
- Push notifications

## Acceptance Criteria

### API
- FastAPI application exists.
- `/health` works.
- `/analytics/analyse` works.
- OpenAPI documentation is generated.
- Request/response schemas are explicit.

### Architecture
- API does not contain analytics logic.
- Existing analytics implementations are reused.
- Service layer separates orchestration from transport.

### Privacy
- Request payloads are not logged.
- No private financial data is committed.

### Testing
- API tests pass.
- At least one real end-to-end analytics request is tested.
- Existing analytics tests remain green.

### Documentation
- Local API setup is documented.
- API contract is documented through schemas/OpenAPI.

## Pull Request

### Title

```text
feat(api): expose analytics through fastapi
```

### Description

```markdown
## Summary

Added a FastAPI service exposing the existing analytics and insights engine through a typed HTTP API.

## Endpoints

- GET /health
- POST /analytics/analyse

## Architecture

Describe the API → service → analytics separation.

## Request / Response

Describe the main API schemas.

## Testing

List commands executed and results.

## Privacy

Describe the local/stateless approach and confirm that request payloads are not logged.

## Documentation

State whether documentation was updated.

## Out of Scope

Confirm that authentication, persistence, deployment and Android integration were not implemented.

## Follow-up

Next PR should integrate the Android application with the analytics API.
```

## Engineering Reflection

Before opening the Pull Request, answer:

1. Is the API only an orchestration/transport layer?
2. Is all analytical logic still in the analytics modules?
3. Are request/response schemas explicit?
4. Is the API stateless?
5. Are financial payloads protected from logging?
6. Is the API contract understandable through OpenAPI?
7. Does the end-to-end API test exercise real analytics?
8. Could Android consume this API without knowing anything about Pandas or Scikit-learn?
9. Is the boundary ready for future authentication and deployment?

## Stop Condition

After implementation, tests, local API verification, OpenAPI verification, documentation and PR preparation are complete:

**STOP.**

Do not implement Android integration, authentication, persistence, deployment or LLM features.

Wait for human review.
