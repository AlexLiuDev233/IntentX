// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wxxsfxyzm.intentx.data.settings.ThemePreferences
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class ThemeSettingsViewModel(private val preferences: ThemePreferences) : ViewModel() {
    private val events = Channel<ThemeSettingsEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()
    val uiState = preferences.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ThemeSettingsState(),
    )

    fun dispatch(action: ThemeSettingsAction) {
        viewModelScope.launch {
            try {
                preferences.update(action)
            } catch (error: IOException) {
                Timber.e(error, "Unable to save appearance setting: %s", action.javaClass.simpleName)
                events.send(ThemeSettingsEvent.SaveFailed)
            }
        }
    }
}
