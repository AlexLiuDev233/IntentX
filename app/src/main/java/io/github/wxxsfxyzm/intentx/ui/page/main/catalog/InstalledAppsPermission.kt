// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.catalog

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** HyperOS/MIUI adds this runtime permission on top of QUERY_ALL_PACKAGES. */
internal object InstalledAppsPermission {
    const val NAME = "com.android.permission.GET_INSTALLED_APPS"
    private const val OWNER = "com.lbe.security.miui"

    fun isSupported(context: Context): Boolean = try {
        context.packageManager.getPermissionInfo(NAME, 0).packageName == OWNER
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun isMissing(context: Context): Boolean = isSupported(context) &&
        ContextCompat.checkSelfPermission(context, NAME) != PackageManager.PERMISSION_GRANTED
}
