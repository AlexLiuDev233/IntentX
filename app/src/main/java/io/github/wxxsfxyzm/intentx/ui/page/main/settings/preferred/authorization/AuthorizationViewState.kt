// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization

import io.github.wxxsfxyzm.intentx.domain.authorization.AuthorizationStatus
import io.github.wxxsfxyzm.intentx.domain.authorization.RootMode
import io.github.wxxsfxyzm.intentx.domain.authorization.ShizukuState
import io.github.wxxsfxyzm.intentx.executor.Authorizer

data class AuthorizationViewState(
    val selected: Authorizer = Authorizer.None,
    val rootStatus: AuthorizationStatus = AuthorizationStatus.Checking,
    val rootMode: RootMode? = null,
    val shizukuState: ShizukuState = ShizukuState(AuthorizationStatus.Unavailable),
)
