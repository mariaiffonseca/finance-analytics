package com.mariafonseca.financeanalytics.features.overview

import com.mariafonseca.financeanalytics.features.overview.presentation.OverviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val overviewModule = module {
    viewModel { OverviewViewModel() }
}
