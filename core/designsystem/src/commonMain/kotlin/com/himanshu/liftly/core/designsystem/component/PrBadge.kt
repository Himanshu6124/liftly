package com.himanshu.liftly.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.theme.LiftlyColors

/** Gold "PR" pill (the yellow plate). [description] is read by screen readers. */
@Composable
fun PrBadge(description: String, modifier: Modifier = Modifier) {
    Text(
        text = "PR",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = LiftlyColors.OnPr,
        modifier = modifier
            .background(LiftlyColors.Pr, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clearAndSetSemantics { contentDescription = description },
    )
}
