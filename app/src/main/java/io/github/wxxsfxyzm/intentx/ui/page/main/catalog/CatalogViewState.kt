// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.catalog

import io.github.wxxsfxyzm.intentx.domain.catalog.ActivitySortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityStatusFilter
import io.github.wxxsfxyzm.intentx.domain.catalog.CatalogSortOrder
import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation
import java.util.Locale

data class CatalogItem(
    val id: String,
    val packageName: String,
    val label: String,
    val lastUpdateTime: Long,
    val firstInstallTime: Long = 0L,
    val isSystem: Boolean = false,
    val isOverlay: Boolean = false,
    val exported: Boolean? = null,
    val enabled: Boolean = true,
    val permission: String? = null,
    val operation: IntentOperation = IntentOperation.Activity,
) {
    internal val labelSortKey = CatalogLabelSort.key(label)
    val listKey: String = if (exported == null) "app:$packageName" else "${operation.name.lowercase(Locale.ROOT)}:$packageName/$id"
    val searchText = "$label\n$id\n$packageName".lowercase(Locale.ROOT)
}

data class CatalogViewState(
    val query: String = "",
    val showSystem: Boolean = false,
    val sortOrder: CatalogSortOrder = CatalogSortOrder.Label,
    val reverseOrder: Boolean = false,
    val showPackageName: Boolean = true,
    val searchActivities: Boolean = false,
    val hideOverlays: Boolean = false,
    val activitySortOrder: ActivitySortOrder = ActivitySortOrder.Label,
    val activityExportedFilter: ActivityStatusFilter = ActivityStatusFilter.All,
    val activityEnabledFilter: ActivityStatusFilter = ActivityStatusFilter.All,
    val packageName: String? = null,
    val componentTab: IntentOperation = IntentOperation.Activity,
    val items: List<CatalogItem> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
)
