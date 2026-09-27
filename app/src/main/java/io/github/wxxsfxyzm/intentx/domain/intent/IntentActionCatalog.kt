// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.intent

import android.content.Intent

/** Common launch actions offered as suggestions; custom action strings remain valid. */
object IntentActionCatalog {
    val suggestions: List<String> = listOf(Intent.ACTION_VIEW, Intent.ACTION_MAIN)

    fun matching(query: String): List<String> {
        val term = query.trim()
        return suggestions.filter { it.contains(term, ignoreCase = true) }
    }
}
