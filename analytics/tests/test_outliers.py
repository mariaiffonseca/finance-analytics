import math

import pandas as pd
import pytest

from finance_analytics.analysis.outliers import (
    flag_category_relative_outliers,
    flag_iqr_outliers,
    iqr_bounds,
    robust_zscore_of,
    robust_zscores,
)


def test_iqr_bounds_widen_with_k():
    values = pd.Series([1, 2, 3, 4, 5, 6, 7, 8, 9, 100])

    narrow_lower, narrow_upper = iqr_bounds(values, k=1.5)
    wide_lower, wide_upper = iqr_bounds(values, k=3.0)

    assert wide_lower <= narrow_lower
    assert wide_upper >= narrow_upper


def test_flag_iqr_outliers_flags_the_extreme_value():
    values = pd.Series([10, 11, 12, 9, 10, 11, 500])

    flags = flag_iqr_outliers(values)

    assert flags.iloc[-1]
    assert not flags.iloc[:-1].any()


def test_robust_zscores_handles_zero_mad():
    values = pd.Series([10.0, 10.0, 10.0, 10.0])

    scores = robust_zscores(values)

    assert (scores == 0.0).all()


def test_robust_zscores_flags_the_far_value():
    values = pd.Series([10.0, 11.0, 9.0, 10.0, 200.0])

    scores = robust_zscores(values)

    assert scores.iloc[-1] > 3


def test_flag_category_relative_outliers_is_relative_to_category():
    frame = pd.DataFrame(
        {
            # 50 is unremarkable for Travel but an outlier within Groceries.
            "amount": [-10, -11, -9, -10, -50, -600, -650, -580],
            "category": ["Groceries"] * 5 + ["Travel"] * 3,
        }
    )

    flags = flag_category_relative_outliers(frame)

    assert flags.iloc[4]
    assert not flags.iloc[5:].any()


def test_robust_zscore_of_scores_against_the_given_reference_only():
    # median=10, mad=1 -> mad scaled by the same 1.4826 constant robust_zscores uses.
    score = robust_zscore_of(15.0, median=10.0, mad=1.0)

    assert score == pytest.approx((15.0 - 10.0) / (1.0 * 1.4826))


def test_robust_zscore_of_zero_mad_matching_value_is_not_anomalous():
    assert robust_zscore_of(10.0, median=10.0, mad=0.0) == 0.0


def test_robust_zscore_of_zero_mad_differing_value_is_infinite():
    assert robust_zscore_of(15.0, median=10.0, mad=0.0) == math.inf
    assert robust_zscore_of(5.0, median=10.0, mad=0.0) == -math.inf


def test_robust_zscore_of_missing_reference_is_nan():
    assert math.isnan(robust_zscore_of(10.0, median=float("nan"), mad=1.0))
