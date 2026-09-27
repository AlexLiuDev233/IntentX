// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

private class ImeFocusField(val onDismiss: () -> Unit)

private class ImeFocusBinding(val focusChanged: (ImeFocusField, Boolean) -> Unit, val pressed: (ImeFocusField) -> Unit)

private val LocalImeFocusBinding = staticCompositionLocalOf<ImeFocusBinding?> { null }

/** Owns one observer for a page or a modal window. Covered scopes only discard pending work. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImeDismissalFocusScope(enabled: Boolean = true, content: @Composable () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val window = LocalWindowInfo.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val source = WindowInsets.imeAnimationSource
    val target = WindowInsets.imeAnimationTarget
    val visible by rememberUpdatedState(WindowInsets.isImeVisible)
    val latestEnabled by rememberUpdatedState(enabled)
    val state = remember { ImeDismissalState<ImeFocusField>() }
    val readKeyboard by rememberUpdatedState {
        ImeDismissalState.Keyboard(visible, ime.getBottom(density), source.getBottom(density), target.getBottom(density))
    }
    val isActive by rememberUpdatedState {
        latestEnabled && window.isWindowFocused && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }
    val binding = remember(state) {
        ImeFocusBinding(
            focusChanged = { field, focused ->
                state.focusChanged(field, focused, if (isActive()) readKeyboard() else null)
            },
            pressed = { field -> state.pressed(field, if (isActive()) readKeyboard() else null) },
        )
    }

    LaunchedEffect(owner, state, focusManager) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                snapshotFlow { if (isActive()) readKeyboard() else null }
                    .collect { keyboard ->
                        if (keyboard == null) {
                            state.resetObservation()
                        } else if (state.keyboardChanged(keyboard) && isActive()) {
                            // Editable ExposedDropdownMenu anchors request focus while expanded.
                            state.focusedField?.onDismiss?.invoke()
                            focusManager.clearFocus(force = true)
                        }
                    }
            } finally {
                state.resetObservation()
            }
        }
    }

    CompositionLocalProvider(LocalImeFocusBinding provides binding, content = content)
}

/** Attach before the TextField's focus target. Observing a down event never consumes the gesture. */
@Composable
internal fun Modifier.clearFocusOnImeDismiss(onDismiss: () -> Unit = {}): Modifier {
    val binding = LocalImeFocusBinding.current ?: return this
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    // Composition identity also survives keyed Category/Extra row moves.
    val field = remember { ImeFocusField { latestOnDismiss() } }
    DisposableEffect(binding, field) {
        onDispose { binding.focusChanged(field, false) }
    }
    return this
        .onFocusChanged { binding.focusChanged(field, it.isFocused) }
        .pointerInput(binding, field) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                binding.pressed(field)
            }
        }
}
