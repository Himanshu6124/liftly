package com.himanshu.liftly.feature.history.body

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.ChartPoint
import com.himanshu.liftly.core.designsystem.component.EmptyState
import com.himanshu.liftly.core.designsystem.component.LineChart
import com.himanshu.liftly.core.designsystem.component.SectionHeader
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.model.MeasurementType
import com.himanshu.liftly.core.domain.util.Measurements
import com.himanshu.liftly.feature.history.resources.Res
import com.himanshu.liftly.feature.history.resources.body_arms
import com.himanshu.liftly.feature.history.resources.body_cancel
import com.himanshu.liftly.feature.history.resources.body_change
import com.himanshu.liftly.feature.history.resources.body_chart_description
import com.himanshu.liftly.feature.history.resources.body_chest
import com.himanshu.liftly.feature.history.resources.body_delete
import com.himanshu.liftly.feature.history.resources.body_deleted
import com.himanshu.liftly.feature.history.resources.body_empty_message
import com.himanshu.liftly.feature.history.resources.body_empty_title
import com.himanshu.liftly.feature.history.resources.body_entries
import com.himanshu.liftly.feature.history.resources.body_fat
import com.himanshu.liftly.feature.history.resources.body_hips
import com.himanshu.liftly.feature.history.resources.body_invalid
import com.himanshu.liftly.feature.history.resources.body_log
import com.himanshu.liftly.feature.history.resources.body_neck
import com.himanshu.liftly.feature.history.resources.body_one_entry
import com.himanshu.liftly.feature.history.resources.body_save
import com.himanshu.liftly.feature.history.resources.body_thighs
import com.himanshu.liftly.feature.history.resources.body_value
import com.himanshu.liftly.feature.history.resources.body_waist
import com.himanshu.liftly.feature.history.resources.body_weight
import com.himanshu.liftly.feature.history.resources.history_undo
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

@Composable
fun BodyScreen(viewModel: BodyViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect {
            val result = snackbar.showSnackbar(
                getString(Res.string.body_deleted),
                getString(Res.string.history_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        }
    }
    var logging by remember { mutableStateOf(false) }
    val typeLabel = stringResource(state.type.label())

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { logging = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.body_log, typeLabel.lowercase())) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TypePicker(state, viewModel::select)
            when {
                state.isLoading -> Unit

                state.entries.isEmpty() -> EmptyState(
                    Icons.Filled.Add,
                    stringResource(Res.string.body_empty_title, typeLabel.lowercase()),
                    stringResource(Res.string.body_empty_message),
                )

                else -> BodyContent(state, typeLabel, onDelete = viewModel::delete)
            }
        }
    }
    if (logging) LogDialog(typeLabel, state.symbol, onDismiss = { logging = false }, onSave = viewModel::add)
}

@Composable
private fun TypePicker(state: BodyUiState, onSelect: (MeasurementType) -> Unit) {
    val spacing = LiftlyTheme.spacing
    // Types already in use come first, so the ones you track are one tap away.
    val ordered = MeasurementType.entries.sortedBy { it !in state.usedTypes }
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        ordered.forEach { t ->
            FilterChip(selected = t == state.type, onClick = { onSelect(t) }, label = { Text(stringResource(t.label())) })
        }
    }
}

@Composable
private fun BodyContent(state: BodyUiState, typeLabel: String, onDelete: (String) -> Unit) {
    val spacing = LiftlyTheme.spacing
    fun fmt(v: Double) = "${Measurements.format(v)} ${state.symbol}"
    LazyColumn(
        contentPadding = PaddingValues(start = spacing.md, end = spacing.md, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "chart") {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    val latest = state.latest ?: return@Column
                    Text(fmt(latest.value), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    state.change?.let { change ->
                        val sign = if (change > 0) {
                            "+"
                        } else if (change < 0) {
                            "−"
                        } else {
                            "±"
                        }
                        Text(
                            stringResource(
                                Res.string.body_change,
                                "$sign${fmt(kotlin.math.abs(change))}",
                                state.entries.first().measuredAt.date(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.entries.size < 2) {
                        Text(
                            stringResource(Res.string.body_one_entry),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val first = state.entries.first()
                        LineChart(
                            points = state.entries.map { ChartPoint(it.measuredAt.toEpochMilliseconds(), it.value) },
                            description = stringResource(
                                Res.string.body_chart_description,
                                typeLabel,
                                state.entries.size,
                                fmt(first.value),
                                first.measuredAt.date(),
                                fmt(latest.value),
                                latest.measuredAt.date(),
                            ),
                            formatY = { Measurements.format(it) },
                            formatX = { Instant.fromEpochMilliseconds(it).date() },
                            selectedIndex = state.entries.lastIndex,
                        )
                    }
                }
            }
        }
        item(key = "entries-header") { SectionHeader(stringResource(Res.string.body_entries)) }
        items(state.entries.asReversed(), key = { it.id }) { e ->
            ListItem(
                headlineContent = { Text(fmt(e.value)) },
                supportingContent = { Text(e.measuredAt.date()) },
                trailingContent = {
                    IconButton(onClick = { onDelete(e.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.body_delete, e.measuredAt.date()))
                    }
                },
            )
        }
    }
}

@Composable
private fun LogDialog(typeLabel: String, symbol: String, onDismiss: () -> Unit, onSave: (String) -> Boolean) {
    val field = rememberTextFieldState()
    var invalid by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } } // keypad up right away: one less tap
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.body_log, typeLabel.lowercase())) },
        text = {
            OutlinedTextField(
                state = field,
                label = { Text(stringResource(Res.string.body_value)) },
                suffix = { Text(symbol) },
                isError = invalid,
                supportingText = if (invalid) ({ Text(stringResource(Res.string.body_invalid)) }) else null,
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (onSave(field.text.toString())) onDismiss() else invalid = true }) {
                Text(stringResource(Res.string.body_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.body_cancel)) } },
    )
}

private fun Instant.date() = toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

private fun MeasurementType.label(): StringResource = when (this) {
    MeasurementType.BODY_WEIGHT -> Res.string.body_weight
    MeasurementType.BODY_FAT -> Res.string.body_fat
    MeasurementType.WAIST -> Res.string.body_waist
    MeasurementType.CHEST -> Res.string.body_chest
    MeasurementType.HIPS -> Res.string.body_hips
    MeasurementType.ARMS -> Res.string.body_arms
    MeasurementType.THIGHS -> Res.string.body_thighs
    MeasurementType.NECK -> Res.string.body_neck
}
