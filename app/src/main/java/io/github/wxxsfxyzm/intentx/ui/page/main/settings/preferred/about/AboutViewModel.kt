// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AboutViewModel : ViewModel() {
    private val events = Channel<AboutViewEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

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
