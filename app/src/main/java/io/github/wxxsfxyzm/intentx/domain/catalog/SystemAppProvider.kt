// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.domain.catalog

import kotlinx.coroutines.flow.Flow

interface SystemAppProvider {
    val packageChanges: Flow<Unit>
    suspend fun getInstalledApps(): List<InstalledAppTarget>
    suspend fun getActivities(packageName: String): List<ActivityTarget>
    suspend fun getReceivers(packageName: String): List<ReceiverTarget> = emptyList()

    /** Null when the component cannot be inspected; the platform remains the launch authority. */
    suspend fun isActivityEnabled(packageName: String, className: String): Boolean? = null
    suspend fun isReceiverEnabled(packageName: String, className: String): Boolean? = null
    suspend fun getAllActivities(): List<ActivityTarget> = emptyList()
}
