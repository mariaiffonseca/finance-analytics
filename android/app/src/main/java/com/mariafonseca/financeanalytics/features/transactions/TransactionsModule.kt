package com.mariafonseca.financeanalytics.features.transactions

import com.mariafonseca.financeanalytics.features.transactions.presentation.TransactionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val transactionsModule = module {
    viewModel { TransactionsViewModel() }
}
