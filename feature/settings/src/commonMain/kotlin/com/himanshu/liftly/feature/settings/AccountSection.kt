package com.himanshu.liftly.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.himanshu.liftly.core.designsystem.component.SectionHeader
import com.himanshu.liftly.core.designsystem.theme.LiftlyTheme
import com.himanshu.liftly.core.domain.auth.AuthError
import com.himanshu.liftly.core.domain.auth.AuthProvider
import com.himanshu.liftly.core.domain.auth.AuthState
import com.himanshu.liftly.core.domain.sync.SyncState
import com.himanshu.liftly.core.domain.util.DateLabels
import com.himanshu.liftly.feature.settings.resources.Res
import com.himanshu.liftly.feature.settings.resources.account_apple
import com.himanshu.liftly.feature.settings.resources.account_cancel
import com.himanshu.liftly.feature.settings.resources.account_delete
import com.himanshu.liftly.feature.settings.resources.account_delete_body
import com.himanshu.liftly.feature.settings.resources.account_delete_confirm
import com.himanshu.liftly.feature.settings.resources.account_delete_title
import com.himanshu.liftly.feature.settings.resources.account_error_network
import com.himanshu.liftly.feature.settings.resources.account_error_rejected
import com.himanshu.liftly.feature.settings.resources.account_error_unavailable
import com.himanshu.liftly.feature.settings.resources.account_google
import com.himanshu.liftly.feature.settings.resources.account_no_providers
import com.himanshu.liftly.feature.settings.resources.account_session_expired
import com.himanshu.liftly.feature.settings.resources.account_sign_out
import com.himanshu.liftly.feature.settings.resources.account_signed_in
import com.himanshu.liftly.feature.settings.resources.account_signed_in_as
import com.himanshu.liftly.feature.settings.resources.account_signed_out_hint
import com.himanshu.liftly.feature.settings.resources.account_sync_failed
import com.himanshu.liftly.feature.settings.resources.account_sync_now
import com.himanshu.liftly.feature.settings.resources.account_synced_at
import com.himanshu.liftly.feature.settings.resources.account_syncing
import com.himanshu.liftly.feature.settings.resources.settings_account
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/** Actions from [AccountSection]; the screen forwards them to [AccountViewModel]. */
interface AccountActions {
    fun signIn(provider: AuthProvider)
    fun signOut()
    fun deleteAccount()
    fun syncNow()
}

@Composable
fun AccountSection(ui: AccountUi, actions: AccountActions) {
    val spacing = LiftlyTheme.spacing
    SectionHeader(stringResource(Res.string.settings_account))
    Column(Modifier.padding(horizontal = spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        when (val state = ui.state) {
            is AuthState.SignedIn -> {
                SignedIn(state, ui.busy, actions)
                SyncStatus(ui.sync, actions)
            }

            is AuthState.SignedOut -> SignedOut(state, ui, actions)
        }
        ui.error?.let { error ->
            Text(
                stringResource(error.message()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun SignedOut(state: AuthState.SignedOut, ui: AccountUi, actions: AccountActions) {
    Hint(stringResource(if (state.expired) Res.string.account_session_expired else Res.string.account_signed_out_hint))
    if (ui.providers.isEmpty()) {
        Hint(stringResource(Res.string.account_no_providers))
        return
    }
    ui.providers.forEach { provider ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (provider) {
                AuthProvider.APPLE -> AppleSignInButton(enabled = !ui.busy, onClick = { actions.signIn(provider) }, Modifier.weight(1f))

                AuthProvider.GOOGLE -> OutlinedButton(
                    onClick = { actions.signIn(provider) },
                    enabled = !ui.busy,
                    modifier = Modifier.weight(1f).height(BUTTON_HEIGHT),
                ) { Text(stringResource(Res.string.account_google)) }
            }
            if (ui.signingInWith == provider) CircularProgressIndicator(Modifier.padding(start = LiftlyTheme.spacing.sm).size(24.dp))
        }
    }
}

@Composable
private fun SignedIn(state: AuthState.SignedIn, busy: Boolean, actions: AccountActions) {
    var confirmDelete by remember { mutableStateOf(false) }
    Text(
        state.email?.let { stringResource(Res.string.account_signed_in_as, it) } ?: stringResource(Res.string.account_signed_in),
        style = MaterialTheme.typography.bodyLarge,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LiftlyTheme.spacing.sm)) {
        OutlinedButton(onClick = actions::signOut, enabled = !busy) { Text(stringResource(Res.string.account_sign_out)) }
        TextButton(onClick = { confirmDelete = true }, enabled = !busy) {
            Text(stringResource(Res.string.account_delete), color = MaterialTheme.colorScheme.error)
        }
        if (busy) CircularProgressIndicator(Modifier.size(24.dp))
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.account_delete_title)) },
            text = { Text(stringResource(Res.string.account_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.deleteAccount()
                }) { Text(stringResource(Res.string.account_delete_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.account_cancel)) } },
        )
    }
}

@Composable
private fun SyncStatus(sync: SyncState, actions: AccountActions) {
    val text = when (sync) {
        SyncState.Syncing, SyncState.Idle, SyncState.LocalOnly -> stringResource(Res.string.account_syncing)
        is SyncState.Synced -> stringResource(Res.string.account_synced_at, syncTime(sync.at))
        is SyncState.Failed -> stringResource(Res.string.account_sync_failed)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (sync is SyncState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
        )
        TextButton(onClick = actions::syncNow, enabled = sync !is SyncState.Syncing) {
            Text(stringResource(Res.string.account_sync_now))
        }
    }
}

/** "14:05" today, otherwise the short date. */
private fun syncTime(at: Instant): String {
    val zone = TimeZone.currentSystemDefault()
    val local = at.toLocalDateTime(zone)
    val today = Clock.System.now().toLocalDateTime(zone).date
    return if (local.date == today) {
        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    } else {
        DateLabels.short(at, zone)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun AuthError.message() = when (this) {
    AuthError.NETWORK -> Res.string.account_error_network
    AuthError.REJECTED -> Res.string.account_error_rejected
    AuthError.UNAVAILABLE, AuthError.CANCELLED -> Res.string.account_error_unavailable
}

internal val BUTTON_HEIGHT = 48.dp

/** Apple's own button on iOS (its look is required); a plain button elsewhere. */
@Composable
internal expect fun AppleSignInButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)

@Composable
internal fun FallbackAppleButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(BUTTON_HEIGHT)) {
        Text(stringResource(Res.string.account_apple))
    }
}
