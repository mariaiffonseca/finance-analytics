import pandas as pd

from finance_analytics.enrichment.models import (
    EnrichedTransaction,
    enrich_transaction,
    enrich_transactions,
)


def _row(id_, merchant, category, description, date=None, amount=None):
    return {
        "id": id_,
        "merchant": merchant,
        "category": category,
        "description": description,
        "date": date,
        "amount": amount,
    }


def _result_for(results: list[EnrichedTransaction], transaction_id: str) -> EnrichedTransaction:
    return next(r for r in results if r.transaction_id == transaction_id)


def test_raw_merchant_value_is_preserved_exactly():
    result = enrich_transaction("1", "  Netflix  ", "Subscriptions", "Monthly subscription")

    assert result.raw_merchant == "  Netflix  "
    assert result.normalised_merchant == "Netflix"


def test_raw_category_and_description_are_preserved_exactly():
    result = enrich_transaction("1", "Netflix", "  Subscriptions  ", "  Monthly subscription  ")

    assert result.raw_category == "  Subscriptions  "
    assert result.raw_description == "  Monthly subscription  "


def test_missing_raw_values_become_none_not_a_placeholder_string():
    result = enrich_transaction("1", None, None, None)

    assert result.raw_merchant is None
    assert result.raw_category is None
    assert result.raw_description is None


def test_missing_merchant_is_safely_identifiable_and_still_categorised():
    result = enrich_transaction("21", None, "Shopping", "Missing merchant test")

    assert result.normalised_merchant is None
    assert result.category == "Shopping"
    assert result.categorisation_method == "existing_category"


def test_enrichment_is_deterministic():
    first = enrich_transaction("1", "Spotify", "Subscriptions", "Monthly subscription")
    second = enrich_transaction("1", "Spotify", "Subscriptions", "Monthly subscription")

    assert first == second


def test_enrich_transactions_deduplicates_by_id():
    frame = pd.DataFrame(
        [
            _row("14", "Spotify", "Subscriptions", "Monthly subscription"),
            _row("14", "Spotify", "Subscriptions", "Monthly subscription"),
        ]
    )

    results = enrich_transactions(frame)

    assert len(results) == 1


def test_enrich_transactions_does_not_require_valid_date_or_amount():
    frame = pd.DataFrame(
        [
            _row(
                "19",
                "Test Merchant",
                "Shopping",
                "Invalid date test",
                date=pd.NaT,
                amount=-20.00,
            ),
            _row(
                "20",
                "Test Merchant",
                "Shopping",
                "Invalid amount test",
                date=pd.Timestamp("2026-03-18"),
                amount=float("nan"),
            ),
        ]
    )

    results = enrich_transactions(frame)

    assert len(results) == 2
    for result in results:
        assert result.normalised_merchant == "Test Merchant"
        assert result.category == "Shopping"


def test_enrich_transactions_handles_a_missing_optional_column():
    # No "description" column at all — should be treated as entirely missing,
    # not raise.
    frame = pd.DataFrame([{"id": "1", "merchant": "Continente", "category": "Groceries"}])

    results = enrich_transactions(frame)

    assert results[0].raw_description is None
    assert results[0].category == "Groceries"


def test_unknown_merchant_and_category_remain_uncategorised_not_forced():
    result = _result_for(
        enrich_transactions(
            pd.DataFrame([_row("99", "Totally New Shop", "Nonexistent Category", None)])
        ),
        "99",
    )

    assert result.category == "Uncategorised"
    assert result.categorisation_method == "fallback"
    assert result.reason
