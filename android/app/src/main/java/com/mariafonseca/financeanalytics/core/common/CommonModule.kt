package com.mariafonseca.financeanalytics.core.common

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    viewModel { AppDataViewModel() }
}
