// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.about

sealed interface AboutViewAction {
    data object OpenSource : AboutViewAction
    data object OpenLicenses : AboutViewAction
    data object OpenReleases : AboutViewAction
}
