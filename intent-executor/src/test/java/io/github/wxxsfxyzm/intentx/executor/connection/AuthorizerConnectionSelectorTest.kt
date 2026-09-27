// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.executor.connection

import io.github.wxxsfxyzm.intentx.executor.Authorizer
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthorizerConnectionSelectorTest {
    @Test
    fun automaticModeChoosesHighestAvailableConnection() {
        val priority = listOf(Authorizer.Root, Authorizer.Shizuku, Authorizer.None)
        priority.forEachIndexed { index, available ->
            val attempts = mutableListOf<Authorizer>()
            val selected = AuthorizerConnectionSelector.connect(Authorizer.Auto) {
                attempts += it
                check(it == available) { "Unavailable" }
                it
            }
            assertEquals(available, selected)
            assertEquals(priority.take(index + 1), attempts)
        }
    }

    @Test
    fun explicitModeDoesNotFallbackOnFailure() {
        val attempts = mutableListOf<Authorizer>()
        val failure = SecurityException("Authorization denied")
        val actual = assertThrows(SecurityException::class.java) {
            AuthorizerConnectionSelector.connect(Authorizer.Shizuku) {
                attempts += it
                throw failure
            }
        }
        assertSame(failure, actual)
        assertEquals(listOf(Authorizer.Shizuku), attempts)
    }

    @Test
    fun cancellationAndInterruptionDoNotFallback() {
        listOf(CancellationException("Cancelled"), InterruptedException("Interrupted")).forEach { failure ->
            val attempts = mutableListOf<Authorizer>()
            val actual = assertThrows(failure.javaClass) {
                AuthorizerConnectionSelector.connect(Authorizer.Auto) {
                    attempts += it
                    throw failure
                }
            }
            assertSame(failure, actual)
            assertEquals(listOf(Authorizer.Root), attempts)
        }
    }

    @Test
    fun interruptedConnectionFailureDoesNotContinueToAnotherIdentity() {
        val attempts = mutableListOf<Authorizer>()
        try {
            assertThrows(InterruptedException::class.java) {
                AuthorizerConnectionSelector.connect(Authorizer.Auto) {
                    attempts += it
                    Thread.currentThread().interrupt()
                    error("Connection aborted")
                }
            }
            assertEquals(listOf(Authorizer.Root), attempts)
        } finally {
            Thread.interrupted()
        }
    }
}
