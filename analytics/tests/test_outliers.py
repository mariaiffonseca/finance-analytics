import pandas as pd

from finance_analytics.analysis.outliers import (
    flag_category_relative_outliers,
    flag_iqr_outliers,
    iqr_bounds,
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
