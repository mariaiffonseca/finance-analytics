"""Expected structure of a transaction dataset.

The column set mirrors the transaction concepts referenced by
`docs/execution/03_ANALYTICS_FOUNDATION/PR-007_PYTHON_ANALYTICS_WORKSPACE.md`.
`docs/project/02_DOMAIN_MODEL.md` does not exist yet in this repository, so this
module — not a notebook — is the source of truth for the analytics-side schema.
"""

from __future__ import annotations

#: Every column a transaction dataset must contain.
REQUIRED_COLUMNS: tuple[str, ...] = (
    "id",
    "date",
    "amount",
    "currency",
    "description",
    "merchant",
    "category",
    "account",
)

#: Columns whose value must be present (non-null, non-blank) on every row.
#:
#: `description`, `category` and `account` may legitimately be blank in a bank
#: export (e.g. an uncategorised or single-account export), so they are part of
#: the schema but are not required to have a value.
REQUIRED_VALUE_COLUMNS: tuple[str, ...] = (
    "id",
    "date",
    "amount",
    "currency",
    "merchant",
)
