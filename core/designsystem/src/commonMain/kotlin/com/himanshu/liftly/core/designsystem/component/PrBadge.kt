package com.himanshu.liftly.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.theme.LiftlyColors

/** 🏆 "PR" pill. [description] is read by screen readers. */
@Composable
fun PrBadge(description: String, modifier: Modifier = Modifier) {
    Text(
        text = "🏆 PR",
        style = MaterialTheme.typography.labelMedium,
        color = Color.Black,
        modifier = modifier
            .background(LiftlyColors.Pr, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { contentDescription = description },
    )
}
