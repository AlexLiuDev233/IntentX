// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.about

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.wxxsfxyzm.intentx.R
import io.github.wxxsfxyzm.intentx.ui.icons.AppIcons
import io.github.wxxsfxyzm.intentx.ui.navigation.LocalNavigator
import io.github.wxxsfxyzm.intentx.ui.navigation.Route
import io.github.wxxsfxyzm.intentx.ui.page.main.widget.setting.ExpressiveBackButton
import io.github.wxxsfxyzm.intentx.ui.page.main.widget.setting.NavigationItemWidget
import io.github.wxxsfxyzm.intentx.ui.page.main.widget.setting.SegmentedColumn
import io.github.wxxsfxyzm.intentx.ui.theme.getMaterial3AppBarColor
import io.github.wxxsfxyzm.intentx.ui.theme.installerMaterial3BlurEffect
import io.github.wxxsfxyzm.intentx.ui.theme.rememberMaterial3BlurBackdrop
import io.github.wxxsfxyzm.intentx.ui.util.CollectUiEvents
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
fun AboutPage(useBlur: Boolean, viewModel: AboutViewModel = koinViewModel()) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    CollectUiEvents(viewModel.eventFlow) { event ->
        when (event) {
            AboutViewEvent.OpenLicenses -> navigator.push(Route.OpenSourceLicense)

            is AboutViewEvent.OpenUrl -> try {
                uriHandler.openUri(event.url)
            } catch (error: IllegalArgumentException) {
                Timber.w(error, "Unable to open About link")
                Toast.makeText(context, R.string.about_open_link_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    AboutContent(useBlur, viewModel::dispatch, { navigator.pop() })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutContent(
    useBlur: Boolean,
    onAction: (AboutViewAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val backdrop = rememberMaterial3BlurBackdrop(useBlur)
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                modifier = Modifier.installerMaterial3BlurEffect(backdrop),
                windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
                title = { Text(stringResource(R.string.about)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    Row {
                        ExpressiveBackButton(onClick = onBack)
                        Spacer(Modifier.size(16.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backdrop.getMaterial3AppBarColor(),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    scrolledContainerColor = backdrop.getMaterial3AppBarColor(),
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
            contentPadding = padding,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "version") {
                AboutHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(top = 32.dp, bottom = 36.dp),
                )
            }
            item(key = "about") {
                SegmentedColumn(title = stringResource(R.string.about)) {
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.ViewSourceCode,
                            title = stringResource(R.string.get_source_code),
                            description = stringResource(R.string.get_source_code_detail),
                            onClick = { onAction(AboutViewAction.OpenSource) },
                        )
                    }
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.OpenSourceLicense,
                            title = stringResource(R.string.open_source_license),
                            description = stringResource(R.string.open_source_license_settings_description),
                            onClick = { onAction(AboutViewAction.OpenLicenses) },
                        )
                    }
                    item {
                        NavigationItemWidget(
                            icon = AppIcons.Update,
                            title = stringResource(R.string.get_update),
                            description = stringResource(R.string.get_update_detail),
                            onClick = { onAction(AboutViewAction.OpenReleases) },
                        )
                    }
                }
            }
        }
    }
}
