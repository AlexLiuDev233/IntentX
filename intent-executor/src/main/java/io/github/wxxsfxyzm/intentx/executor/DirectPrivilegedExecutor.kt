// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.github.wxxsfxyzm.app_process.AppProcess
import io.github.wxxsfxyzm.intentx.executor.connection.AuthorizerConnectionSelector
import io.github.wxxsfxyzm.intentx.executor.connection.PrivilegedConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import timber.log.Timber

/**
 * The action runs locally against wrapped framework binders, never a UserService.
 * Root's app-process bridge is scoped to this call and closed even when the action fails.
 */
class DirectPrivilegedExecutor(private val context: Context) : IntentExecutor {
    init {
        HiddenApiBypass.addHiddenApiExemptions("")
    }

    suspend fun <T> useDirectPrivileged(
        authorizer: Authorizer,
        action: (DirectPrivilegedOperations) -> T,
    ): T = runInterruptible(Dispatchers.IO) {
        AuthorizerConnectionSelector.connect(authorizer) { candidate ->
            openConnection(candidate, automatic = authorizer == Authorizer.Auto)
        }.use { connection ->
            if (Thread.currentThread().isInterrupted) throw InterruptedException("Privileged operation interrupted")
            Timber.d("Using authorizer: requested=%s, selected=%s", authorizer, connection.authorizer)
            action(connection.operations)
        }
    }

    private fun openConnection(authorizer: Authorizer, automatic: Boolean): PrivilegedConnection {
        val connection = when (authorizer) {
            Authorizer.None -> PrivilegedConnection(authorizer, DirectPrivilegedOperations.normal(context))

            Authorizer.Root -> {
                val process = AppProcess.Root()
                var initialized = false
                try {
                    check(process.init(context)) { "Root authorization or Binder connection failed" }
                    PrivilegedConnection(authorizer, DirectPrivilegedOperations.wrapped(context, process::binderWrapper), process::close)
                        .also { initialized = true }
                } finally {
                    if (!initialized) process.close()
                }
            }

            Authorizer.Shizuku -> {
                awaitShizukuBinder(if (automatic) AUTO_SHIZUKU_WAIT_SECONDS else SHIZUKU_WAIT_SECONDS)
                check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { "Shizuku permission is required" }
                val uid = Shizuku.getUid()
                if (automatic) check(uid == 0 || uid == 2000) { "Unknown Shizuku identity: $uid" }
                Timber.d("Shizuku connection identity: uid=%d", uid)
                PrivilegedConnection(authorizer, DirectPrivilegedOperations.wrapped(context, ::ShizukuBinderWrapper))
            }

            Authorizer.Auto -> error("Auto must be resolved before opening a connection")
        }
        var ready = false
        try {
            connection.operations.checkConnection()
            ready = true
            return connection
        } finally {
            if (!ready) connection.close()
        }
    }

    private fun awaitShizukuBinder(timeoutSeconds: Long) {
        if (Shizuku.pingBinder()) return
        val received = CountDownLatch(1)
        val listener = Shizuku.OnBinderReceivedListener { received.countDown() }
        try {
            Shizuku.addBinderReceivedListenerSticky(listener)
            check(received.await(timeoutSeconds, TimeUnit.SECONDS) && Shizuku.pingBinder()) { "Shizuku is not running" }
        } finally {
            Shizuku.removeBinderReceivedListener(listener)
        }
    }

    private companion object {
        const val AUTO_SHIZUKU_WAIT_SECONDS = 1L
        const val SHIZUKU_WAIT_SECONDS = 5L
    }

    suspend fun check(authorizer: Authorizer) = useDirectPrivileged(authorizer) { it.checkConnection() }

    override suspend fun startActivity(authorizer: Authorizer, intent: Intent): Boolean {
        require(!authorizer.requiresNewTask || intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0) {
            "Normal and automatic launches require ACTIVITY_NEW_TASK"
        }
        return useDirectPrivileged(authorizer) { it.startActivity(intent) >= 0 }
    }

    override suspend fun sendBroadcast(authorizer: Authorizer, intent: Intent): Boolean = useDirectPrivileged(authorizer) {
        it.sendBroadcast(intent)
    }
}
