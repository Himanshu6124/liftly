package com.himanshu.liftly.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.theme.LiftlyColors
import com.himanshu.liftly.core.domain.model.ExerciseStatus

/** Pending ring / Logged green check (springs in) / Skipped arrow, with a spoken label. */
@Composable
fun StatusIcon(status: ExerciseStatus, label: String, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = status,
        transitionSpec = {
            (scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), initialScale = 0.4f) + fadeIn()) togetherWith
                fadeOut()
        },
        contentAlignment = Alignment.Center,
        label = "status",
        modifier = modifier.size(28.dp).clearAndSetSemantics { contentDescription = label },
    ) { s ->
        when (s) {
            ExerciseStatus.PENDING -> Box(Modifier.size(28.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))

            ExerciseStatus.LOGGED -> Box(
                Modifier.size(28.dp).background(LiftlyColors.Success, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            ExerciseStatus.SKIPPED -> Box(
                Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
