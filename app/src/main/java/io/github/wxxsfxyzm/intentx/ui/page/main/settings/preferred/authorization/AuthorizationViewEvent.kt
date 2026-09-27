// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization

import androidx.annotation.StringRes

sealed interface AuthorizationViewEvent {
    data class ShowMessage(@param:StringRes val message: Int) : AuthorizationViewEvent
}
