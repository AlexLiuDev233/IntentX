// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.data.settings

import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.emptyPreferences
import io.github.wxxsfxyzm.intentx.data.settings.local.datastore.AppDataStore
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.PredictiveBackAnimation
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.PredictiveBackExitDirection
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.ThemeState
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme.PaletteStyle
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme.ThemeColorSpec
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.theme.ThemeMode
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.theme.ThemeSettingsAction
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.theme.ThemeSettingsState
import io.github.wxxsfxyzm.intentx.ui.theme.material.PresetColors
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

/** Maps the shared AppDataStore preferences to the transplanted theme UI. */
class ThemePreferences(private val store: AppDataStore) {
    val settings: Flow<ThemeSettingsState> = store.data
        .catch {
            if (it is IOException) {
                Timber.e(it, "Unable to read appearance settings; using defaults")
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { prefs ->
            val dynamic = prefs[AppDataStore.THEME_USE_DYNAMIC_COLOR] ?: true
            val manual = prefs[AppDataStore.THEME_SEED_COLOR] ?: PresetColors[0].color.toArgb()
            val available = PresetColors
            val seed = available.firstOrNull { it.color.toArgb() == manual }?.color ?: available[0].color
            ThemeSettingsState(
                useBlur = prefs[AppDataStore.UI_USE_BLUR] ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU),
                themeMode = ThemeMode.fromValueOrDefault(prefs[AppDataStore.THEME_MODE].orEmpty()),
                usePureBlack = prefs[AppDataStore.THEME_USE_PURE_BLACK] ?: false,
                paletteStyle = PaletteStyle.fromValueOrDefault(prefs[AppDataStore.THEME_PALETTE_STYLE].orEmpty()),
                colorSpec = ThemeColorSpec.fromValueOrDefault(prefs[AppDataStore.THEME_COLOR_SPEC].orEmpty()),
                useDynamicColor = dynamic,
                useAppleFloatingBar = prefs[AppDataStore.UI_USE_APPLE_FLOATING_BAR] ?: false,
                seedColor = seed,
                availableColors = available,
                predictiveBackAnimation = PredictiveBackAnimation.fromValueOrDefault(prefs[AppDataStore.PREDICTIVE_BACK_ANIMATION].orEmpty()),
                predictiveBackExitDirection = PredictiveBackExitDirection.fromValueOrDefault(prefs[AppDataStore.PREDICTIVE_BACK_EXIT_DIRECTION].orEmpty()),
            )
        }

    val themeStateFlow: Flow<ThemeState> = settings.map {
        ThemeState(
            isLoaded = true, themeMode = it.themeMode, paletteStyle = it.paletteStyle,
            usePureBlack = it.usePureBlack,
            colorSpec = it.colorSpec, useDynamicColor = it.useDynamicColor,
            seedColor = it.seedColor.toArgb(), useBlur = it.useBlur,
            useAppleFloatingBar = it.useAppleFloatingBar,
            predictiveBackAnimation = it.predictiveBackAnimation,
            predictiveBackExitDirection = it.predictiveBackExitDirection,
        )
    }

    suspend fun update(action: ThemeSettingsAction) {
        store.edit { prefs ->
            when (action) {
                is ThemeSettingsAction.SetUseBlur -> prefs[AppDataStore.UI_USE_BLUR] = action.enable

                is ThemeSettingsAction.SetThemeMode -> prefs[AppDataStore.THEME_MODE] = action.mode.name

                is ThemeSettingsAction.SetUsePureBlack -> prefs[AppDataStore.THEME_USE_PURE_BLACK] = action.use

                is ThemeSettingsAction.SetPaletteStyle -> prefs[AppDataStore.THEME_PALETTE_STYLE] = action.style.name

                is ThemeSettingsAction.SetColorSpec -> prefs[AppDataStore.THEME_COLOR_SPEC] = action.spec.name

                is ThemeSettingsAction.SetUseDynamicColor -> {
                    prefs[AppDataStore.THEME_USE_DYNAMIC_COLOR] = action.use
                    prefs.remove(AppDataStore.THEME_SEED_COLOR)
                }

                is ThemeSettingsAction.SetUseAppleFloatingBar -> prefs[AppDataStore.UI_USE_APPLE_FLOATING_BAR] = action.use

                is ThemeSettingsAction.SetSeedColor -> prefs[AppDataStore.THEME_SEED_COLOR] = action.color.toArgb()

                is ThemeSettingsAction.SetPredictiveBackAnimation -> prefs[AppDataStore.PREDICTIVE_BACK_ANIMATION] =
                    action.animation.value

                is ThemeSettingsAction.SetPredictiveBackExitDirection -> prefs[AppDataStore.PREDICTIVE_BACK_EXIT_DIRECTION] =
                    action.direction.value
            }
        }
        Timber.d("Appearance setting saved: %s", action.javaClass.simpleName)
    }
}
