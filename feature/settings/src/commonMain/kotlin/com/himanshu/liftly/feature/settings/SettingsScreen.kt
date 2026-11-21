package com.himanshu.liftly.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.himanshu.liftly.core.designsystem.component.NumberStepper
import com.himanshu.liftly.core.designsystem.component.SectionHeader
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.designsystem.theme.supportsDynamicColor
import com.himanshu.liftly.core.domain.model.ThemeMode
import com.himanshu.liftly.core.domain.model.UserSettings
import com.himanshu.liftly.core.domain.model.WeightUnit
import com.himanshu.liftly.core.domain.repository.SettingsRepository
import com.himanshu.liftly.core.domain.util.SetFormatter.symbol
import com.himanshu.liftly.feature.settings.resources.Res
import com.himanshu.liftly.feature.settings.resources.settings_about
import com.himanshu.liftly.feature.settings.resources.settings_appearance
import com.himanshu.liftly.feature.settings.resources.settings_attribution
import com.himanshu.liftly.feature.settings.resources.settings_auto_open
import com.himanshu.liftly.feature.settings.resources.settings_auto_open_hint
import com.himanshu.liftly.feature.settings.resources.settings_bar_weight
import com.himanshu.liftly.feature.settings.resources.settings_barbell
import com.himanshu.liftly.feature.settings.resources.settings_default_sets
import com.himanshu.liftly.feature.settings.resources.settings_default_sets_hint
import com.himanshu.liftly.feature.settings.resources.settings_dynamic_color
import com.himanshu.liftly.feature.settings.resources.settings_goal_down
import com.himanshu.liftly.feature.settings.resources.settings_goal_up
import com.himanshu.liftly.feature.settings.resources.settings_increment
import com.himanshu.liftly.feature.settings.resources.settings_kg
import com.himanshu.liftly.feature.settings.resources.settings_lb
import com.himanshu.liftly.feature.settings.resources.settings_plates
import com.himanshu.liftly.feature.settings.resources.settings_plates_hint
import com.himanshu.liftly.feature.settings.resources.settings_sets_down
import com.himanshu.liftly.feature.settings.resources.settings_sets_up
import com.himanshu.liftly.feature.settings.resources.settings_storage
import com.himanshu.liftly.feature.settings.resources.settings_theme_dark
import com.himanshu.liftly.feature.settings.resources.settings_theme_light
import com.himanshu.liftly.feature.settings.resources.settings_theme_system
import com.himanshu.liftly.feature.settings.resources.settings_units
import com.himanshu.liftly.feature.settings.resources.settings_version
import com.himanshu.liftly.feature.settings.resources.settings_weekly_goal
import com.himanshu.liftly.feature.settings.resources.settings_weekly_goal_hint
import com.himanshu.liftly.feature.settings.resources.settings_workout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<UserSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings(),
    )

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    companion object {
        val KG_STEPS = listOf(1.0, 1.25, 2.5, 5.0)
        val LB_STEPS = listOf(2.5, 5.0, 10.0)
        const val MAX_DEFAULT_SETS = 10
        const val MAX_WEEKLY_GOAL = 7
        val BARS_KG = listOf(10.0, 15.0, 20.0, 25.0)
        val BARS_LB = listOf(15.0, 25.0, 35.0, 45.0)
        val PLATES_KG = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 2.0, 1.25, 1.0, 0.5)
        val PLATES_LB = listOf(55.0, 45.0, 35.0, 25.0, 10.0, 5.0, 2.5, 1.25)
        const val APP_VERSION = "1.0.0"
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsContent(settings, viewModel::update)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(s: UserSettings, update: ((UserSettings) -> UserSettings) -> Unit) {
    val spacing = LiftlyTheme.spacing
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        SectionHeader(stringResource(Res.string.settings_units))
        Column(Modifier.padding(horizontal = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Segmented(
                options = listOf(
                    WeightUnit.KG to stringResource(Res.string.settings_kg),
                    WeightUnit.LB to stringResource(Res.string.settings_lb),
                ),
                selected = s.weightUnit,
                onSelect = { unit -> update { it.copy(weightUnit = unit) } },
            )
            Text(stringResource(Res.string.settings_increment), style = MaterialTheme.typography.bodyLarge)
            val steps = if (s.weightUnit == WeightUnit.KG) SettingsViewModel.KG_STEPS else SettingsViewModel.LB_STEPS
            val current = if (s.weightUnit == WeightUnit.KG) s.weightIncrementKg else s.weightIncrementLb
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                steps.forEach { step ->
                    FilterChip(
                        selected = current == step,
                        onClick = {
                            update {
                                if (it.weightUnit ==
                                    WeightUnit.KG
                                ) {
                                    it.copy(weightIncrementKg = step)
                                } else {
                                    it.copy(weightIncrementLb = step)
                                }
                            }
                        },
                        label = { Text("${formatStep(step)} ${s.weightUnit.symbol}") },
                    )
                }
            }
        }

        HorizontalDivider()
        SectionHeader(stringResource(Res.string.settings_workout))
        Row(Modifier.padding(horizontal = spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.settings_default_sets), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(Res.string.settings_default_sets_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NumberStepper(
                value = s.defaultSetCount.toString(),
                onDecrement = { update { it.copy(defaultSetCount = (it.defaultSetCount - 1).coerceAtLeast(1)) } },
                onIncrement = {
                    update { it.copy(defaultSetCount = (it.defaultSetCount + 1).coerceAtMost(SettingsViewModel.MAX_DEFAULT_SETS)) }
                },
                decrementDescription = stringResource(Res.string.settings_sets_down),
                incrementDescription = stringResource(Res.string.settings_sets_up),
                canDecrement = s.defaultSetCount > 1,
                canIncrement = s.defaultSetCount < SettingsViewModel.MAX_DEFAULT_SETS,
            )
        }
        Row(Modifier.padding(horizontal = spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.settings_weekly_goal), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(Res.string.settings_weekly_goal_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NumberStepper(
                value = s.weeklyGoal.toString(),
                onDecrement = { update { it.copy(weeklyGoal = (it.weeklyGoal - 1).coerceAtLeast(1)) } },
                onIncrement = { update { it.copy(weeklyGoal = (it.weeklyGoal + 1).coerceAtMost(SettingsViewModel.MAX_WEEKLY_GOAL)) } },
                decrementDescription = stringResource(Res.string.settings_goal_down),
                incrementDescription = stringResource(Res.string.settings_goal_up),
                canDecrement = s.weeklyGoal > 1,
                canIncrement = s.weeklyGoal < SettingsViewModel.MAX_WEEKLY_GOAL,
            )
        }
        SwitchRow(
            title = stringResource(Res.string.settings_auto_open),
            subtitle = stringResource(Res.string.settings_auto_open_hint),
            checked = s.openNextExerciseAutomatically,
            onChange = { v -> update { it.copy(openNextExerciseAutomatically = v) } },
        )

        HorizontalDivider()
        BarbellSettings(s, update)

        HorizontalDivider()
        SectionHeader(stringResource(Res.string.settings_appearance))
        Column(Modifier.padding(horizontal = spacing.md)) {
            Segmented(
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(Res.string.settings_theme_system),
                    ThemeMode.LIGHT to stringResource(Res.string.settings_theme_light),
                    ThemeMode.DARK to stringResource(Res.string.settings_theme_dark),
                ),
                selected = s.themeMode,
                onSelect = { mode -> update { it.copy(themeMode = mode) } },
            )
        }
        if (supportsDynamicColor) {
            SwitchRow(stringResource(Res.string.settings_dynamic_color), null, s.dynamicColor) { v -> update { it.copy(dynamicColor = v) } }
        }

        HorizontalDivider()
        SectionHeader(stringResource(Res.string.settings_about))
        Column(Modifier.padding(horizontal = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(stringResource(Res.string.settings_version, SettingsViewModel.APP_VERSION), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(Res.string.settings_storage), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(Res.string.settings_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val spacing = LiftlyTheme.spacing
    Row(Modifier.fillMaxWidth().padding(horizontal = spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = spacing.sm)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Bar weight and owned plates for the current unit (plate calculator + warm-up rounding). */
@Composable
private fun BarbellSettings(s: UserSettings, update: ((UserSettings) -> UserSettings) -> Unit) {
    val spacing = LiftlyTheme.spacing
    val kg = s.weightUnit == WeightUnit.KG
    SectionHeader(stringResource(Res.string.settings_barbell))
    Column(Modifier.padding(horizontal = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(stringResource(Res.string.settings_bar_weight), style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            (if (kg) SettingsViewModel.BARS_KG else SettingsViewModel.BARS_LB).forEach { bar ->
                FilterChip(
                    selected = s.barWeight(s.weightUnit) == bar,
                    onClick = { update { if (kg) it.copy(barWeightKg = bar) else it.copy(barWeightLb = bar) } },
                    label = { Text("${formatStep(bar)} ${s.weightUnit.symbol}") },
                )
            }
        }
        Text(stringResource(Res.string.settings_plates), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(Res.string.settings_plates_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            val owned = s.plates(s.weightUnit)
            (if (kg) SettingsViewModel.PLATES_KG else SettingsViewModel.PLATES_LB).forEach { plate ->
                FilterChip(
                    selected = plate in owned,
                    onClick = {
                        val next = (if (plate in owned) owned - plate else owned + plate).sortedDescending()
                        update { if (kg) it.copy(platesKg = next) else it.copy(platesLb = next) }
                    },
                    label = { Text(formatStep(plate)) },
                )
            }
        }
    }
}

private fun formatStep(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
