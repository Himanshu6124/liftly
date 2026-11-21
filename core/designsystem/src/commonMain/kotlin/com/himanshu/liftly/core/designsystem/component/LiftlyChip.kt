package com.himanshu.liftly.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.theme.PlateColors

/** Filled, borderless filter chip: grey when off, plate red (or [selectedColor]) when on. */
@Composable
fun LiftlyChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    selectedColor: Color = Color.Unspecified,
) {
    val colors = MaterialTheme.colorScheme
    val on = if (selectedColor == Color.Unspecified) colors.primary else selectedColor
    val onContent = if (selectedColor == Color.Unspecified) colors.onPrimary else PlateColors.contentOn(selectedColor)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        // Colored chips (plates) also get a check, so "selected" never depends on color alone.
        leadingIcon = leadingIcon ?: if (selected && selectedColor != Color.Unspecified) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceVariant,
            labelColor = colors.onSurface,
            iconColor = colors.onSurfaceVariant,
            selectedContainerColor = on,
            selectedLabelColor = onContent,
            selectedLeadingIconColor = onContent,
        ),
        border = null,
    )
}

/** Filled search field look (no outline, no underline), iOS-style. */
object SearchFieldDefaults {
    val shape = RoundedCornerShape(14.dp)

    @Composable
    fun colors(): TextFieldColors {
        val c = MaterialTheme.colorScheme
        return TextFieldDefaults.colors(
            focusedContainerColor = c.surfaceVariant,
            unfocusedContainerColor = c.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        )
    }
}
