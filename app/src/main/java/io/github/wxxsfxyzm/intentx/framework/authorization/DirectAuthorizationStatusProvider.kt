// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.framework.authorization

import android.content.pm.PackageManager
import io.github.wxxsfxyzm.intentx.domain.authorization.AuthorizationStatus
import io.github.wxxsfxyzm.intentx.domain.authorization.AuthorizationStatusProvider
import io.github.wxxsfxyzm.intentx.domain.authorization.RootMode
import io.github.wxxsfxyzm.intentx.domain.authorization.ShizukuMode
import io.github.wxxsfxyzm.intentx.domain.authorization.ShizukuState
import io.github.wxxsfxyzm.intentx.executor.Authorizer
import io.github.wxxsfxyzm.intentx.executor.DirectPrivilegedExecutor
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber

class DirectAuthorizationStatusProvider(private val executor: DirectPrivilegedExecutor) : AuthorizationStatusProvider {
    private class PermissionRequest(val code: Int)

    private val pendingPermissionRequest = AtomicReference<PermissionRequest?>(null)

    override val shizukuState = callbackFlow {
        fun refresh() {
            launch(Dispatchers.IO) {
                val state = try {
                    when {
                        !Shizuku.pingBinder() -> ShizukuState(AuthorizationStatus.Unavailable)

                        else -> {
                            val mode = when (Shizuku.getUid()) {
                                0 -> ShizukuMode.Root
                                2000 -> ShizukuMode.Shell
                                else -> ShizukuMode.Unknown
                            }
                            val status = if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                AuthorizationStatus.Ready
                            } else {
                                AuthorizationStatus.PermissionRequired
                            }
                            ShizukuState(status, mode)
                        }
                    }
                } catch (error: Exception) {
                    Timber.w(error, "Unable to query Shizuku status")
                    ShizukuState(AuthorizationStatus.Unavailable)
                }
                trySend(state)
            }
        }

        val received = Shizuku.OnBinderReceivedListener { refresh() }
        val dead = Shizuku.OnBinderDeadListener {
            Timber.w("Shizuku Binder died")
            trySend(ShizukuState(AuthorizationStatus.Unavailable))
        }
        val permission = Shizuku.OnRequestPermissionResultListener { code, _ ->
            val pending = pendingPermissionRequest.get()
            if (pending != null && pending.code == code && pendingPermissionRequest.compareAndSet(pending, null)) {
                Timber.d("Shizuku permission request completed")
                refresh()
            }
        }
        Shizuku.addBinderReceivedListenerSticky(received)
        Shizuku.addBinderDeadListener(dead)
        Shizuku.addRequestPermissionResultListener(permission)
        val polling = launch {
            while (true) {
                refresh()
                delay(2000.milliseconds)
            }
        }
        awaitClose {
            polling.cancel()
            Shizuku.removeBinderReceivedListener(received)
            Shizuku.removeBinderDeadListener(dead)
            Shizuku.removeRequestPermissionResultListener(permission)
        }
    }.distinctUntilChanged().onEach {
        Timber.d("Shizuku state changed: status=%s, identity=%s", it.status, it.mode)
    }

    override suspend fun checkRoot(): RootMode {
        Timber.d("Connecting to root Binder bridge")
        executor.check(Authorizer.Root)
        return withContext(Dispatchers.IO) {
            when {
                checkRootBinary("ksud -V") -> RootMode.KernelSU
                checkRootBinary("magisk -v") -> RootMode.Magisk
                checkRootBinary("apd -V") -> RootMode.APatch
                else -> RootMode.Other
            }
        }
    }

    private fun checkRootBinary(command: String): Boolean {
        val process = try {
            ProcessBuilder("su", "-c", "export PATH=\$PATH:/data/adb/ksu/bin:/data/adb/ap/bin:/data/adb/magisk/bin && $command")
                .redirectErrorStream(true)
                .redirectOutput(File("/dev/null"))
                .start()
        } catch (error: Exception) {
            Timber.d(error, "Root implementation probe could not start: %s", command)
            return false
        }
        return try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                Timber.w("Root implementation probe timed out: %s", command)
                process.destroyForcibly()
                false
            } else {
                val exitCode = process.exitValue()
                Timber.d("Root implementation probe finished: command=%s, exitCode=%d", command, exitCode)
                exitCode == 0
            }
        } catch (cancelled: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            throw CancellationException("Root detection interrupted").apply { initCause(cancelled) }
        }
    }

    override fun requestShizukuPermission() {
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Timber.d("Requesting Shizuku permission; remoteUid=%d", Shizuku.getUid())
            val request = PermissionRequest(Random.nextInt())
            pendingPermissionRequest.set(request)
            try {
                Shizuku.requestPermission(request.code)
            } catch (error: Exception) {
                Timber.w(error, "Shizuku permission request failed")
                pendingPermissionRequest.compareAndSet(request, null)
                throw error
            }
        }
    }
}
