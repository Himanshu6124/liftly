package com.himanshu.liftly.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.himanshu.liftly.core.designsystem.component.workoutDisplayName
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.feature.home.resources.Res
import com.himanshu.liftly.feature.home.resources.home_greeting
import com.himanshu.liftly.feature.home.resources.home_in_progress
import com.himanshu.liftly.feature.home.resources.home_in_progress_detail
import com.himanshu.liftly.feature.home.resources.home_no_routines
import com.himanshu.liftly.feature.home.resources.home_resume
import com.himanshu.liftly.feature.home.resources.home_start_empty
import com.himanshu.liftly.feature.home.resources.home_sync_failed
import com.himanshu.liftly.feature.home.resources.home_sync_local_only
import com.himanshu.liftly.feature.home.resources.home_sync_synced
import com.himanshu.liftly.feature.home.resources.home_sync_syncing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onOpenActiveWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.OpenActiveWorkout -> onOpenActiveWorkout()
            }
        }
    }
    HomeContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LiftlyTheme.spacing
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(stringResource(Res.string.home_greeting), style = MaterialTheme.typography.headlineMedium)
        AssistChip(onClick = {}, label = { Text(state.syncLabel.text()) })
        state.inProgress?.let { ResumeCard(it, onClick = { onIntent(HomeIntent.ResumeWorkout) }) }
        Text(
            stringResource(Res.string.home_no_routines),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        // Primary action sits at the bottom: reachable with one hand.
        Button(
            onClick = { onIntent(if (state.inProgress != null) HomeIntent.ResumeWorkout else HomeIntent.StartEmptyWorkout) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(
                stringResource(if (state.inProgress != null) Res.string.home_resume else Res.string.home_start_empty),
                modifier = Modifier.padding(start = spacing.sm),
            )
        }
    }
}

@Composable
private fun ResumeCard(banner: InProgressBanner, onClick: () -> Unit) {
    val spacing = LiftlyTheme.spacing
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(stringResource(Res.string.home_in_progress).uppercase(), style = MaterialTheme.typography.labelMedium)
            Text(workoutDisplayName(banner.name, banner.startedAt), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(Res.string.home_in_progress_detail, banner.loggedCount, banner.exerciseCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SyncLabel.text() = stringResource(
    when (this) {
        SyncLabel.LOCAL_ONLY -> Res.string.home_sync_local_only
        SyncLabel.SYNCING -> Res.string.home_sync_syncing
        SyncLabel.SYNCED -> Res.string.home_sync_synced
        SyncLabel.FAILED -> Res.string.home_sync_failed
    },
)
