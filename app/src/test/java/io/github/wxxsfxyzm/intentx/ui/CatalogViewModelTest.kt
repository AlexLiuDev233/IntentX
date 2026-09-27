// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivitySortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityStatusFilter
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityTarget
import io.github.wxxsfxyzm.intentx.domain.catalog.CatalogSortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.InstalledAppTarget
import io.github.wxxsfxyzm.intentx.domain.catalog.ReceiverTarget
import io.github.wxxsfxyzm.intentx.domain.catalog.SystemAppProvider
import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation
import io.github.wxxsfxyzm.intentx.domain.settings.repository.AppSettingsRepository
import io.github.wxxsfxyzm.intentx.executor.Authorizer
import io.github.wxxsfxyzm.intentx.ui.page.main.catalog.CatalogItem
import io.github.wxxsfxyzm.intentx.ui.page.main.catalog.CatalogViewAction
import io.github.wxxsfxyzm.intentx.ui.page.main.catalog.CatalogViewEvent
import io.github.wxxsfxyzm.intentx.ui.page.main.catalog.CatalogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() = runTest {
        val jobs = store.keys().mapNotNull { store[it]?.viewModelScope?.coroutineContext?.get(Job) }
        store.clear()
        jobs.joinAll()
        Dispatchers.resetMain()
    }

    @Test
    fun filtersSystemAppsAndSearchesWithoutRescanning() = runTest {
        val provider = FakeProvider()
        val model = CatalogViewModel(SavedStateHandle(), provider, FakeSettings())
        store.put("catalog", model)
        model.dispatch(CatalogViewAction.StartObserving())
        val first = model.uiState.first { !it.isLoading && it.items.size == 2 }
        assertEquals(listOf("alpha.app", "beta.app"), first.items.map { it.id })
        model.dispatch(CatalogViewAction.SetShowSystem(true))
        model.uiState.first { it.items.size == 3 }
        model.dispatch(CatalogViewAction.SetQuery("unmatched"))
        model.dispatch(CatalogViewAction.SetQuery("  系统   SYSTEM  "))
        val searched = model.uiState.first { it.items.size == 1 && it.items.single().id == "system.app" }
        assertEquals("系统设置", searched.items.single().label)
        assertEquals(1, provider.appScans)
        model.dispatch(CatalogViewAction.SetShowSystem(false))
        model.uiState.first { it.items.isEmpty() }
        assertEquals(1, provider.appScans)
    }

    @Test
    fun changesSortAndDisplayPreferencesWithoutRescanning() = runTest {
        val provider = FakeProvider().apply {
            apps = listOf(
                InstalledAppTarget("z.pkg", "Alpha", null, 1, 100, 100, false),
                InstalledAppTarget("a.pkg", "Beta", null, 1, 200, 200, false),
            )
        }
        val settings = FakeSettings()
        val model = CatalogViewModel(SavedStateHandle(), provider, settings)
        store.put("catalog", model)
        model.dispatch(CatalogViewAction.StartObserving())
        model.uiState.first { !it.isLoading && it.items.size == 2 }
        assertEquals(listOf("z.pkg", "a.pkg"), model.uiState.value.items.map { it.id })

        model.dispatch(CatalogViewAction.SetSortOrder(CatalogSortOrder.PackageName))
        model.uiState.first {
            it.sortOrder == CatalogSortOrder.PackageName && it.items.map { item -> item.id } == listOf(
                "a.pkg",
                "z.pkg",
            )
        }
        model.dispatch(CatalogViewAction.SetSortOrder(CatalogSortOrder.FirstInstallTime))
        model.uiState.first {
            it.sortOrder == CatalogSortOrder.FirstInstallTime && it.items.map { item -> item.id } == listOf(
                "a.pkg",
                "z.pkg",
            )
        }
        model.dispatch(CatalogViewAction.SetReverseOrder(true))
        model.uiState.first { it.reverseOrder && it.items.map { item -> item.id } == listOf("z.pkg", "a.pkg") }
        model.dispatch(CatalogViewAction.SetShowPackageName(false))
        model.uiState.first { !it.showPackageName }
        assertEquals(1, provider.appScans)
        assertEquals(CatalogSortOrder.FirstInstallTime, settings.catalogSortOrder.value)
        assertEquals(true, settings.catalogReverseOrder.value)
        assertEquals(false, settings.catalogShowPackageName.value)
    }

    @Test
    fun hidingResourceOverlaysFiltersCachedAppsWithoutRescanning() = runTest {
        val provider = FakeProvider().apply {
            apps = listOf(
                app("regular.pkg", "Regular"),
                app("overlay.pkg", "Overlay", system = true, overlay = true),
            )
        }
        val settings = FakeSettings()
        val model = CatalogViewModel(SavedStateHandle(), provider, settings)
        store.put("overlays", model)
        model.dispatch(CatalogViewAction.StartObserving())
        model.dispatch(CatalogViewAction.SetShowSystem(true))
        model.uiState.first { !it.isLoading && it.items.size == 2 }

        model.dispatch(CatalogViewAction.SetHideOverlays(true))
        val hidden = model.uiState.first { it.hideOverlays && it.items.map { item -> item.id } == listOf("regular.pkg") }
        assertEquals(false, hidden.items.single().isOverlay)
        assertEquals(true, settings.catalogHideOverlays.value)
        assertEquals(1, provider.appScans)

        model.dispatch(CatalogViewAction.SetHideOverlays(false))
        model.uiState.first { !it.hideOverlays && it.items.size == 2 }
        assertEquals(1, provider.appScans)
    }

    @Test
    fun activityPageLoadsOnlySelectedPackageAndKeepsNonExportedDisabledEntries() = runTest {
        val provider = FakeProvider()
        val model = CatalogViewModel(SavedStateHandle(), provider, FakeSettings())
        store.put("catalog", model)
        model.dispatch(CatalogViewAction.StartObserving("alpha.app"))
        val state = model.uiState.first { !it.isLoading && it.items.size == 1 }
        assertEquals("alpha.app", provider.activityPackage)
        assertEquals(0, provider.appScans)
        assertEquals(false, state.items.single().exported)
        assertFalse(state.items.single().enabled)
        assertEquals("example.permission", state.items.single().permission)
    }

    @Test
    fun duplicateActivityDeclarationsDoNotProduceDuplicateListKeys() = runTest {
        val packageName = "vendor.package"
        val className = "vendor.package.CommandLineActivity"
        val provider = FakeProvider().apply {
            activities = listOf(
                ActivityTarget(packageName, className, "Command line", true, true, null, 1),
                ActivityTarget(packageName, className, "Command line", true, true, null, 1),
            )
        }
        val model = CatalogViewModel(SavedStateHandle(), provider, FakeSettings())
        store.put("duplicateActivities", model)
        model.dispatch(CatalogViewAction.StartObserving(packageName))
        val items = model.uiState.first { !it.isLoading && it.items.isNotEmpty() }.items
        assertEquals(listOf("activity:$packageName/$className"), items.map(CatalogItem::listKey))
        assertNotEquals(items.single().listKey, CatalogItem(className, "other.package", "Other", 1, exported = true).listKey)
    }

    @Test
    fun receiverTabLoadsReceiversAndRoutesToBroadcastEditor() = runTest {
        val packageName = "alpha.app"
        val receiver = ReceiverTarget(packageName, "$packageName.Receiver", "Receiver", false, true, null, 1)
        val provider = FakeProvider().apply { receivers = listOf(receiver) }
        val model = CatalogViewModel(SavedStateHandle(), provider, FakeSettings())
        store.put("receivers", model)
        model.dispatch(CatalogViewAction.StartObserving(packageName))
        model.uiState.first { !it.isLoading && it.items.isNotEmpty() }
        model.dispatch(CatalogViewAction.SetComponentTab(IntentOperation.Broadcast))
        val state = model.uiState.first { !it.isLoading && it.componentTab == IntentOperation.Broadcast && it.items.isNotEmpty() }
        assertEquals("broadcast:$packageName/${receiver.className}", state.items.single().listKey)
        assertEquals(1, provider.activityScans)
        assertEquals(1, provider.receiverScans)
        model.dispatch(CatalogViewAction.OpenReceiver(packageName, receiver.className, receiver.label))
        assertEquals(
            CatalogViewEvent.NavigateToEditor(packageName, receiver.className, receiver.label, IntentOperation.Broadcast),
            model.eventFlow.first(),
        )
    }

    @Test
    fun activitySortAndStatusFiltersReuseLoadedActivities() = runTest {
        val packageName = "alpha.app"
        val provider = FakeProvider().apply {
            activities = listOf(
                ActivityTarget(packageName, "$packageName.ZActivity", "Alpha", true, false, null, 1),
                ActivityTarget(packageName, "$packageName.AActivity", "Beta", false, true, null, 1),
                ActivityTarget(packageName, "$packageName.MActivity", "Gamma", true, true, null, 1),
            )
        }
        val settings = FakeSettings()
        val model = CatalogViewModel(SavedStateHandle(), provider, settings)
        store.put("activities", model)
        model.dispatch(CatalogViewAction.StartObserving(packageName))
        val initial = model.uiState.first { !it.isLoading && it.items.size == 3 }
        assertEquals(listOf("Alpha", "Beta", "Gamma"), initial.items.map { it.label })

        model.dispatch(CatalogViewAction.SetActivitySortOrder(ActivitySortOrder.ClassName))
        model.uiState.first {
            it.activitySortOrder == ActivitySortOrder.ClassName && it.items.map { item -> item.label } == listOf(
                "Beta",
                "Gamma",
                "Alpha",
            )
        }
        model.dispatch(CatalogViewAction.SetActivityExportedFilter(ActivityStatusFilter.Yes))
        model.uiState.first {
            it.activityExportedFilter == ActivityStatusFilter.Yes && it.items.map { item -> item.label } == listOf(
                "Gamma",
                "Alpha",
            )
        }
        model.dispatch(CatalogViewAction.SetActivityEnabledFilter(ActivityStatusFilter.No))
        model.uiState.first {
            it.activityEnabledFilter == ActivityStatusFilter.No && it.items.map { item -> item.label } == listOf(
                "Alpha",
            )
        }
        model.dispatch(CatalogViewAction.SetActivityExportedFilter(ActivityStatusFilter.No))
        model.uiState.first { it.activityExportedFilter == ActivityStatusFilter.No && it.items.isEmpty() }
        model.dispatch(CatalogViewAction.SetActivityEnabledFilter(ActivityStatusFilter.Yes))
        model.uiState.first {
            it.activityEnabledFilter == ActivityStatusFilter.Yes && it.items.map { item -> item.label } == listOf(
                "Beta",
            )
        }
        assertEquals(1, provider.activityScans)
        assertEquals(0, provider.appScans)
        assertEquals(ActivitySortOrder.ClassName, settings.activitySortOrder.value)
        assertEquals(ActivityStatusFilter.No, settings.activityExportedFilter.value)
        assertEquals(ActivityStatusFilter.Yes, settings.activityEnabledFilter.value)
    }

    @Test
    fun packageChangeReloadsAndFailureCanBeRetried() = runTest {
        val provider = FakeProvider()
        val model = CatalogViewModel(SavedStateHandle(), provider, FakeSettings())
        store.put("catalog", model)
        model.dispatch(CatalogViewAction.StartObserving())
        model.uiState.first { !it.isLoading && it.items.size == 2 }
        provider.fail = true
        provider.packageChanges.emit(Unit)
        model.uiState.first { it.loadFailed && !it.isLoading }
        provider.fail = false
        provider.apps = emptyList()
        model.dispatch(CatalogViewAction.Reload)
        model.uiState.first { !it.loadFailed && !it.isLoading && it.items.isEmpty() }
        assertEquals(3, provider.appScans)
        model.dispatch(CatalogViewAction.StopObserving)
        provider.packageChanges.subscriptionCount.first { it == 0 }
    }

    private class FakeProvider : SystemAppProvider {
        override val packageChanges = MutableSharedFlow<Unit>()
        var appScans = 0
        var activityPackage: String? = null
        var activityScans = 0
        var receiverScans = 0
        var receivers = emptyList<ReceiverTarget>()
        var activities: List<ActivityTarget>? = null
        var fail = false
        var apps = listOf(app("beta.app", "Beta"), app("system.app", "系统设置", true), app("alpha.app", "Alpha"))

        override suspend fun getInstalledApps(): List<InstalledAppTarget> {
            appScans++
            if (fail) error("Test scan failure")
            return apps
        }

        override suspend fun getActivities(packageName: String): List<ActivityTarget> {
            activityPackage = packageName
            activityScans++
            return activities ?: listOf(
                ActivityTarget(
                    packageName,
                    "$packageName.HiddenActivity",
                    "Hidden",
                    false,
                    false,
                    "example.permission",
                    1,
                ),
            )
        }

        override suspend fun getReceivers(packageName: String): List<ReceiverTarget> {
            receiverScans++
            return receivers
        }

        fun app(name: String, label: String, system: Boolean = false, overlay: Boolean = false) = InstalledAppTarget(name, label, null, 1, 1, 1, system, overlay)
    }

    private class FakeSettings : AppSettingsRepository {
        override val authorizer = MutableStateFlow(Authorizer.None)
        override val showSystemApps = MutableStateFlow(false)
        override val catalogSortOrder = MutableStateFlow(CatalogSortOrder.Label)
        override val catalogReverseOrder = MutableStateFlow(false)
        override val catalogShowPackageName = MutableStateFlow(true)
        override val catalogHideOverlays = MutableStateFlow(false)
        override val activitySortOrder = MutableStateFlow(ActivitySortOrder.Label)
        override val activityExportedFilter = MutableStateFlow(ActivityStatusFilter.All)
        override val activityEnabledFilter = MutableStateFlow(ActivityStatusFilter.All)
        override suspend fun setAuthorizer(authorizer: Authorizer) = Unit
        override suspend fun setShowSystemApps(show: Boolean) {
            showSystemApps.value = show
        }

        override suspend fun setCatalogSortOrder(order: CatalogSortOrder) {
            catalogSortOrder.value = order
        }

        override suspend fun setCatalogReverseOrder(reverse: Boolean) {
            catalogReverseOrder.value = reverse
        }

        override suspend fun setCatalogShowPackageName(show: Boolean) {
            catalogShowPackageName.value = show
        }

        override suspend fun setCatalogHideOverlays(hide: Boolean) {
            catalogHideOverlays.value = hide
        }

        override suspend fun setActivitySortOrder(order: ActivitySortOrder) {
            activitySortOrder.value = order
        }

        override suspend fun setActivityExportedFilter(filter: ActivityStatusFilter) {
            activityExportedFilter.value = filter
        }

        override suspend fun setActivityEnabledFilter(filter: ActivityStatusFilter) {
            activityEnabledFilter.value = filter
        }
    }
}
