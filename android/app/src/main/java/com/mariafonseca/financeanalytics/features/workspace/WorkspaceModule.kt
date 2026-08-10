package com.mariafonseca.financeanalytics.features.workspace

import com.mariafonseca.financeanalytics.features.workspace.presentation.WorkspaceViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val workspaceModule = module {
    viewModel { WorkspaceViewModel() }
}
