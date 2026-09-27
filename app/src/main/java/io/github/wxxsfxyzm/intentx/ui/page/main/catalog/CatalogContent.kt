// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.catalog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.wxxsfxyzm.intentx.R
import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation
import io.github.wxxsfxyzm.intentx.ui.EmptyPanel
import io.github.wxxsfxyzm.intentx.ui.theme.bottomShape
import io.github.wxxsfxyzm.intentx.ui.theme.middleShape
import io.github.wxxsfxyzm.intentx.ui.theme.singleShape
import io.github.wxxsfxyzm.intentx.ui.theme.topShape
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun CatalogContent(
    state: CatalogViewState,
    onAction: (CatalogViewAction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    requestInstalledAppsPermission: (() -> Unit)? = null,
    icon: @Composable (CatalogItem) -> Unit,
) {
    var showLoading by remember(state.isLoading) { mutableStateOf(false) }
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) {
            delay(200.milliseconds)
            showLoading = true
        }
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = (if (state.packageName == null) 96.dp else 0.dp) + contentPadding.calculateBottomPadding(),
            ),
        ) {
            item(key = "loading") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                ) {
                    if (showLoading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
            if (requestInstalledAppsPermission != null) {
                item(key = "installed_apps_permission") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.catalog_permission_required))
                        TextButton(onClick = requestInstalledAppsPermission) {
                            Text(stringResource(R.string.catalog_grant_permission))
                        }
                    }
                }
            }
            if (state.loadFailed) {
                item(key = "error") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.catalog_load_failed))
                        TextButton(onClick = { onAction(CatalogViewAction.Reload) }) { Text(stringResource(R.string.catalog_retry)) }
                    }
                }
            }
            if (!state.isLoading && !state.loadFailed && state.items.isEmpty()) {
                item(key = "empty") {
                    EmptyPanel(
                        stringResource(R.string.catalog_no_matches),
                        stringResource(
                            when {
                                state.packageName == null -> R.string.catalog_filter_hint
                                state.componentTab == IntentOperation.Broadcast -> R.string.catalog_no_receivers
                                else -> R.string.catalog_no_components
                            },
                        ),
                        Modifier.fillMaxWidth(),
                    )
                }
            }
            itemsIndexed(
                state.items,
                key = { _, item -> item.listKey },
                contentType = { _, _ -> "catalog_entry" },
            ) { index, item ->
                val shape = when {
                    state.items.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == state.items.lastIndex -> bottomShape
                    else -> middleShape
                }
                val entryAnimation = remember(item.listKey) { Animatable(0f) }
                LaunchedEffect(item.listKey) {
                    entryAnimation.animateTo(1f, tween(durationMillis = 300))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = entryAnimation.value
                            translationY = 50f * (1f - entryAnimation.value)
                        }
                        .background(MaterialTheme.colorScheme.surfaceBright, shape)
                        .clip(shape)
                        .then(
                            if (state.packageName == null && item.exported == null) {
                                Modifier.clickable { onAction(CatalogViewAction.OpenApp(item.packageName)) }
                            } else {
                                Modifier.clickable {
                                    onAction(
                                        if (item.operation == IntentOperation.Broadcast) {
                                            CatalogViewAction.OpenReceiver(item.packageName, item.id, item.label)
                                        } else {
                                            CatalogViewAction.OpenActivity(item.packageName, item.id, item.label)
                                        },
                                    )
                                }
                            },
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (state.packageName == null && item.exported == null) 16.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.packageName == null && item.exported == null) icon(item)
                    Column(Modifier.weight(1f)) {
                        val textDecoration = if (item.exported == false) {
                            TextDecoration.LineThrough
                        } else {
                            null
                        }
                        Text(
                            item.label,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            textDecoration = textDecoration,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.exported != null || state.showPackageName) {
                            Text(
                                item.id,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = textDecoration,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (item.exported != null) {
                            val availability =
                                stringResource(if (item.enabled) R.string.catalog_enabled else R.string.catalog_disabled)
                            val componentStatus = stringResource(
                                R.string.catalog_component_status,
                                stringResource(if (item.exported) R.string.catalog_exported else R.string.catalog_not_exported),
                                availability,
                            )
                            val disabledColor = MaterialTheme.colorScheme.error
                            Text(
                                buildAnnotatedString {
                                    append(componentStatus)
                                    if (!item.enabled) {
                                        val start = componentStatus.lastIndexOf(availability)
                                        if (start >= 0) {
                                            addStyle(
                                                SpanStyle(color = disabledColor),
                                                start,
                                                start + availability.length,
                                            )
                                        }
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                            item.permission?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
