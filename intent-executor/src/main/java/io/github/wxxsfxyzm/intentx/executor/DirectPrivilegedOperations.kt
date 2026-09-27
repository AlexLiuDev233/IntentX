// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.executor

import android.app.AppOpsManager
import android.app.IActivityManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Process
import com.android.internal.app.IAppOpsService
import rikka.shizuku.SystemServiceHelper

/** Binder operations shared by normal and privileged execution paths. */
class DirectPrivilegedOperations private constructor(
    private val context: Context,
    private val wrap: ((IBinder) -> IBinder)?,
) {
    companion object {
        fun normal(context: Context) = DirectPrivilegedOperations(context, null)

        fun wrapped(context: Context, wrap: (IBinder) -> IBinder) = DirectPrivilegedOperations(context, wrap)
    }

    private fun activityManager(): IActivityManager {
        val original = checkNotNull(SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE))
        return IActivityManager.Stub.asInterface(checkNotNull(wrap)(original))
    }

    fun checkConnection() {
        if (wrap != null) check(activityManager().asBinder().pingBinder()) { "Binder connection lost" }
    }

    /** Grant only this app's OEM shortcut operation, under the selected Binder identity. */
    fun allowShortcutAppOp(operation: Int) {
        val wrapper = checkNotNull(wrap) { "Shortcut AppOps requires privileged authorization" }
        val binder = checkNotNull(SystemServiceHelper.getSystemService(Context.APP_OPS_SERVICE))
        IAppOpsService.Stub.asInterface(wrapper(binder)).setMode(
            operation,
            context.applicationInfo.uid,
            context.packageName,
            AppOpsManager.MODE_ALLOWED,
        )
    }

    fun startActivity(intent: Intent): Int {
        if (wrap == null) {
            require(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
                "Normal launch requires ACTIVITY_NEW_TASK"
            }
            context.startActivity(Intent(intent))
            return 0
        }
        return activityManager().startActivityAsUser(
            null,
            "com.android.shell",
            Intent(intent),
            intent.resolveType(context.contentResolver),
            null,
            null,
            0,
            0,
            null,
            null,
            Process.myUid() / 100000,
        )
    }

    fun sendBroadcast(intent: Intent): Boolean {
        if (wrap == null) {
            context.sendBroadcast(Intent(intent))
            return true
        }
        return activityManager().broadcastIntent(
            null,
            Intent(intent),
            intent.resolveType(context.contentResolver),
            null,
            0,
            null,
            null,
            null,
            -1,
            null,
            false,
            false,
            Process.myUid() / 100000,
        ) >= 0
    }
}
