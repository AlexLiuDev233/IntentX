// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.PredictiveBackAnimation
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.PredictiveBackExitDirection
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme.PaletteStyle
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme.ThemeColorSpec
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme.ThemeMode
import io.github.wxxsfxyzm.intentx.ui.theme.material.PresetColors
import io.github.wxxsfxyzm.intentx.ui.theme.material.RawColor

data class ThemeSettingsState(
    val showMiuixUI: Boolean = false,
    val useBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val usePureBlack: Boolean = false,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useMiuixMonet: Boolean = false,
    val useAppleFloatingBar: Boolean = false,
    val seedColor: Color = PresetColors[0].color,
    val availableColors: List<RawColor> = PresetColors,
    val useDynColorFollowPkgIcon: Boolean = false,
    val useDynColorFollowPkgIconForLiveActivity: Boolean = false,
    val preferSystemIcon: Boolean = false,
    val showLiveActivity: Boolean = false,
    val predictiveBackAnimation: PredictiveBackAnimation = PredictiveBackAnimation.None,
    val predictiveBackExitDirection: PredictiveBackExitDirection = PredictiveBackExitDirection.FOLLOW_GESTURE,
)
