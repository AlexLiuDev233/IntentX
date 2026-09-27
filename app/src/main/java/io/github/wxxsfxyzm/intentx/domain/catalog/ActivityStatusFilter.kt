// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.catalog

enum class ActivityStatusFilter {
    All,
    Yes,
    No,
    ;

    fun matches(value: Boolean): Boolean = when (this) {
        All -> true
        Yes -> value
        No -> !value
    }
}
