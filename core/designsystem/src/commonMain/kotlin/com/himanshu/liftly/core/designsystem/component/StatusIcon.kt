package com.himanshu.liftly.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.theme.LiftlyColors
import com.himanshu.liftly.core.domain.model.ExerciseStatus

/** Pending ⬜ / Logged ✅ / Skipped ⏭, with a spoken label for screen readers. */
@Composable
fun StatusIcon(status: ExerciseStatus, label: String, modifier: Modifier = Modifier) {
    val described = modifier.size(28.dp).semantics { contentDescription = label }
    when (status) {
        ExerciseStatus.PENDING -> Box(described.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))

        ExerciseStatus.LOGGED -> Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = LiftlyColors.Success,
            modifier = described,
        )

        ExerciseStatus.SKIPPED -> Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = described,
        )
    }
}
