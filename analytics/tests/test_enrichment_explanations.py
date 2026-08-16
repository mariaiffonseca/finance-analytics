from finance_analytics.enrichment.explanations import explain


def test_known_merchant_rule_explanation_names_merchant_and_category():
    reason = explain("known_merchant_rule", "Spotify", "Subscriptions")

    assert "Spotify" in reason
    assert "Subscriptions" in reason


def test_known_description_rule_explanation_names_category():
    reason = explain("known_description_rule", None, "Subscriptions")

    assert "Subscriptions" in reason


def test_existing_category_explanation_names_category():
    reason = explain("existing_category", "Totally New Shop", "Shopping")

    assert "Shopping" in reason


def test_fallback_explanation_names_merchant_when_known():
    reason = explain("fallback", "Totally New Shop", "Uncategorised")

    assert "Totally New Shop" in reason
    assert "Uncategorised" in reason


def test_fallback_explanation_handles_missing_merchant():
    reason = explain("fallback", None, "Uncategorised")

    assert "Uncategorised" in reason
    assert "This transaction" in reason
