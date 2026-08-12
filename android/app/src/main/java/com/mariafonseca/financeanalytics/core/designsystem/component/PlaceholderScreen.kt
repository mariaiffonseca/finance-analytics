package com.mariafonseca.financeanalytics.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.core.designsystem.Space12
import com.mariafonseca.financeanalytics.core.designsystem.Space24

/**
 * Centered title + supporting-message placeholder, matching the empty-state
 * convention used by EmptyScreen/ImportScreen (Box centered on the full
 * available size) rather than a top-aligned layout. No own Scaffold: this is
 * nested inside AppShellScreen's Scaffold, which already applies the window
 * insets to its tab NavHost.
 */
@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Space24),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(Space12))
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
        }
    }
}
