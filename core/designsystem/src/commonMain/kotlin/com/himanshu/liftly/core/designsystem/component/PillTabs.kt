package com.himanshu.liftly.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Segmented control with a sliding thumb (iOS style), used for tabs and short option lists.
 * [descriptions] optionally replaces what screen readers hear for each option.
 */
@Composable
fun PillTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    descriptions: List<String>? = null,
) {
    val colors = MaterialTheme.colorScheme
    val outer = RoundedCornerShape(14.dp)
    val inner = RoundedCornerShape(11.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.surfaceVariant, outer)
            .padding(3.dp),
    ) {
        val segment = maxWidth / options.size.coerceAtLeast(1)
        val thumbX by animateDpAsState(
            segment * selectedIndex,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "pillThumb",
        )
        Box(
            Modifier
                .offset(x = thumbX)
                .width(segment)
                .fillMaxHeight()
                .shadow(2.dp, inner)
                .background(colors.surfaceBright, inner),
        )
        Row(Modifier.fillMaxSize().selectableGroup()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val color by animateColorAsState(if (selected) colors.onSurface else colors.onSurfaceVariant, label = "pillText")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            onClick = { onSelect(i) },
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        )
                        .then(
                            descriptions?.getOrNull(i)?.let { d -> Modifier.semantics { contentDescription = d } } ?: Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
