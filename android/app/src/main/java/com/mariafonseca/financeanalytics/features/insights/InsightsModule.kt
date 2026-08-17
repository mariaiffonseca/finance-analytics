package com.mariafonseca.financeanalytics.features.insights

import com.mariafonseca.financeanalytics.features.insights.presentation.InsightsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val insightsModule = module {
    viewModel { InsightsViewModel(get(), get()) }
}
