// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.about

sealed interface AboutViewEvent {
    data class OpenUrl(val url: String) : AboutViewEvent
    data object OpenLicenses : AboutViewEvent
}
