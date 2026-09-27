// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme

enum class ThemeColorSpec(val displayName: String) {
    SPEC_2021("Material 3 (2021)"),
    SPEC_2025("Expressive (2025)"),
    ;

    companion object {
        fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: SPEC_2025
    }
}
