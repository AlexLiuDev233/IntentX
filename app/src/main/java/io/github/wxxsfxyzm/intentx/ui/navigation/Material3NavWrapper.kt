// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
// Copyright (C) 2023-2026 iamr0s
package io.github.wxxsfxyzm.intentx.ui.navigation

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wxxsfxyzm.intentx.R
import io.github.wxxsfxyzm.intentx.domain.settings.model.preferences.ThemeState
import io.github.wxxsfxyzm.intentx.ui.icons.AppIcons
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.Material3SettingsCompactLayout
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.Material3SettingsWideScreenLayout
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.SettingsSharedViewModel
import io.github.wxxsfxyzm.intentx.ui.theme.LocalWindowLayoutInfo
import io.github.wxxsfxyzm.intentx.ui.theme.rememberMaterial3BlurBackdrop

@Immutable
data class NavigationTab(val icon: ImageVector, val label: String)

@Composable
fun Material3MainPageWrapper(uiState: ThemeState, sharedViewModel: SettingsSharedViewModel) {
    val sharedState by sharedViewModel.state.collectAsStateWithLifecycle()
    val useBlur = uiState.useBlur
    val useFloatingBottomBar = uiState.useAppleFloatingBar
    val backdrop = rememberMaterial3BlurBackdrop(useBlur)

    val configCount = 0
    val homeLabel = stringResource(id = R.string.apps)
    val configLabel = stringResource(R.string.saved)
    val preferredLabel = stringResource(R.string.preferred)

    val tabs = remember(homeLabel, configLabel, preferredLabel) {
        listOf(
            NavigationTab(
                icon = AppIcons.Catalog,
                label = homeLabel,
            ),
            NavigationTab(
                icon = AppIcons.SavedIntents,
                label = configLabel,
            ),
            NavigationTab(
                icon = AppIcons.SettingsSuggest,
                label = preferredLabel,
            ),
        )
    }

    val pagerState = rememberPagerState(
        initialPage = sharedState.lastMainPageIndex.coerceIn(0, tabs.lastIndex),
        pageCount = { tabs.size },
    )
    val mainPagerState = rememberMainPagerState(pagerState)
    val currentPage = mainPagerState.pagerState.currentPage
    val settledPage = mainPagerState.pagerState.settledPage
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(currentPage) {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        mainPagerState.syncPage()
    }
    LaunchedEffect(settledPage) {
        if (sharedState.lastMainPageIndex != settledPage) {
            sharedViewModel.updateLastMainPageIndex(settledPage)
        }
    }
    MainScreenBackHandler(
        mainPagerState = mainPagerState,
        navController = LocalNavigator.current,
    )

    val layoutInfo = LocalWindowLayoutInfo.current
    val showRail = layoutInfo.showNavigationRail
    val isMedium = layoutInfo.isMediumPortrait

    // Branch statically without layout delay traps
    if (showRail) {
        Material3SettingsWideScreenLayout(
            configCount = configCount,
            mainPagerState = mainPagerState,
            tabs = tabs,
            useBlur = useBlur,
            useFloatingBottomBar = useFloatingBottomBar,
            backdrop = backdrop,
        )
    } else {
        Material3SettingsCompactLayout(
            configCount = configCount,
            mainPagerState = mainPagerState,
            tabs = tabs,
            useBlur = useBlur,
            useFloatingBottomBar = useFloatingBottomBar,
            backdrop = backdrop,
            isMedium = isMedium,
        )
    }
}
