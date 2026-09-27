// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
// Adapted from KernelSU manager's ui/util/module/Shortcut.kt and OemHelper.kt.
package io.github.wxxsfxyzm.intentx.framework.shortcut

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import androidx.core.net.toUri
import io.github.wxxsfxyzm.intentx.executor.Authorizer
import io.github.wxxsfxyzm.intentx.executor.DirectPrivilegedExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal enum class ShortcutPermissionState { Granted, Denied, Ask, Unknown }
internal enum class ShortcutOem { Xiaomi, ColorOs, Other }

internal class OemShortcutPermission(
    private val context: Context,
    private val executor: DirectPrivilegedExecutor,
) {
    companion object {
        // Xiaomi's private operation; never use it on other OEMs.
        private const val XIAOMI_SHORTCUT_OP = 10017
        private const val MODE_ASK = 5
    }

    val oem: ShortcutOem by lazy {
        when {
            property("ro.miui.ui.version.name").isNotEmpty() || property("ro.mi.os.version.name").isNotEmpty() -> ShortcutOem.Xiaomi
            property("ro.build.version.oplus.api").isNotEmpty() || property("ro.vendor.oplus.market.name").isNotEmpty() -> ShortcutOem.ColorOs
            else -> ShortcutOem.Other
        }
    }

    suspend fun check(): ShortcutPermissionState = withContext(Dispatchers.IO) {
        try {
            val state = when (oem) {
                ShortcutOem.Xiaomi -> checkXiaomi()
                ShortcutOem.ColorOs -> checkColorOs()
                ShortcutOem.Other -> ShortcutPermissionState.Unknown
            }
            Timber.d("OEM shortcut permission: oem=%s, state=%s", oem, state)
            state
        } catch (error: Exception) {
            Timber.w(error, "OEM shortcut permission query failed: oem=%s", oem)
            // OEM APIs may be absent or inaccessible to an ordinary app. Unknown is not denial.
            ShortcutPermissionState.Unknown
        }
    }

    suspend fun tryAllow(authorizer: Authorizer): Boolean {
        if (oem != ShortcutOem.Xiaomi || authorizer == Authorizer.None) return false
        return try {
            Timber.d("Attempting Xiaomi shortcut AppOps grant: authorizer=%s", authorizer)
            executor.useDirectPrivileged(authorizer) { it.allowShortcutAppOp(XIAOMI_SHORTCUT_OP) }
            Timber.d("Xiaomi shortcut AppOps grant submitted")
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "Xiaomi shortcut AppOps grant failed: authorizer=%s", authorizer)
            // Shizuku may run as shell or root; the actual Binder permission decides.
            false
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun checkXiaomi(): ShortcutPermissionState {
        val manager = context.getSystemService(AppOpsManager::class.java)
        val method = AppOpsManager::class.java.getDeclaredMethod(
            "checkOpNoThrow",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        return when (method.invoke(manager, XIAOMI_SHORTCUT_OP, context.applicationInfo.uid, context.packageName) as? Int) {
            AppOpsManager.MODE_ALLOWED -> ShortcutPermissionState.Granted
            AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> ShortcutPermissionState.Denied
            MODE_ASK -> ShortcutPermissionState.Ask
            else -> ShortcutPermissionState.Unknown
        }
    }

    private fun checkColorOs(): ShortcutPermissionState {
        val uri = "content://settings/secure/launcher_shortcut_permission_settings".toUri()
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex("value")
            if (index < 0) return ShortcutPermissionState.Unknown
            val granted = "${context.packageName}, 1"
            val denied = "${context.packageName}, 0"
            while (cursor.moveToNext()) {
                val value = cursor.getString(index).orEmpty()
                if (value.contains(granted)) return ShortcutPermissionState.Granted
                if (value.contains(denied)) return ShortcutPermissionState.Denied
            }
        }
        return ShortcutPermissionState.Unknown
    }

    @SuppressLint("PrivateApi")
    private fun property(key: String): String = try {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, key) as? String ?: ""
    } catch (error: Exception) {
        Timber.d(error, "OEM system property unavailable: %s", key)
        ""
    }
}
