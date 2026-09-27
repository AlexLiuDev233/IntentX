// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.executor.connection

import io.github.wxxsfxyzm.intentx.executor.Authorizer
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/** Only connection acquisition may fall back; never retry an already submitted operation. */
internal object AuthorizerConnectionSelector {
    fun <T> connect(authorizer: Authorizer, open: (Authorizer) -> T): T {
        if (authorizer != Authorizer.Auto) return open(authorizer)
        for (candidate in listOf(Authorizer.Root, Authorizer.Shizuku)) {
            ensureNotInterrupted()
            try {
                return open(candidate)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (interrupted: InterruptedException) {
                throw interrupted
            } catch (error: Exception) {
                ensureNotInterrupted()
                Timber.d(error, "Auto authorizer connection unavailable: %s", candidate)
            }
        }
        ensureNotInterrupted()
        return open(Authorizer.None)
    }

    private fun ensureNotInterrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedException("Authorization selection interrupted")
    }
}
