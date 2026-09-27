// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/** One collector per screen; retain buffered events while the screen is stopped. */
@Composable
fun <T> CollectUiEvents(events: Flow<T>, onEvent: (T) -> Unit) {
    val owner = LocalLifecycleOwner.current
    val handler = rememberUpdatedState(onEvent)
    LaunchedEffect(events, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            events.collect { handler.value(it) }
        }
    }
}
