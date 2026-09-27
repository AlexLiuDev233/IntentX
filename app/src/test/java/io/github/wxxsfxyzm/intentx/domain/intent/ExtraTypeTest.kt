// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.intent

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtraTypeTest {
    @Test
    fun inferCommonScalarTypes() {
        assertEquals(ExtraType.Boolean, ExtraType.infer(" true "))
        assertEquals(ExtraType.Int, ExtraType.infer("42"))
        assertEquals(ExtraType.Long, ExtraType.infer("2147483648"))
        assertEquals(ExtraType.Double, ExtraType.infer("1.25"))
    }

    @Test
    fun preserveLeadingZerosAndAmbiguousValuesAsStrings() {
        listOf("00123", "-001", "9223372036854775808", "", "hello", "NaN", "Infinity", "content://example", "[1,2]").forEach {
            assertEquals(it, ExtraType.String, ExtraType.infer(it))
        }
    }
}
