package com.himanshu.liftly.feature.exercises.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.core.designsystem.component.ExerciseThumbnail
import com.himanshu.liftly.core.designsystem.component.LiftlyChip
import com.himanshu.liftly.core.designsystem.component.LocalFloatingBarInset
import com.himanshu.liftly.core.designsystem.component.SearchFieldDefaults
import com.himanshu.liftly.core.designsystem.component.SectionHeader
import com.himanshu.liftly.core.designsystem.component.label
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.Equipment
import com.himanshu.liftly.core.domain.model.MuscleGroup
import com.himanshu.liftly.feature.exercises.resources.Res
import com.himanshu.liftly.feature.exercises.resources.exercises_clear_filters
import com.himanshu.liftly.feature.exercises.resources.exercises_clear_search
import com.himanshu.liftly.feature.exercises.resources.exercises_favorite_add
import com.himanshu.liftly.feature.exercises.resources.exercises_favorite_remove
import com.himanshu.liftly.feature.exercises.resources.exercises_no_results_message
import com.himanshu.liftly.feature.exercises.resources.exercises_no_results_title
import com.himanshu.liftly.feature.exercises.resources.exercises_row_subtitle
import com.himanshu.liftly.feature.exercises.resources.exercises_search_hint
import com.himanshu.liftly.feature.exercises.resources.exercises_section_favorites
import com.himanshu.liftly.feature.exercises.resources.exercises_section_results
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExerciseLibraryScreen(
    onOpenExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExerciseLibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    ExerciseLibraryContent(
        state = state,
        query = query,
        onIntent = viewModel::onIntent,
        onOpenExercise = onOpenExercise,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseLibraryContent(
    state: ExerciseLibraryUiState,
    query: String,
    onIntent: (ExerciseLibraryIntent) -> Unit,
    onOpenExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LiftlyTheme.spacing
    val focus = LocalFocusManager.current
    val listState = rememberLazyListState()

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        TextField(
            value = query,
            onValueChange = { onIntent(ExerciseLibraryIntent.QueryChanged(it)) },
            placeholder = { Text(stringResource(Res.string.exercises_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onIntent(ExerciseLibraryIntent.QueryChanged("")) }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(Res.string.exercises_clear_search))
                    }
                }
            },
            singleLine = true,
            shape = SearchFieldDefaults.shape,
            colors = SearchFieldDefaults.colors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
        )
        FilterRow(state, onIntent)

        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Filled.Search,
                title = stringResource(Res.string.exercises_no_results_title),
                message = stringResource(Res.string.exercises_no_results_message, state.query.trim()),
                actionLabel = stringResource(Res.string.exercises_clear_filters),
                onAction = { onIntent(ExerciseLibraryIntent.ClearFilters) },
            )
            return@Column
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = spacing.lg + LocalFloatingBarInset.current),
            modifier = Modifier.fillMaxSize(),
        ) {
            state.sections.forEach { section ->
                stickyHeader(key = "header-${section.key}", contentType = "header") {
                    val title = when (section.kind) {
                        LibrarySection.Kind.FAVORITES -> stringResource(Res.string.exercises_section_favorites)
                        LibrarySection.Kind.RESULTS -> stringResource(Res.string.exercises_section_results)
                        LibrarySection.Kind.LETTER -> section.letter.toString()
                    }
                    SectionHeader(title, Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background))
                }
                items(section.rows, key = { "${section.key}-${it.id}" }, contentType = { "row" }) { row ->
                    ExerciseRowItem(
                        row = row,
                        onClick = { onOpenExercise(row.id) },
                        trailing = { FavoriteButton(row) { onIntent(ExerciseLibraryIntent.FavoriteToggled(row.id, !row.isFavorite)) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(state: ExerciseLibraryUiState, onIntent: (ExerciseLibraryIntent) -> Unit) {
    val spacing = LiftlyTheme.spacing
    LazyRow(
        contentPadding = PaddingValues(horizontal = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        items(MuscleGroup.entries, key = { "g-${it.name}" }) { group ->
            LiftlyChip(
                selected = state.muscleGroup == group,
                onClick = { onIntent(ExerciseLibraryIntent.MuscleGroupToggled(group)) },
                label = { Text(group.label()) },
            )
        }
        // Fixed height: a fillMaxHeight divider inside a LazyRow stretches the whole row once it's visible.
        item(key = "divider") { VerticalDivider(Modifier.padding(vertical = spacing.sm).height(32.dp)) }
        items(Equipment.entries, key = { "e-${it.name}" }) { equipment ->
            LiftlyChip(
                selected = state.equipment == equipment,
                onClick = { onIntent(ExerciseLibraryIntent.EquipmentToggled(equipment)) },
                label = { Text(equipment.label()) },
            )
        }
    }
    HorizontalDivider(Modifier.padding(top = spacing.xs))
}
