// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.intent

import kotlinx.serialization.Serializable

/** Order is retained for editor drafts that store the selected type index. */
@Serializable
enum class ExtraType {
    String,
    Boolean,
    Byte,
    Short,
    Int,
    Long,
    Float,
    Double,
    Char,
    CharSequence,
    Uri,
    ComponentName,
    Bundle,
    StringArray,
    BooleanArray,
    ByteArray,
    ShortArray,
    IntArray,
    LongArray,
    FloatArray,
    DoubleArray,
    CharArray,
    StringList,
    IntList,
    Auto,
    ;

    val notation: String
        get() = when (this) {
            Bundle -> "Bundle (JSON)"
            StringArray -> "String[]"
            BooleanArray -> "Boolean[]"
            ByteArray -> "Byte[]"
            ShortArray -> "Short[]"
            IntArray -> "Int[]"
            LongArray -> "Long[]"
            FloatArray -> "Float[]"
            DoubleArray -> "Double[]"
            CharArray -> "Char[]"
            StringList -> "ArrayList<String>"
            IntList -> "ArrayList<Int>"
            else -> name
        }

    val isCollection: Boolean
        get() = when (this) {
            StringArray, BooleanArray, ByteArray, ShortArray, IntArray, LongArray,
            FloatArray, DoubleArray, CharArray, StringList, IntList,
            -> true

            else -> false
        }

    companion object {
        fun infer(value: String): ExtraType {
            val text = value.trim()
            if (text.toBooleanStrictOrNull() != null) return Boolean
            val digits = text.removePrefix("+").removePrefix("-")
            if (digits.length > 1 && digits.startsWith('0') && digits.all { it.isDigit() }) return String
            if (text.toIntOrNull() != null) return Int
            if (text.toLongOrNull() != null) return Long
            if (digits.isNotEmpty() && digits.all { it.isDigit() }) return String
            if (text.toDoubleOrNull()?.isFinite() == true) return Double
            return String
        }
    }
}
