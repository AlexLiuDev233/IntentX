// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.catalog

import io.github.wxxsfxyzm.intentx.domain.catalog.ActivitySortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityStatusFilter
import io.github.wxxsfxyzm.intentx.domain.catalog.CatalogSortOrder
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Filter and sort the cached index; typing never queries PackageManager or reloads icons. */
internal suspend fun filterCatalogItems(
    items: List<CatalogItem>,
    query: String,
    showSystem: Boolean,
    hideOverlays: Boolean,
    sortOrder: CatalogSortOrder,
    reverseOrder: Boolean,
    isAppCatalog: Boolean,
    activitySortOrder: ActivitySortOrder = ActivitySortOrder.Label,
    exportedFilter: ActivityStatusFilter = ActivityStatusFilter.All,
    enabledFilter: ActivityStatusFilter = ActivityStatusFilter.All,
): List<CatalogItem> {
    val terms = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter { it.isNotEmpty() }
    val filtered = items.filter { item ->
        currentCoroutineContext().ensureActive()
        (showSystem || !item.isSystem) &&
            (!isAppCatalog || !hideOverlays || !item.isOverlay) &&
            (isAppCatalog || (item.exported?.let(exportedFilter::matches) == true && enabledFilter.matches(item.enabled))) &&
            terms.all { it in item.searchText }
    }
    val comparator = if (isAppCatalog) {
        when (sortOrder) {
            CatalogSortOrder.Label -> compareBy<CatalogItem> { it.labelSortKey }.thenBy { it.packageName }
            CatalogSortOrder.PackageName -> compareBy<CatalogItem> { it.packageName.lowercase(Locale.ROOT) }.thenBy { it.label }
            CatalogSortOrder.FirstInstallTime -> compareByDescending<CatalogItem> { it.firstInstallTime }.thenBy { it.packageName }
        }.let { if (reverseOrder) it.reversed() else it }
    } else {
        when (activitySortOrder) {
            ActivitySortOrder.Label -> compareBy<CatalogItem> { it.labelSortKey }.thenBy { it.id }
            ActivitySortOrder.ClassName -> compareBy<CatalogItem> { it.id.lowercase(Locale.ROOT) }.thenBy { it.label }
        }
    }
    return filtered.sortedWith(comparator)
}
