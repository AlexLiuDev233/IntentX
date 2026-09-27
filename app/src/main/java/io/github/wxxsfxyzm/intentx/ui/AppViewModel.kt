// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wxxsfxyzm.intentx.data.settings.ThemePreferences
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.ThemeState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AppViewModel(preferences: ThemePreferences) : ViewModel() {
    val uiState = preferences.themeStateFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeState())
}
