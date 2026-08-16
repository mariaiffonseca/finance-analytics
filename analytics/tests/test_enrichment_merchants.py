import math

import pytest

from finance_analytics.enrichment.merchants import MERCHANT_ALIASES, normalise_merchant


@pytest.mark.parametrize("raw", [None, float("nan"), "", "   "])
def test_missing_or_blank_merchant_returns_none(raw):
    assert normalise_merchant(raw) is None


def test_trims_and_collapses_internal_whitespace():
    assert normalise_merchant("  Coffee   Corner  ") == "Coffee Corner"


def test_already_normalised_merchant_is_unchanged():
    assert normalise_merchant("Netflix") == "Netflix"


def test_preserves_original_casing_when_no_alias_matches():
    # "EDP" is a real acronym in the dataset — title-casing would corrupt it
    # to "Edp". No alias is registered for it, so it must pass through as-is.
    assert normalise_merchant("EDP") == "EDP"
    assert normalise_merchant("TAP Air") == "TAP Air"


def test_unknown_merchant_is_returned_cleaned_but_unmerged():
    assert normalise_merchant("Totally New Shop") == "Totally New Shop"


def test_default_alias_table_is_empty():
    # See merchants.py's module docstring: no alias has evidence yet.
    assert MERCHANT_ALIASES == {}


@pytest.mark.parametrize("raw", ["NETFLIX", "netflix", "netflix.com", "  Netflix  "])
def test_known_alias_folds_casing_and_punctuation_variants(raw):
    aliases = {"netflix": "Netflix", "netflix.com": "Netflix"}

    assert normalise_merchant(raw, aliases=aliases) == "Netflix"


def test_alias_lookup_does_not_merge_an_unrelated_merchant():
    aliases = {"netflix": "Netflix"}

    # A different merchant must never be pulled into an alias it wasn't
    # explicitly mapped to — no fuzzy matching.
    assert normalise_merchant("Netflix Extra Member", aliases=aliases) == "Netflix Extra Member"


def test_nan_float_is_treated_as_missing():
    assert normalise_merchant(math.nan) is None
