// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.ThemeState
import io.github.wxxsfxyzm.intentx.ui.navigation.InstallerNavContainer
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization.AuthorizationViewAction
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization.AuthorizationViewState
import io.github.wxxsfxyzm.intentx.ui.theme.InstallerTheme
import io.github.wxxsfxyzm.intentx.ui.theme.LocalWindowLayoutInfo
import io.github.wxxsfxyzm.intentx.ui.theme.rememberWindowLayoutInfo

data class AuthorizationUi(
    val state: AuthorizationViewState,
    val dispatch: (AuthorizationViewAction) -> Unit,
)

val LocalAuthorizationUi = staticCompositionLocalOf<AuthorizationUi> { error("Authorization UI missing") }

@Composable
fun IntentXRoot(
    state: ThemeState,
    authorization: AuthorizationUi,
    onThemeLoaded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoaded) return
    SideEffect { onThemeLoaded() }
    CompositionLocalProvider(
        LocalWindowLayoutInfo provides rememberWindowLayoutInfo(),
        LocalAuthorizationUi provides authorization,
    ) {
        InstallerTheme(
            useMiuix = false,
            themeMode = state.themeMode,
            usePureBlack = state.usePureBlack,
            paletteStyle = state.paletteStyle,
            colorSpec = state.colorSpec,
            useDynamicColor = state.useDynamicColor,
            useMiuixMonet = false,
            seedColor = Color(state.seedColor),
        ) {
            Box(
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                InstallerNavContainer(state)
            }
        }
    }
}
