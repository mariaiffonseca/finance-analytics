package com.mariafonseca.financeanalytics.features.`import`

import com.mariafonseca.financeanalytics.features.`import`.data.ContentResolverCsvFileSource
import com.mariafonseca.financeanalytics.features.`import`.data.CsvFileSource
import com.mariafonseca.financeanalytics.features.`import`.data.CsvImportPipeline
import com.mariafonseca.financeanalytics.features.`import`.presentation.ImportViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val importModule = module {
    single<CsvFileSource> { ContentResolverCsvFileSource(androidContext().contentResolver) }
    single { CsvImportPipeline(get()) }
    viewModel { ImportViewModel(get(), get()) }
}
