// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.settings.repository

import io.github.wxxsfxyzm.intentx.domain.catalog.ActivitySortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityStatusFilter
import io.github.wxxsfxyzm.intentx.domain.catalog.CatalogSortOrder
import io.github.wxxsfxyzm.intentx.executor.Authorizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface AppSettingsRepository {
    val authorizer: Flow<Authorizer>
    val showSystemApps: Flow<Boolean>
    val catalogSortOrder: Flow<CatalogSortOrder>
    val catalogReverseOrder: Flow<Boolean>
    val catalogShowPackageName: Flow<Boolean>
    val catalogSearchActivities: Flow<Boolean>
        get() = flowOf(false)
    val catalogHideOverlays: Flow<Boolean>
        get() = flowOf(false)
    val activitySortOrder: Flow<ActivitySortOrder>
    val activityExportedFilter: Flow<ActivityStatusFilter>
    val activityEnabledFilter: Flow<ActivityStatusFilter>
    suspend fun setAuthorizer(authorizer: Authorizer)
    suspend fun setShowSystemApps(show: Boolean)
    suspend fun setCatalogSortOrder(order: CatalogSortOrder)
    suspend fun setCatalogReverseOrder(reverse: Boolean)
    suspend fun setCatalogShowPackageName(show: Boolean)
    suspend fun setCatalogSearchActivities(search: Boolean) = Unit
    suspend fun setCatalogHideOverlays(hide: Boolean) = Unit
    suspend fun setActivitySortOrder(order: ActivitySortOrder)
    suspend fun setActivityExportedFilter(filter: ActivityStatusFilter)
    suspend fun setActivityEnabledFilter(filter: ActivityStatusFilter)
}
