// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.wxxsfxyzm.intentx.data.settings.local.datastore.AppDataStore
import io.github.wxxsfxyzm.intentx.data.settings.repository.AppSettingsRepositoryImpl
import io.github.wxxsfxyzm.intentx.data.shortcut.ShortcutRepositoryImpl
import io.github.wxxsfxyzm.intentx.di.serializationModule
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivitySortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityStatusFilter
import io.github.wxxsfxyzm.intentx.domain.catalog.CatalogSortOrder
import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation
import io.github.wxxsfxyzm.intentx.domain.intent.IntentSpec
import io.github.wxxsfxyzm.intentx.domain.shortcut.IntentShortcut
import io.github.wxxsfxyzm.intentx.executor.Authorizer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication

class AppDataStoreTest {
    private val serialization = koinApplication { modules(serializationModule) }
    private val json = serialization.koin.get<Json>()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @After
    fun tearDown() {
        serialization.close()
    }

    @Test
    fun authorizationSurvivesReopeningWithoutChangingTheme() = runTest {
        val file = File(temporaryFolder.root, "authorization.preferences_pb")
        withStore(file) { store ->
            val settings = AppSettingsRepositoryImpl(store)
            assertEquals(Authorizer.None, settings.authorizer.first())
            store.putBoolean(AppDataStore.THEME_USE_DYNAMIC_COLOR, false)
        }
        for (authorizer in Authorizer.entries) {
            withStore(file) { AppSettingsRepositoryImpl(it).setAuthorizer(authorizer) }
            withStore(file) { store ->
                assertEquals(authorizer, AppSettingsRepositoryImpl(store).authorizer.first())
                assertFalse(store.getBoolean(AppDataStore.THEME_USE_DYNAMIC_COLOR, true).first())
            }
        }
    }

    @Test
    fun catalogPreferencesSurviveReopening() = runTest {
        val file = File(temporaryFolder.root, "catalog.preferences_pb")
        withStore(file) { store ->
            val settings = AppSettingsRepositoryImpl(store)
            assertFalse(settings.showSystemApps.first())
            assertEquals(CatalogSortOrder.Label, settings.catalogSortOrder.first())
            assertFalse(settings.catalogReverseOrder.first())
            assertEquals(true, settings.catalogShowPackageName.first())
            settings.setShowSystemApps(true)
            settings.setCatalogSortOrder(CatalogSortOrder.FirstInstallTime)
            settings.setCatalogReverseOrder(true)
            settings.setCatalogShowPackageName(false)
        }
        withStore(file) { store ->
            assertEquals(true, AppSettingsRepositoryImpl(store).showSystemApps.first())
            val settings = AppSettingsRepositoryImpl(store)
            assertEquals(CatalogSortOrder.FirstInstallTime, settings.catalogSortOrder.first())
            assertEquals(true, settings.catalogReverseOrder.first())
            assertEquals(false, settings.catalogShowPackageName.first())
        }
    }

    @Test
    fun activityPreferencesSurviveReopening() = runTest {
        val file = File(temporaryFolder.root, "activities.preferences_pb")
        withStore(file) { store ->
            val settings = AppSettingsRepositoryImpl(store)
            assertEquals(ActivitySortOrder.Label, settings.activitySortOrder.first())
            assertEquals(ActivityStatusFilter.All, settings.activityExportedFilter.first())
            assertEquals(ActivityStatusFilter.All, settings.activityEnabledFilter.first())
            settings.setActivitySortOrder(ActivitySortOrder.ClassName)
            settings.setActivityExportedFilter(ActivityStatusFilter.Yes)
            settings.setActivityEnabledFilter(ActivityStatusFilter.No)
        }
        withStore(file) { store ->
            val settings = AppSettingsRepositoryImpl(store)
            assertEquals(ActivitySortOrder.ClassName, settings.activitySortOrder.first())
            assertEquals(ActivityStatusFilter.Yes, settings.activityExportedFilter.first())
            assertEquals(ActivityStatusFilter.No, settings.activityEnabledFilter.first())
        }
    }

    @Test
    fun unknownAuthorizationFallsBackWithoutOverwritingStoredValue() = runTest {
        withStore(File(temporaryFolder.root, "unknown.preferences_pb")) { store ->
            store.putString(AppDataStore.AUTHORIZER, "FutureBackend")
            assertEquals(Authorizer.None, AppSettingsRepositoryImpl(store).authorizer.first())
            assertEquals("FutureBackend", store.getString(AppDataStore.AUTHORIZER).first())
        }
    }

    @Test
    fun shortcutSnapshotsSurviveReopeningAndRejectInvalidCapabilities() = runTest {
        val file = File(temporaryFolder.root, "shortcuts.preferences_pb")
        val spec = IntentSpec("example.app", "example.app.Main", null, null, null, emptyList(), 0x10000000, emptyList(), null)
        val shortcuts = Authorizer.entries.map { mode ->
            IntentShortcut(mode.name, "token-${mode.name}", "Example", spec, IntentOperation.Activity, mode)
        }
        withStore(file) { store ->
            val repository = ShortcutRepositoryImpl(store, json)
            shortcuts.forEach { repository.upsert(it) }
            // A newer writer can add fields without breaking this reader's injected configuration.
            val payload = store.getString(AppDataStore.INTENT_SHORTCUTS).first()
            store.putString(AppDataStore.INTENT_SHORTCUTS, payload.replace("\"token\":", "\"futureField\":true,\"token\":"))
        }
        withStore(file) { store ->
            val repository = ShortcutRepositoryImpl(store, json)
            for (shortcut in shortcuts) {
                assertEquals(shortcut, repository.resolve(shortcut.id, shortcut.token))
                assertNull(repository.resolve(shortcut.id, "wrong-token"))
                assertNull(repository.resolve(shortcut.id, ""))
            }
            assertNull(repository.resolve("missing", shortcuts.first().token))
            val updated = shortcuts.first().copy(name = "Updated", operation = IntentOperation.Broadcast)
            repository.upsert(updated)
            assertEquals(updated, repository.resolve(updated.id, updated.token))
            assertEquals(shortcuts.last(), repository.resolve(shortcuts.last().id, shortcuts.last().token))
            val future = updated.copy(version = 2)
            repository.upsert(future)
            assertNull(repository.resolve(future.id, future.token))
        }
    }

    private suspend fun withStore(file: File, block: suspend (AppDataStore) -> Unit) {
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file },
        )
        try {
            block(AppDataStore(store))
        } finally {
            job.cancelAndJoin()
        }
    }
}
