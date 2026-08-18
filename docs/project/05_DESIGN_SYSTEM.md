# Finance Analytics Design System

| Field | Value |
|---|---|
| Version | 1.0.0 |
| Status | Approved visual reference |
| Source | Claude Design prototype (`Finance Analytics.dc.html`) |
| Target | Android / Jetpack Compose |
| Themes | Light + Dark |

---

# 1. Purpose

This document translates the approved Finance Analytics visual prototype into implementation rules for the Android application.

The prototype uses a restrained editorial/Modernist direction: strong typography, sharp geometry, thin dividers, minimal decoration, and a strong orange accent.

The design should feel like an analytics product rather than a traditional banking or budgeting application.

---

# 2. Design Principles

- Insight first.
- Analytics over CRUD.
- Strong editorial hierarchy.
- Restrained surfaces.
- Sharp geometry.
- Minimal elevation.
- Strong numeric hierarchy.
- Charts should clarify data, not decorate it.
- Use colour deliberately.

The prototype prioritises spending, trends, anomalies, recurring activity and local/offline processing.

---

# 3. Color System

## Light Theme

| Token | Value | Usage |
|---|---|---|
| `background` | `#F3F2F2` | Main background |
| `surface` | `#EAE9E9` | Secondary surfaces |
| `surfaceElevated` | `#FFFFFF` | Elevated surfaces / sheets |
| `text` | `#201E1D` | Primary text |
| `textSecondary` | `rgba(32,30,29,0.62)` | Secondary text |
| `divider` | `rgba(32,30,29,0.14)` | Standard divider |
| `dividerStrong` | `rgba(32,30,29,0.40)` | Strong divider |
| `accent` | `#EC3013` | Primary action / active state |
| `accentDeep` | `#AE1800` | Accent text |
| `accentTint` | `#FFF2EF` | Accent/negative surface |
| `positive` | `#3F7D52` | Positive indicator |
| `positiveDeep` | `#2B5C3B` | Positive text |
| `positiveTint` | `#EAF3EC` | Positive surface |
| `warningDeep` | `#8A5A06` | Warning/anomaly text |
| `warningTint` | `#FBF1DE` | Warning/anomaly surface |
| `errorTint` | `#FFE0D9` | Error surface |

## Dark Theme

| Token | Value | Usage |
|---|---|---|
| `background` | `#131211` | Main background |
| `surface` | `#1E1C1B` | Secondary surfaces |
| `surfaceElevated` | `#252322` | Elevated surfaces / sheets |
| `text` | `#F3F1F0` | Primary text |
| `textSecondary` | `rgba(243,241,240,0.62)` | Secondary text |
| `divider` | `rgba(243,241,240,0.16)` | Standard divider |
| `dividerStrong` | `rgba(243,241,240,0.32)` | Strong divider |
| `accent` | `#FF563C` | Primary action / active state |
| `accentDeep` | `#FFC4B8` | Accent text |
| `accentTint` | `rgba(255,86,60,0.16)` | Accent/negative surface |
| `positive` | `#6FAE82` | Positive indicator |
| `positiveDeep` | `#9CCAA9` | Positive text |
| `positiveTint` | `rgba(111,174,130,0.16)` | Positive surface |
| `warningDeep` | `#E0A53F` | Warning/anomaly text |
| `warningTint` | `rgba(224,165,63,0.16)` | Warning/anomaly surface |
| `errorTint` | `rgba(255,86,60,0.16)` | Error surface |

These values come directly from the prototype's `LIGHT` and `DARK` theme definitions.

---

# 4. Semantic Color Rules

Feature code must not use raw hex values.

Use semantic tokens such as:

```text
color.background
color.surface
color.surfaceElevated

color.text.primary
color.text.secondary

color.border.default
color.border.strong

color.accent
color.accentContent
color.accentSurface

color.status.positive
color.status.positiveContent
color.status.positiveSurface

color.status.warning
color.status.warningSurface

color.status.error
color.status.errorSurface
```

---

# 5. Typography

## Font

The prototype uses **Archivo** throughout the application UI.

Archivo is therefore the visual reference font for the Android implementation.

## Type Scale

| Token | Size | Weight | Usage |
|---|---:|---:|---|
| `displayLarge` | 38sp | 800 | Main financial metric |
| `headlineLarge` | 30sp | 800 | Empty-state headline |
| `headlineMedium` | 20sp | 800 | Major title |
| `headlineSmall` | 19sp | 800 | Screen title |
| `titleLarge` | 18sp | 800 | Important metric |
| `titleMedium` | 16sp | 800 | Import headers |
| `bodyLarge` | 14sp | 400 | Supporting text |
| `bodyMedium` | 13sp | 400-600 | Standard content |
| `bodySmall` | 12sp | 400 | Supporting information |
| `labelLarge` | 14sp | 700 | Primary buttons |
| `labelMedium` | 12sp | 700 | Filters / controls |
| `labelSmall` | 10-11sp | 700 | Section labels / metadata |

Uppercase section labels use increased letter spacing.

Financial values use tabular numerals.

---

# 6. Spacing

Use a 4dp-based spacing scale:

```text
space4   = 4dp
space8   = 8dp
space12  = 12dp
space16  = 16dp
space20  = 20dp
space24  = 24dp
space28  = 28dp
space32  = 32dp
```

Common prototype values:

- 16dp horizontal screen padding.
- 18-24dp section spacing.
- 12dp item spacing.
- 24dp major content gaps.
- 28dp empty/error-state padding.

---

# 7. Shapes

The visual language is predominantly square.

```text
shape.none   = 0dp
shape.small  = 4dp
shape.medium = 8dp
shape.large  = 12dp
shape.full   = 50%
```

Use square shapes by default for:

- Buttons
- Filters
- Search fields
- List rows
- Insight rows
- Import panels

Use circular shapes only for intrinsically circular elements:

- Status indicators
- Progress indicators
- Donut charts
- Success/error icon containers
- Insight dots

Do not turn every section into a rounded card.

---

# 8. Borders and Elevation

Borders are an important part of the visual language.

Use:

- 1dp standard borders/dividers.
- 2dp strong section separators.

Prefer dividers and borders over shadows.

Use elevation primarily for:

- Bottom sheets.
- Modal surfaces.
- Other true overlays.

---

# 9. Buttons

Primary buttons use:

- Accent background.
- White content.
- Square corners.
- 14sp bold label.
- Approximately 15dp vertical padding.
- Approximately 18dp horizontal padding.

They are full-width in the main import flow.

Examples:

```text
Import CSV
Continue
View dashboard
Choose another file
```

---

# 10. Filters

Filters are compact and outlined.

Selected:

- Accent background.
- White text.
- Accent border.

Unselected:

- Transparent background.
- Primary text.
- Standard border.

Used for:

- Insight filters.
- Transaction date filters.
- Transaction category filters.

Filters should scroll horizontally on small screens.

---

# 11. Lists

Lists use an editorial row structure instead of cards.

Typical structure:

```text
Primary content                  Amount
Secondary metadata
───────────────────────────────────────
```

Use:

- 10-14dp vertical padding.
- Thin dividers.
- Strong primary text.
- Secondary metadata in `textSecondary`.
- Right-aligned tabular numeric values.

---

# 12. Metric Blocks

Primary metrics should not automatically be placed inside cards.

Example:

```text
TOTAL SPENT
€1,234.56
+12% vs Jun
```

Hierarchy:

1. Small uppercase label.
2. Large heavy number.
3. Secondary comparison.

The main spending value uses approximately 38sp / 800 weight.

---

# 13. Insights

Insights are a primary product component.

Supported types:

```text
neutral
positive
negative
anomaly
informational
recommendation
```

An insight may contain:

- Type indicator.
- Headline.
- Metadata.
- Magnitude.
- Optional chart.
- Optional recommendation.

Semantic treatment:

| Type | Primary visual tokens |
|---|---|
| Positive | `positive`, `positiveDeep`, `positiveTint` |
| Negative | `accent`, `accentDeep`, `accentTint` |
| Anomaly | `warningDeep`, `warningTint` |
| Informational | Neutral text/surface |
| Recommendation | Neutral text/surface |
| Neutral | Neutral text/surface |

Recommendations should feel optional, not alarming.

---

# 14. Charts

Charts are deliberately minimal.

## Line charts

Used for:

- Spending trends.
- Category trends.
- Insight detail.

Rules:

- Accent line.
- Optional accent-tinted area.
- Small data points.
- Minimal decoration.
- Lightweight labels.

The prototype uses compact charts around 70-90px high.

## Donut charts

Used for category spending breakdown.

Prototype reference:

- 112dp outer diameter.
- 16dp inner inset.
- Background-coloured centre.
- Largest category uses the accent colour.
- Remaining categories use a neutral ramp.

Avoid many saturated colours.

---

# 15. Navigation

Primary destinations:

```text
Overview
Insights
Transactions
```

Active destination uses the accent colour.

Inactive destinations use secondary text.

Do not add navigation destinations without a product requirement.

---

# 16. Overview

Visual hierarchy:

1. Screen title.
2. Selected month.
3. Total spent.
4. Income / saved metrics.
5. Top insight.
6. Spending trend.
7. Category breakdown.
8. More insights.

The overview prioritises understanding over transaction browsing.

---

# 17. Insights Screen

The screen contains:

- Sticky header.
- Horizontal filter controls.
- Editorial insight list.
- Optional charts.
- Magnitude values.

Filters:

```text
Recent
Spending
Trends
Anomalies
Recurring
```

---

# 18. Transactions

Hierarchy:

1. Title.
2. Search.
3. Filters.
4. Date groups.
5. Transactions.

Transactions are rows, not cards.

Selecting a transaction opens a bottom sheet.

---

# 19. Transaction Detail

Use a bottom sheet with:

```text
Merchant
Amount
────────────────
Date
Category
Recurring status
```

The amount is visually dominant.

Use `surfaceElevated` for the sheet.

"Recurring status" is deferred: the local `Transaction` domain model has no
link back to an analytics `RecurringTransaction` result (that result is keyed
by merchant/currency, not by transaction id), so showing it would mean
inventing data rather than reflecting what the domain supports (PR-015 §6).
Add the row once that link exists.

---

# 20. Category Analytics

Category views contain:

- Category name.
- Current spending.
- Change vs previous period.
- Change vs average.
- Trend chart.
- Merchant breakdown.

Use the same typography and chart language as Overview.

---

# 21. Import Flow

States:

```text
Select file
Preview
Validating
Progress
Complete
Error
```

The prototype communicates:

- CSV source.
- File validation.
- Duplicate detection.
- Merchant categorisation.
- Insight generation.
- Local processing.

Keep progress understandable and restrained.

---

# 22. Empty State

The empty state is editorial and value-led:

```text
FINANCE ANALYTICS

Understand where your money goes.

Supporting explanation

Privacy / offline principles

Import CSV
```

---

# 23. Loading and Progress

Validation uses:

- Accent circular spinner.
- Bold title.
- Sequential validation steps.

Completed steps use the positive colour.

Pending steps remain neutral.

Import progress uses a thin accent progress bar.

---

# 24. Error State

Errors should be specific and actionable.

Use:

- Error-tinted surface.
- Clear title.
- Specific validation messages.
- Short explanation.
- Primary recovery action.

Avoid generic error messaging when the cause is known.

---

# 25. Success State

Successful import uses:

- Circular positive-tinted icon container.
- Positive check icon.
- Strong completion title.
- Summary metrics.
- Privacy reassurance.
- Primary next action.

---

# 26. Settings

Current visual scope:

- Appearance.
- Currency.
- Local-data/privacy message.
- Reset demo data.

Keep Settings intentionally small.

---

# 27. Accessibility

The Android implementation must:

- Maintain sufficient contrast.
- Never rely on colour alone.
- Provide content descriptions for meaningful icons.
- Provide accessible alternatives for charts.
- Preserve appropriate touch targets.
- Support font scaling.
- Keep both themes readable.

---

# 28. Compose Mapping

Recommended design-system structure:

```text
designsystem/
├── color/
├── typography/
├── shape/
├── spacing/
├── component/
└── chart/
```

Features must consume these tokens/components rather than defining their own visual language.

---

# 29. Rules for Future LLM Implementations

1. Do not invent new colours.
2. Do not use raw hex values in feature code.
3. Prefer existing typography tokens.
4. Prefer existing spacing tokens.
5. Prefer dividers over unnecessary cards or shadows.
6. Keep corners predominantly square.
7. Use the accent colour deliberately.
8. Keep charts restrained.
9. Preserve numeric hierarchy.
10. Do not redesign screens while implementing functionality.
11. Reuse existing design-system components.
12. If the specification and prototype disagree, stop and report the discrepancy.

---

# 30. Source of Truth

The Claude Design prototype is the visual reference.

This document converts its visual decisions into implementation guidance.

If the document and prototype disagree, do not silently choose one. Resolve the discrepancy before implementing the affected UI.

---

# Related Documents

- `00_ENGINEERING_STACK.md`
- `01_ANDROID_BLUEPRINT.md`
- `02_ANDROID_ARCHITECTURE.md`
- `03_ANDROID_FEATURE_TEMPLATE.md` (not yet created — planned for a future PR)
- `04_FEATURE_REVIEW_CHECKLIST.md` (not yet created — planned for a future PR)
- `00_PROJECT_CHARTER.md`

---

# Changelog

## 1.0.0

Initial design system extracted from the approved Finance Analytics Claude Design prototype.
