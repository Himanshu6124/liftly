package com.himanshu.liftly.feature.exercises.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.component.ExerciseThumbnail
import com.himanshu.liftly.core.designsystem.component.label
import com.himanshu.liftly.feature.exercises.resources.Res
import com.himanshu.liftly.feature.exercises.resources.exercises_favorite_add
import com.himanshu.liftly.feature.exercises.resources.exercises_favorite_remove
import com.himanshu.liftly.feature.exercises.resources.exercises_row_subtitle
import org.jetbrains.compose.resources.stringResource

/**
 * One exercise in a list. [selected] != null turns it into a selectable row (picker): the
 * thumbnail shows a check mark and the row is tinted.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExerciseRowItem(
    row: ExerciseRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val isSelected = selected == true
    ListItem(
        modifier = modifier
            .semantics { if (selected != null) this.selected = isSelected }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = if (selected != null) Role.Checkbox else Role.Button,
            ),
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        leadingContent = {
            Box {
                ExerciseThumbnail(row.imagePaths, contentDescription = null)
                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        },
        headlineContent = { Text(row.name, maxLines = 1) },
        supportingContent = {
            Text(stringResource(Res.string.exercises_row_subtitle, row.primaryMuscle.label(), row.equipment.label()), maxLines = 1)
        },
        trailingContent = trailing,
    )
}

@Composable
internal fun FavoriteButton(row: ExerciseRow, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (row.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(
                if (row.isFavorite) Res.string.exercises_favorite_remove else Res.string.exercises_favorite_add,
                row.name,
            ),
            tint = if (row.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
