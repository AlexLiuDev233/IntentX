// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntentFlagsTest {
    @Test
    fun acceptsSignedUnsignedAndHexadecimalMasksWithoutLosingHighBits() {
        for (input in listOf("-1", "4294967295", "0xFFFFFFFF", " 0Xffffffff ")) {
            assertEquals(UInt.MAX_VALUE, parseFlags(input))
        }
        assertEquals(0x80000000u, parseFlags("-2147483648"))
        assertEquals(0u, parseFlags("0"))
    }

    @Test
    fun rejectsOverflowAndMalformedInput() {
        for (input in listOf("", "0x", "4294967296", "-2147483649", "0x100000000", "1.5", "0xGG")) {
            assertNull(parseFlags(input))
        }
    }

    @Test
    fun togglingKnownFlagPreservesUnknownBits() {
        val original = parseFlags("0x80000001")!!
        val newTask = 0x10000000u
        val edited = "0x" + (original xor newTask).toString(16)
        assertEquals(0x90000001u, parseFlags(edited))
        assertEquals(original, parseFlags(edited)!! xor newTask)
    }
}
