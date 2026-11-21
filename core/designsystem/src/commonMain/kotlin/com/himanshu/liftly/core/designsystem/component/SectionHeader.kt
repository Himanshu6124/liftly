package com.himanshu.liftly.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme

/** Small uppercase eyebrow above a group of rows. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val spacing = LiftlyTheme.spacing
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .semantics { heading() },
    )
}
