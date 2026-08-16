from finance_analytics.enrichment.categories import (
    FALLBACK_CATEGORY,
    KNOWN_CATEGORIES,
    categorise,
)


def test_known_merchant_rule_is_used_when_available():
    result = categorise("Spotify", raw_category=None, description=None)

    assert result.category == "Subscriptions"
    assert result.method == "known_merchant_rule"
    assert result.confidence == 1.0


def test_known_merchant_rule_wins_over_a_conflicting_raw_category():
    # Netflix is unambiguously "Subscriptions" in the merchant rule table;
    # a one-off, conflicting raw category on this row must not override it.
    result = categorise("Netflix", raw_category="Entertainment", description=None)

    assert result.category == "Subscriptions"
    assert result.method == "known_merchant_rule"


def test_merchant_rule_lookup_is_case_insensitive():
    result = categorise("SPOTIFY", raw_category=None, description=None)

    assert result.category == "Subscriptions"
    assert result.method == "known_merchant_rule"


def test_known_description_rule_used_when_merchant_is_unknown():
    result = categorise(None, raw_category=None, description="Monthly subscription renewal")

    assert result.category == "Subscriptions"
    assert result.method == "known_description_rule"
    assert result.confidence == 0.75


def test_description_rule_wins_over_a_conflicting_existing_category():
    result = categorise(None, raw_category="Shopping", description="Monthly subscription")

    assert result.category == "Subscriptions"
    assert result.method == "known_description_rule"


def test_existing_trusted_category_used_when_no_rule_matches():
    result = categorise("Totally New Shop", raw_category="Shopping", description="A new gadget")

    assert result.category == "Shopping"
    assert result.method == "existing_category"
    assert result.confidence == 0.5


def test_missing_merchant_still_categorised_via_existing_trusted_category():
    # Mirrors transaction_id=21 in the real fixture: merchant missing, but
    # the raw category is present and trustworthy — it must not be
    # discarded just because the merchant couldn't be resolved.
    result = categorise(None, raw_category="Shopping", description="Missing merchant test")

    assert result.category == "Shopping"
    assert result.method == "existing_category"


def test_unrecognised_raw_category_is_not_trusted():
    result = categorise("Totally New Shop", raw_category="Misc", description=None)

    assert result.category == FALLBACK_CATEGORY
    assert result.method == "fallback"
    assert result.confidence == 0.0


def test_blank_raw_category_is_not_trusted():
    result = categorise("Totally New Shop", raw_category="   ", description=None)

    assert result.category == FALLBACK_CATEGORY
    assert result.method == "fallback"


def test_fallback_when_nothing_matches():
    result = categorise(None, raw_category=None, description=None)

    assert result.category == FALLBACK_CATEGORY
    assert result.method == "fallback"
    assert result.confidence == 0.0


def test_transaction_is_never_forced_into_a_real_category_without_evidence():
    result = categorise(None, raw_category=float("nan"), description="")

    assert result.category not in KNOWN_CATEGORIES
    assert result.category == FALLBACK_CATEGORY


def test_synthetic_merchant_rules_are_used_instead_of_the_module_defaults():
    rules = {"new shop": "Shopping"}

    result = categorise("New Shop", raw_category=None, description=None, merchant_rules=rules)

    assert result.category == "Shopping"
    assert result.method == "known_merchant_rule"


def test_synthetic_description_rules_are_used_instead_of_the_module_defaults():
    rules = {"withdrawal": "Cash"}

    result = categorise(
        None, raw_category=None, description="ATM withdrawal", description_rules=rules
    )

    assert result.category == "Cash"
    assert result.method == "known_description_rule"
