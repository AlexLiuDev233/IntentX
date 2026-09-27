// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
// OEM permission handling and pinned-shortcut update flow adapted from KernelSU manager.
package io.github.wxxsfxyzm.intentx.framework.shortcut

import android.content.ComponentName
import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import io.github.wxxsfxyzm.intentx.MainActivity
import io.github.wxxsfxyzm.intentx.domain.shortcut.IntentShortcut
import io.github.wxxsfxyzm.intentx.domain.shortcut.ShortcutCreator
import io.github.wxxsfxyzm.intentx.domain.shortcut.ShortcutRepository
import io.github.wxxsfxyzm.intentx.domain.shortcut.ShortcutResult
import io.github.wxxsfxyzm.intentx.executor.DirectPrivilegedExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AndroidShortcutCreator(
    private val context: Context,
    private val repository: ShortcutRepository,
    executor: DirectPrivilegedExecutor,
) : ShortcutCreator {
    private val permission = OemShortcutPermission(context, executor)
    private val icons = ShortcutIconLoader(context)

    override suspend fun pin(shortcut: IntentShortcut, iconUri: String?): ShortcutResult {
        Timber.d("Preparing shortcut: id=%s, operation=%s, authorizer=%s, customIcon=%s", shortcut.id, shortcut.operation, shortcut.authorizer, !iconUri.isNullOrBlank())
        val launchShortcut = shortcut.copy(token = repository.get(shortcut.id)?.token ?: shortcut.token)
        val info = withContext(Dispatchers.IO) {
            ShortcutInfoCompat.Builder(context, shortcut.id)
                .setShortLabel(shortcut.name)
                .setActivity(ComponentName(context, MainActivity::class.java))
                .setIntent(ShortcutActivity.createIntent(context, launchShortcut))
                .setIcon(icons.load(iconUri, shortcut.intent.packageName))
                .build()
        }
        val hasPinned = withContext(Dispatchers.IO) {
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
                .any { it.id == shortcut.id && it.isEnabled }
        }
        if (hasPinned) {
            Timber.d("Existing pinned shortcut found; updating: id=%s", shortcut.id)
            repository.upsert(launchShortcut)
            return withContext(Dispatchers.Main.immediate) {
                val updated = ShortcutManagerCompat.updateShortcuts(context, listOf(info))
                Timber.d("Pinned shortcut update accepted=%s", updated)
                if (updated) ShortcutResult.Updated else ShortcutResult.Failed
            }
        }
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Timber.w("Launcher does not support pinned shortcuts")
            return ShortcutResult.Unsupported
        }
        var state = permission.check()
        if (state == ShortcutPermissionState.Denied || state == ShortcutPermissionState.Ask) {
            if (permission.tryAllow(shortcut.authorizer)) state = permission.check()
            if (state == ShortcutPermissionState.Denied || state == ShortcutPermissionState.Ask) {
                Timber.w("Shortcut creation blocked by OEM permission: %s", state)
                return permissionHint()
            }
        }
        // Persist before requesting: the launcher can immediately invoke the exported entry point.
        repository.upsert(launchShortcut)
        return withContext(Dispatchers.Main.immediate) {
            val accepted = ShortcutManagerCompat.requestPinShortcut(context, info, null)
            Timber.d("Launcher pin request accepted=%s", accepted)
            if (accepted) {
                // True means the launcher accepted the request, not that the user confirmed it.
                ShortcutResult.Requested
            } else {
                permissionHint()
            }
        }
    }

    private fun permissionHint() = when (permission.oem) {
        ShortcutOem.Xiaomi -> ShortcutResult.XiaomiPermissionRequired
        ShortcutOem.ColorOs -> ShortcutResult.ColorOsPermissionRequired
        ShortcutOem.Other -> ShortcutResult.PermissionRequired
    }
}
