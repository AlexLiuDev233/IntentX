// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
// Copyright (C) 2023-2026 iamr0s
package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.wxxsfxyzm.intentx.BuildConfig
import io.github.wxxsfxyzm.intentx.R

@Composable
internal fun AboutHeader(appIcon: ImageBitmap?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (appIcon != null) {
            Image(appIcon, stringResource(R.string.app_name), Modifier.size(80.dp))
        } else {
            Box(Modifier.size(80.dp))
        }
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.app_version_info_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
