package com.mariafonseca.financeanalytics

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { PlaceholderViewModel() }
}
