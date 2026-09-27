// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.theme

import androidx.compose.ui.graphics.Color
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.PredictiveBackAnimation
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.PredictiveBackExitDirection
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme.PaletteStyle
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme.ThemeColorSpec
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme.ThemeMode

sealed interface ThemeSettingsAction {
    data class SetUseBlur(val enable: Boolean) : ThemeSettingsAction
    data class SetThemeMode(val mode: ThemeMode) : ThemeSettingsAction
    data class SetPaletteStyle(val style: PaletteStyle) : ThemeSettingsAction
    data class SetColorSpec(val spec: ThemeColorSpec) : ThemeSettingsAction
    data class SetUseDynamicColor(val use: Boolean) : ThemeSettingsAction
    data class SetUseAppleFloatingBar(val use: Boolean) : ThemeSettingsAction
    data class SetSeedColor(val color: Color) : ThemeSettingsAction
    data class SetPredictiveBackAnimation(val animation: PredictiveBackAnimation) : ThemeSettingsAction
    data class SetPredictiveBackExitDirection(val direction: PredictiveBackExitDirection) : ThemeSettingsAction
}
