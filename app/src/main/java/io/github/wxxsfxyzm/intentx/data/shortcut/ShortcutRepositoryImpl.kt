// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.data.shortcut

import io.github.wxxsfxyzm.intentx.data.settings.local.datastore.AppDataStore
import io.github.wxxsfxyzm.intentx.domain.shortcut.IntentShortcut
import io.github.wxxsfxyzm.intentx.domain.shortcut.ShortcutRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import timber.log.Timber

class ShortcutRepositoryImpl(private val store: AppDataStore, private val json: Json) : ShortcutRepository {

    override suspend fun get(id: String): IntentShortcut? = decode(store.getString(AppDataStore.INTENT_SHORTCUTS).first()).firstOrNull { it.id == id }

    override suspend fun upsert(shortcut: IntentShortcut) {
        store.edit { preferences ->
            val existing = decode(preferences[AppDataStore.INTENT_SHORTCUTS].orEmpty())
            preferences[AppDataStore.INTENT_SHORTCUTS] = json.encodeToString(existing.filterNot { it.id == shortcut.id } + shortcut)
        }
        Timber.d("Shortcut snapshot persisted: id=%s, version=%d", shortcut.id, shortcut.version)
    }

    private fun decode(value: String): List<IntentShortcut> = if (value.isBlank()) emptyList() else json.decodeFromString(value)
}
