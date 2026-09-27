// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.authorization

import kotlinx.coroutines.flow.Flow

enum class AuthorizationStatus { Checking, Ready, Unavailable, PermissionRequired }

enum class RootMode { Magisk, KernelSU, APatch, Other }

enum class ShizukuMode { Root, Shell, Unknown }

data class ShizukuState(
    val status: AuthorizationStatus,
    val mode: ShizukuMode = ShizukuMode.Unknown,
)

interface AuthorizationStatusProvider {
    val shizukuState: Flow<ShizukuState>
    suspend fun checkRoot(): RootMode
    fun requestShizukuPermission()
}
