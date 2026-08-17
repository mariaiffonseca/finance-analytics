# PR-014 — Android Analytics API Integration

| Field | Value |
|---|---|
| Sprint | 07 - Android Integration |
| PR | PR-014 |
| Status | Ready |
| Goal | Connect the Android application to the Analytics API |
| Depends On | PR-013 — Analytics API with FastAPI |

## Objective

Integrate the existing Android application with the Python Analytics API.

```text
Android
   │ HTTP / JSON
   ▼
FastAPI
   ▼
Analytics Engine
   ▼
Structured Results
   ▼
Android
```

The Android application must not know anything about Pandas, Scikit-learn, Python, or analytics implementation details. It consumes the validated API contract.

## Required Context

Read:

1. `docs/foundation/00_ENGINEERING_STACK.md`
2. `docs/foundation/01_ANDROID_BLUEPRINT.md`
3. `docs/foundation/02_ANDROID_ARCHITECTURE.md`
4. `docs/foundation/07_REPOSITORY_CONVENTIONS.md`
5. `docs/project/00_PROJECT_CHARTER.md`
6. `docs/project/01_PRODUCT_PRINCIPLES.md`
7. `docs/project/02_DOMAIN_MODEL.md`
8. `docs/project/03_PRODUCT_REQUIREMENTS.md`
9. `docs/project/05_DESIGN_SYSTEM.md`
10. `docs/execution/06_ANALYTICS_SERVICE/PR-013_ANALYTICS_API_FASTAPI.md`

Also inspect the existing Android codebase and the actual FastAPI OpenAPI contract created in PR-013.

## Critical Rule

**The OpenAPI/API contract is the source of truth.**

Do not invent request or response fields.

Do not duplicate Python analytical logic in Kotlin.

If the actual PR-013 contract differs from assumptions here, follow the validated contract.

## 1. Networking

Use the existing Android networking conventions.

If no networking stack is established, use:

```text
Ktor Client
Kotlin Serialization
```

Do not introduce Retrofit if the project has standardised on another stack.

## 2. Architecture

Keep:

```text
UI
 ↓
ViewModel
 ↓
Repository
 ↓
API Client
 ↓
FastAPI
```

Composables must not call HTTP directly. ViewModels must not construct HTTP requests.

## 3. API Client

Create an API client responsible only for:

- serialising requests;
- executing HTTP calls;
- deserialising responses;
- mapping HTTP/network failures.

Do not put business logic here.

## 4. DTOs

Create DTOs matching the actual API contract, for example:

```text
AnalyticsRequestDto
AnalyticsResponseDto
InsightDto
AnomalyDto
RecurringDto
```

Keep API DTOs separate from domain models.

## 5. Mapping

Use explicit:

```text
DTO
 ↓
Domain Model
 ↓
UI Model
```

Mappings must be deterministic and testable.

## 6. Repository

Create/extend an analytics repository following existing conventions.

Conceptually:

```kotlin
interface AnalyticsRepository {
    suspend fun analyse(
        transactions: List<Transaction>
    ): Result<AnalyticsResult>
}
```

The repository prepares requests, calls the API, maps results and translates expected failures.

## 7. Error Handling

Handle:

```text
No internet connection
Timeout
HTTP 4xx
HTTP 5xx
Invalid API response
Unexpected exception
```

Do not expose raw technical errors to users.

Follow existing application error conventions.

## 8. Local-First Behaviour

The app must remain usable when the Analytics API is unavailable.

Analytics connectivity must not prevent:

- viewing transactions;
- navigating the app;
- accessing locally available transaction data.

For this PR:

```text
API available
    ↓
request analytics
    ↓
show results

API unavailable
    ↓
keep local app functional
    ↓
show unavailable state
```

Do not implement an offline analytics queue yet.

## 9. Analytics Trigger

Add an explicit application-level trigger for analytics.

Do not trigger API calls on every recomposition or every database emission.

Follow the existing UI/product flow.

## 10. Analytics State

Expose clear ViewModel states, using existing project conventions:

```text
Idle
Loading
Success
Unavailable
Error
```

The UI must distinguish loading, success, unavailable and unexpected failure.

## 11. Analytics UI

Integrate API results into the existing analytics experience and design system.

At minimum support, when returned by the API:

- analytics summary;
- insights;
- anomalies;
- recurring transactions.

Do not expose raw JSON or technical metadata.

Do not redesign the application in this PR.

## 12. Design System

Follow:

```text
docs/project/05_DESIGN_SYSTEM.md
```

Do not introduce new colours, typography or component styles unless genuinely required.

## 13. Loading and Empty States

Provide a local analytics loading state.

Handle:

```text
No analytics available
```

without treating it as an application failure.

Examples:

- insufficient history;
- no detected anomalies;
- no recurring transactions;
- no generated insights.

## 14. API Configuration

Do not hard-code production URLs throughout the code.

Use the project's build/configuration conventions.

Support at minimum a local development endpoint.

Do not commit secrets.

## 15. Testing

Add tests for:

### API client
- successful response;
- HTTP errors;
- malformed response;
- network exception.

### Mapping
- DTO → domain;
- missing/optional fields;
- empty lists.

### Repository
- successful analysis;
- API failure;
- network failure.

### ViewModel
- loading;
- success;
- unavailable;
- error.

### UI
Add focused Compose tests for important analytics states where consistent with the existing strategy.

## 16. Integration Test

Create at least one integration-level test:

```text
Android request
    ↓
API layer
    ↓
Response
    ↓
Domain mapping
```

Use a deterministic test server/mock server, not a live production deployment.

## 17. Security / Privacy

Do not:

- log transaction payloads;
- log full financial analytics responses;
- commit API credentials;
- persist raw API responses unnecessarily.

Use HTTPS for non-local environments.

Document production transport-security/authentication requirements.

Do not implement authentication in this PR unless already required by PR-013.

## Out of Scope

Do NOT implement:

- API authentication
- User accounts
- Server-side persistence
- Offline analytics queue
- Push notifications
- LLM-generated explanations
- New analytics algorithms
- Changes to anomaly detection
- Changes to recurring detection
- Changes to the Insights Engine
- Cloud deployment

## Acceptance Criteria

### Integration
- Android communicates with `/analytics/analyse`.
- Request matches the validated API contract.
- Response maps into Android domain models.
- No Python/ML details leak into Android.

### Architecture
- UI → ViewModel → Repository → API Client.
- Composables do not perform HTTP calls.
- API DTOs remain separate from domain models.

### UX
- Loading state exists.
- Success state displays analytics.
- API unavailability does not break local transaction functionality.
- Empty analytics states are handled appropriately.
- Existing design system is respected.

### Testing
- API client tests pass.
- Mapping tests pass.
- Repository tests pass.
- ViewModel tests pass.
- Relevant Compose tests pass.
- Integration-level test passes.

### Privacy
- Financial payloads are not logged.
- No secrets are committed.

## Pull Request

### Title

```text
feat(android): integrate analytics api
```

### Description

```markdown
## Summary

Connected the Android application to the Python Analytics API.

## Architecture

Android UI
→ ViewModel
→ Repository
→ API Client
→ FastAPI

## API Contract

Reference the PR-013 OpenAPI contract.

## UI

Describe the analytics states and screens/components updated.

## Error Handling

Describe network, HTTP and unavailable states.

## Testing

List commands executed and results.

## Privacy

Confirm that transaction payloads and analytics responses are not logged.

## Out of Scope

Confirm that analytics algorithms, authentication, persistence and deployment were not changed.

## Follow-up

Next PR should focus on final analytics UX/polish and end-to-end product validation.
```

## Engineering Reflection

Before opening the Pull Request, answer:

1. Is the API contract the single source of truth?
2. Are DTOs separated from domain models?
3. Is HTTP logic isolated from UI?
4. Does the Repository expose domain-friendly operations?
5. Does the app remain useful when the API is unavailable?
6. Are analytics requests explicitly triggered?
7. Are financial payloads protected from logs?
8. Does the UI follow the design system?
9. Could the API implementation be replaced without rewriting the UI?

## Stop Condition

After integration, tests, integration test, UI-state verification, configuration documentation and PR preparation are complete:

**STOP.**

Do not implement authentication, deployment, offline analytics queues or new analytical algorithms.

Wait for human review.
