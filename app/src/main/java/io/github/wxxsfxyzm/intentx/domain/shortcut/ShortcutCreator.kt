// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.shortcut

interface ShortcutCreator {
    suspend fun pin(shortcut: IntentShortcut, iconUri: String?): ShortcutResult
}
