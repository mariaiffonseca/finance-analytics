package com.mariafonseca.financeanalytics.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mariafonseca.financeanalytics.core.designsystem.LocalFinanceColors
import com.mariafonseca.financeanalytics.core.designsystem.Space16
import com.mariafonseca.financeanalytics.core.designsystem.Space8

/**
 * Compact outlined filter control per docs/project/05_DESIGN_SYSTEM.md section 10.
 * Selected uses an accent fill with white content; unselected stays transparent
 * with a standard-contrast border (textSecondary, matching the outline mapping
 * in Theme.kt rather than the low-contrast divider token).
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFinanceColors.current
    val backgroundColor = if (selected) colors.accent else Color.Transparent
    val borderColor = if (selected) colors.accent else colors.textSecondary
    val contentColor = if (selected) Color.White else colors.text
    val shape = RoundedCornerShape(0.dp)

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = modifier
                .clip(shape)
                .background(color = backgroundColor, shape = shape)
                .border(width = 1.dp, color = borderColor, shape = shape)
                .selectable(selected = selected, onClick = onClick, role = Role.Tab)
                .padding(horizontal = Space16, vertical = Space8),
        )
    }
}
