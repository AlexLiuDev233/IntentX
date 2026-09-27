// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.editor

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wxxsfxyzm.intentx.R
import io.github.wxxsfxyzm.intentx.domain.shortcut.ShortcutResult
import io.github.wxxsfxyzm.intentx.ui.IntentEditor
import io.github.wxxsfxyzm.intentx.ui.LocalAuthorizationUi
import io.github.wxxsfxyzm.intentx.ui.navigation.LocalNavigator
import io.github.wxxsfxyzm.intentx.ui.navigation.Route
import io.github.wxxsfxyzm.intentx.ui.util.CollectUiEvents
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
fun EditorPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    route: Route.Editor? = null,
    viewModel: EditorViewModel = koinViewModel(),
) {
    LaunchedEffect(route) {
        route?.let {
            if (it.profileId != null) {
                viewModel.loadProfile(it.profileId)
            } else {
                viewModel.prefillComponent(it.packageName, it.className, it.activityLabel, it.operation)
            }
        }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val displayedState = if (route != null && (
            (route.profileId != null && state.profileId != route.profileId) ||
                (route.className != null && !state.operationLocked)
            )
    ) {
        state.copy(operation = route.operation, operationLocked = true)
    } else {
        state
    }
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    CollectUiEvents(viewModel.eventFlow) { event ->
        when (event) {
            EditorViewEvent.FeatureNotConnected -> Toast.makeText(context, R.string.editor_ui_only, Toast.LENGTH_SHORT).show()

            EditorViewEvent.LaunchSucceeded -> Toast.makeText(context, R.string.editor_launch_success, Toast.LENGTH_SHORT).show()

            EditorViewEvent.BroadcastSent -> Toast.makeText(context, R.string.editor_broadcast_sent, Toast.LENGTH_SHORT).show()

            EditorViewEvent.SaveSucceeded -> {
                Toast.makeText(context, R.string.profile_saved, Toast.LENGTH_SHORT).show()
                navigator.pop()
            }

            EditorViewEvent.SaveFailed -> Toast.makeText(context, R.string.profile_save_failed, Toast.LENGTH_SHORT).show()

            EditorViewEvent.LaunchFailed -> Toast.makeText(context, R.string.editor_launch_failed, Toast.LENGTH_SHORT).show()

            EditorViewEvent.ActivityDisabled -> Toast.makeText(context, R.string.editor_activity_disabled, Toast.LENGTH_SHORT).show()

            EditorViewEvent.ReceiverDisabled -> Toast.makeText(context, R.string.editor_receiver_disabled, Toast.LENGTH_SHORT).show()

            is EditorViewEvent.InvalidIntent -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()

            is EditorViewEvent.ShortcutFinished -> {
                val message = when (event.result) {
                    ShortcutResult.Requested -> R.string.shortcut_requested
                    ShortcutResult.Updated -> R.string.shortcut_updated
                    ShortcutResult.Unsupported -> R.string.shortcut_unsupported
                    ShortcutResult.XiaomiPermissionRequired -> R.string.shortcut_permission_xiaomi
                    ShortcutResult.ColorOsPermissionRequired -> R.string.shortcut_permission_coloros
                    ShortcutResult.PermissionRequired -> R.string.shortcut_permission_required
                    ShortcutResult.Failed -> R.string.shortcut_failed
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                if (event.result in setOf(ShortcutResult.XiaomiPermissionRequired, ShortcutResult.ColorOsPermissionRequired, ShortcutResult.PermissionRequired)) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                    } catch (error: Exception) {
                        Timber.w(error, "Unable to open shortcut permission settings")
                        // Some OEMs have no handler; the permission hint remains useful.
                    }
                }
            }
        }
    }
    IntentEditor(
        displayedState,
        LocalAuthorizationUi.current.state,
        viewModel::dispatch,
        modifier,
        contentPadding,
        allowOperationSelection = route?.profileId == null && route?.className == null,
    )
}
