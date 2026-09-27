// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization

import io.github.wxxsfxyzm.intentx.executor.Authorizer

sealed interface AuthorizationViewAction {
    data object StartObserving : AuthorizationViewAction
    data object StopObserving : AuthorizationViewAction
    data class Select(val authorizer: Authorizer) : AuthorizationViewAction
}
