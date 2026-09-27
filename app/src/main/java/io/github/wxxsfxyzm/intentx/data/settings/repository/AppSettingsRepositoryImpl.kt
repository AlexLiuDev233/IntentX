// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.data.settings.repository

import io.github.wxxsfxyzm.intentx.data.settings.local.datastore.AppDataStore
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivitySortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityStatusFilter
import io.github.wxxsfxyzm.intentx.domain.catalog.CatalogSortOrder
import io.github.wxxsfxyzm.intentx.domain.settings.repository.AppSettingsRepository
import io.github.wxxsfxyzm.intentx.executor.Authorizer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AppSettingsRepositoryImpl(private val store: AppDataStore) : AppSettingsRepository {
    override val authorizer = store.getString(AppDataStore.AUTHORIZER).map { stored ->
        Authorizer.entries.firstOrNull { it.name == stored } ?: Authorizer.None
    }.distinctUntilChanged()
    override val showSystemApps = store.getBoolean(AppDataStore.SHOW_SYSTEM_APPS).distinctUntilChanged()
    override val catalogSortOrder = store.getString(AppDataStore.CATALOG_SORT_ORDER).map { stored ->
        CatalogSortOrder.entries.firstOrNull { it.name == stored } ?: CatalogSortOrder.Label
    }.distinctUntilChanged()
    override val catalogReverseOrder = store.getBoolean(AppDataStore.CATALOG_REVERSE_ORDER).distinctUntilChanged()
    override val catalogShowPackageName = store.getBoolean(AppDataStore.CATALOG_SHOW_PACKAGE_NAME, default = true).distinctUntilChanged()
    override val catalogSearchActivities = store.getBoolean(AppDataStore.CATALOG_SEARCH_ACTIVITIES).distinctUntilChanged()
    override val catalogHideOverlays = store.getBoolean(AppDataStore.CATALOG_HIDE_OVERLAYS).distinctUntilChanged()
    override val activitySortOrder = store.getString(AppDataStore.ACTIVITY_SORT_ORDER).map { stored ->
        ActivitySortOrder.entries.firstOrNull { it.name == stored } ?: ActivitySortOrder.Label
    }.distinctUntilChanged()
    override val activityExportedFilter = store.getString(AppDataStore.ACTIVITY_EXPORTED_FILTER).map { stored ->
        ActivityStatusFilter.entries.firstOrNull { it.name == stored } ?: ActivityStatusFilter.All
    }.distinctUntilChanged()
    override val activityEnabledFilter = store.getString(AppDataStore.ACTIVITY_ENABLED_FILTER).map { stored ->
        ActivityStatusFilter.entries.firstOrNull { it.name == stored } ?: ActivityStatusFilter.All
    }.distinctUntilChanged()

    override suspend fun setAuthorizer(authorizer: Authorizer) = store.putString(AppDataStore.AUTHORIZER, authorizer.name)

    override suspend fun setShowSystemApps(show: Boolean) = store.putBoolean(AppDataStore.SHOW_SYSTEM_APPS, show)
    override suspend fun setCatalogSortOrder(order: CatalogSortOrder) = store.putString(AppDataStore.CATALOG_SORT_ORDER, order.name)
    override suspend fun setCatalogReverseOrder(reverse: Boolean) = store.putBoolean(AppDataStore.CATALOG_REVERSE_ORDER, reverse)
    override suspend fun setCatalogShowPackageName(show: Boolean) = store.putBoolean(AppDataStore.CATALOG_SHOW_PACKAGE_NAME, show)
    override suspend fun setCatalogSearchActivities(search: Boolean) = store.putBoolean(AppDataStore.CATALOG_SEARCH_ACTIVITIES, search)
    override suspend fun setCatalogHideOverlays(hide: Boolean) = store.putBoolean(AppDataStore.CATALOG_HIDE_OVERLAYS, hide)
    override suspend fun setActivitySortOrder(order: ActivitySortOrder) = store.putString(AppDataStore.ACTIVITY_SORT_ORDER, order.name)
    override suspend fun setActivityExportedFilter(filter: ActivityStatusFilter) = store.putString(AppDataStore.ACTIVITY_EXPORTED_FILTER, filter.name)
    override suspend fun setActivityEnabledFilter(filter: ActivityStatusFilter) = store.putString(AppDataStore.ACTIVITY_ENABLED_FILTER, filter.name)
}
