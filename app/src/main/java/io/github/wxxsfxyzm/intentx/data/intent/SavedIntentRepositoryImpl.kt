// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.data.intent

import io.github.wxxsfxyzm.intentx.data.settings.local.datastore.AppDataStore
import io.github.wxxsfxyzm.intentx.domain.intent.SavedIntentProfile
import io.github.wxxsfxyzm.intentx.domain.intent.SavedIntentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber

class SavedIntentRepositoryImpl(
    private val store: AppDataStore,
    private val json: Json,
) : SavedIntentRepository {
    override val profiles: Flow<List<SavedIntentProfile>> = store.getString(AppDataStore.SAVED_INTENT_PROFILES)
        .map { decode(it).sortedByDescending(SavedIntentProfile::updatedAt) }
        .catch {
            Timber.e(it, "Unable to read saved Intent profiles")
            throw it
        }

    override suspend fun upsert(profile: SavedIntentProfile) {
        store.edit { preferences ->
            val existing = decode(preferences[AppDataStore.SAVED_INTENT_PROFILES].orEmpty())
            preferences[AppDataStore.SAVED_INTENT_PROFILES] =
                json.encodeToString(existing.filterNot { it.id == profile.id } + profile)
        }
    }

    override suspend fun delete(id: String) {
        store.edit { preferences ->
            val existing = decode(preferences[AppDataStore.SAVED_INTENT_PROFILES].orEmpty())
            preferences[AppDataStore.SAVED_INTENT_PROFILES] = json.encodeToString(existing.filterNot { it.id == id })
        }
    }

    private fun decode(value: String): List<SavedIntentProfile> = if (value.isBlank()) emptyList() else json.decodeFromString(value)
}
