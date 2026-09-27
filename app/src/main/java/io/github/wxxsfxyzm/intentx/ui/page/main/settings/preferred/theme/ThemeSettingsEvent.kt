// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.theme

sealed interface ThemeSettingsEvent {
    data object SaveFailed : ThemeSettingsEvent
}
