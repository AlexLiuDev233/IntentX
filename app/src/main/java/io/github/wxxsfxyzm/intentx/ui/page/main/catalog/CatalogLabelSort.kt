// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.catalog

import java.text.CollationKey
import java.text.Collator
import java.util.Locale

/** Cache keys from one collator so comparisons use identical rules across loading jobs. */
internal object CatalogLabelSort {
    private val collator = Collator.getInstance(Locale.SIMPLIFIED_CHINESE).apply {
        strength = Collator.SECONDARY
        decomposition = Collator.CANONICAL_DECOMPOSITION
    }

    fun key(label: String): CollationKey = synchronized(collator) {
        collator.getCollationKey(label)
    }
}
