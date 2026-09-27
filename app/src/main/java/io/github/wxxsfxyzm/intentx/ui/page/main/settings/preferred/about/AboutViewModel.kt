// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.about

import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wxxsfxyzm.intentx.BuildConfig
import io.github.wxxsfxyzm.intentx.data.catalog.AppIconLoader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AboutViewModel(iconLoader: AppIconLoader) : ViewModel() {
    private val _uiState = MutableStateFlow(AboutViewState())
    val uiState = _uiState.asStateFlow()
    private val events = Channel<AboutViewEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val icon = iconLoader.load(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_CODE.toLong())?.asImageBitmap()
            _uiState.update { it.copy(appIcon = icon) }
        }
    }

    fun dispatch(action: AboutViewAction) {
        viewModelScope.launch {
            events.send(
                when (action) {
                    AboutViewAction.OpenSource -> AboutViewEvent.OpenUrl("https://github.com/wxxsfxyzm/IntentX")
                    AboutViewAction.OpenReleases -> AboutViewEvent.OpenUrl("https://github.com/wxxsfxyzm/IntentX/releases")
                    AboutViewAction.OpenLicenses -> AboutViewEvent.OpenLicenses
                },
            )
        }
    }
}
