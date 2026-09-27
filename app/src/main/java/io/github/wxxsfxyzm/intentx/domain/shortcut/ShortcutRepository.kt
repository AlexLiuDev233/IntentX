// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.shortcut

interface ShortcutRepository {
    suspend fun get(id: String): IntentShortcut?
    suspend fun upsert(shortcut: IntentShortcut)

    suspend fun resolve(id: String, token: String): IntentShortcut? = if (token.isBlank()) null else get(id)?.takeIf { it.version == 1 && it.token == token }
}
