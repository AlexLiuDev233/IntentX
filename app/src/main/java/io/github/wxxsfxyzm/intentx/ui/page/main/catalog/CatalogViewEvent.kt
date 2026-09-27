// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.catalog

import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation

sealed interface CatalogViewEvent {
    data class ShowMessage(val message: Int) : CatalogViewEvent
    data class NavigateToActivities(val packageName: String, val appLabel: String) : CatalogViewEvent
    data class NavigateToEditor(
        val packageName: String,
        val className: String,
        val activityLabel: String,
        val operation: IntentOperation = IntentOperation.Activity,
    ) : CatalogViewEvent
}
