// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.domain.catalog

data class InstalledAppTarget(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean,
    val isOverlay: Boolean = false,
)

data class ActivityTarget(
    val packageName: String,
    val className: String,
    val label: String,
    val exported: Boolean,
    val enabled: Boolean,
    val permission: String?,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean = false,
    val isOverlay: Boolean = false,
)

data class ReceiverTarget(
    val packageName: String,
    val className: String,
    val label: String,
    val exported: Boolean,
    val enabled: Boolean,
    val permission: String?,
    val lastUpdateTime: Long,
)
