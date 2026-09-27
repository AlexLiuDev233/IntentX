// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.catalog

import io.github.wxxsfxyzm.intentx.domain.catalog.ActivitySortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityStatusFilter
import io.github.wxxsfxyzm.intentx.domain.catalog.CatalogSortOrder
import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation

sealed interface CatalogViewAction {
    data class SetQuery(val query: String) : CatalogViewAction
    data class SetShowSystem(val show: Boolean) : CatalogViewAction
    data class SetSortOrder(val order: CatalogSortOrder) : CatalogViewAction
    data class SetReverseOrder(val reverse: Boolean) : CatalogViewAction
    data class SetShowPackageName(val show: Boolean) : CatalogViewAction
    data class SetSearchActivities(val search: Boolean) : CatalogViewAction
    data class SetHideOverlays(val hide: Boolean) : CatalogViewAction
    data class SetActivitySortOrder(val order: ActivitySortOrder) : CatalogViewAction
    data class SetActivityExportedFilter(val filter: ActivityStatusFilter) : CatalogViewAction
    data class SetActivityEnabledFilter(val filter: ActivityStatusFilter) : CatalogViewAction
    data class OpenApp(val packageName: String) : CatalogViewAction
    data class OpenActivity(val packageName: String, val className: String, val label: String) : CatalogViewAction
    data class OpenReceiver(val packageName: String, val className: String, val label: String) : CatalogViewAction
    data class SetComponentTab(val operation: IntentOperation) : CatalogViewAction
    data class StartObserving(val packageName: String? = null) : CatalogViewAction
    data object StopObserving : CatalogViewAction
    data object Reload : CatalogViewAction
}
