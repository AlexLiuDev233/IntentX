// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.ui.page.main.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wxxsfxyzm.intentx.R
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivitySortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityStatusFilter
import io.github.wxxsfxyzm.intentx.domain.catalog.CatalogSortOrder
import io.github.wxxsfxyzm.intentx.domain.catalog.SystemAppProvider
import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation
import io.github.wxxsfxyzm.intentx.domain.settings.repository.AppSettingsRepository
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class CatalogViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val provider: SystemAppProvider,
    private val settings: AppSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CatalogViewState(
            query = savedStateHandle["query"] ?: "",
            componentTab = if (savedStateHandle.get<String>("component_tab") == IntentOperation.Broadcast.name) {
                IntentOperation.Broadcast
            } else {
                IntentOperation.Activity
            },
        ),
    )
    val uiState = _uiState.asStateFlow()
    private val events = Channel<CatalogViewEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()
    private var source: List<CatalogItem> = emptyList()
    private var activitySource: List<CatalogItem> = emptyList()
    private var activityIndexReady = false
    private var observeJob: Job? = null
    private var loadJob: Job? = null
    private var activityIndexJob: Job? = null
    private var filterJob: Job? = null

    @OptIn(FlowPreview::class)
    fun dispatch(action: CatalogViewAction) {
        when (action) {
            is CatalogViewAction.SetQuery -> {
                savedStateHandle["query"] = action.query
                _uiState.update { it.copy(query = action.query) }
                filter(debounce = true)
            }

            is CatalogViewAction.SetShowSystem -> {
                viewModelScope.launch {
                    try {
                        settings.setShowSystemApps(action.show)
                    } catch (error: IOException) {
                        Timber.e(error, "Unable to save system-app visibility")
                        events.send(CatalogViewEvent.ShowMessage(R.string.settings_save_failed))
                    }
                }
            }

            is CatalogViewAction.SetSortOrder -> savePreference { settings.setCatalogSortOrder(action.order) }

            is CatalogViewAction.SetReverseOrder -> savePreference { settings.setCatalogReverseOrder(action.reverse) }

            is CatalogViewAction.SetShowPackageName -> savePreference { settings.setCatalogShowPackageName(action.show) }

            is CatalogViewAction.SetSearchActivities -> savePreference { settings.setCatalogSearchActivities(action.search) }

            is CatalogViewAction.SetHideOverlays -> savePreference { settings.setCatalogHideOverlays(action.hide) }

            is CatalogViewAction.SetActivitySortOrder -> savePreference { settings.setActivitySortOrder(action.order) }

            is CatalogViewAction.SetActivityExportedFilter -> savePreference { settings.setActivityExportedFilter(action.filter) }

            is CatalogViewAction.SetActivityEnabledFilter -> savePreference { settings.setActivityEnabledFilter(action.filter) }

            is CatalogViewAction.OpenApp -> viewModelScope.launch {
                val appLabel = source.firstOrNull { it.packageName == action.packageName }?.label.orEmpty()
                events.send(CatalogViewEvent.NavigateToActivities(action.packageName, appLabel))
            }

            is CatalogViewAction.OpenActivity -> viewModelScope.launch {
                events.send(
                    CatalogViewEvent.NavigateToEditor(
                        packageName = action.packageName,
                        className = action.className,
                        activityLabel = action.label,
                    ),
                )
            }

            is CatalogViewAction.OpenReceiver -> viewModelScope.launch {
                events.send(CatalogViewEvent.NavigateToEditor(action.packageName, action.className, action.label, IntentOperation.Broadcast))
            }

            is CatalogViewAction.SetComponentTab -> {
                if (_uiState.value.packageName == null || _uiState.value.componentTab == action.operation) return
                savedStateHandle["component_tab"] = action.operation.name
                filterJob?.cancel()
                source = emptyList()
                _uiState.update { it.copy(componentTab = action.operation, items = emptyList()) }
                reload()
            }

            is CatalogViewAction.StartObserving -> {
                if (observeJob?.isActive == true && _uiState.value.packageName == action.packageName) return
                observeJob?.cancel()
                if (_uiState.value.packageName != action.packageName) {
                    source = emptyList()
                    activitySource = emptyList()
                    activityIndexReady = false
                    activityIndexJob?.cancel()
                    _uiState.update { it.copy(packageName = action.packageName, items = emptyList()) }
                }
                observeJob = viewModelScope.launch {
                    launch {
                        if (action.packageName == null) {
                            combine(
                                settings.showSystemApps,
                                settings.catalogSortOrder,
                                settings.catalogReverseOrder,
                                settings.catalogShowPackageName,
                                settings.catalogSearchActivities,
                            ) { showSystem, sortOrder, reverseOrder, showPackageName, searchActivities ->
                                CatalogPreferences(showSystem, sortOrder, reverseOrder, showPackageName, searchActivities, false)
                            }.combine(settings.catalogHideOverlays) { prefs, hideOverlays ->
                                prefs.copy(hideOverlays = hideOverlays)
                            }.catch {
                                if (it !is IOException) throw it
                                Timber.e(it, "Unable to read catalog preferences")
                                events.send(CatalogViewEvent.ShowMessage(R.string.settings_read_failed))
                            }.collect { prefs ->
                                _uiState.update {
                                    it.copy(
                                        showSystem = prefs.showSystem,
                                        sortOrder = prefs.sortOrder,
                                        reverseOrder = prefs.reverseOrder,
                                        showPackageName = prefs.showPackageName,
                                        searchActivities = prefs.searchActivities,
                                        hideOverlays = prefs.hideOverlays,
                                    )
                                }
                                if (prefs.searchActivities) startActivityIndex()
                                filter()
                            }
                        } else {
                            combine(
                                settings.activitySortOrder,
                                settings.activityExportedFilter,
                                settings.activityEnabledFilter,
                            ) { sortOrder, exportedFilter, enabledFilter ->
                                ActivityPreferences(sortOrder, exportedFilter, enabledFilter)
                            }.catch {
                                if (it !is IOException) throw it
                                Timber.e(it, "Unable to read component filters")
                                events.send(CatalogViewEvent.ShowMessage(R.string.settings_read_failed))
                            }.collect { prefs ->
                                _uiState.update {
                                    it.copy(
                                        activitySortOrder = prefs.sortOrder,
                                        activityExportedFilter = prefs.exportedFilter,
                                        activityEnabledFilter = prefs.enabledFilter,
                                    )
                                }
                                filter()
                            }
                        }
                    }
                    provider.packageChanges.debounce(200.milliseconds).collect {
                        Timber.d("Installed packages changed; refreshing catalog")
                        reload()
                    }
                }
                reload()
            }

            CatalogViewAction.StopObserving -> {
                observeJob?.cancel()
                observeJob = null
                loadJob?.cancel()
                activityIndexJob?.cancel()
                filterJob?.cancel()
            }

            CatalogViewAction.Reload -> reload()
        }
    }

    private data class CatalogPreferences(
        val showSystem: Boolean,
        val sortOrder: CatalogSortOrder,
        val reverseOrder: Boolean,
        val showPackageName: Boolean,
        val searchActivities: Boolean,
        val hideOverlays: Boolean,
    )

    private data class ActivityPreferences(
        val sortOrder: ActivitySortOrder,
        val exportedFilter: ActivityStatusFilter,
        val enabledFilter: ActivityStatusFilter,
    )

    private fun savePreference(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (error: IOException) {
                Timber.e(error, "Unable to save catalog preference")
                events.send(CatalogViewEvent.ShowMessage(R.string.settings_save_failed))
            }
        }
    }

    private fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            val packageName = _uiState.value.packageName
            val started = System.nanoTime()
            Timber.d("Loading catalog: package=%s, tab=%s", packageName, _uiState.value.componentTab)
            if (packageName == null) {
                activityIndexJob?.cancel()
                activitySource = emptyList()
                activityIndexReady = false
            }
            try {
                val apps = if (packageName == null) provider.getInstalledApps() else emptyList()
                val operation = _uiState.value.componentTab
                val activities = if (packageName != null && operation == IntentOperation.Activity) provider.getActivities(packageName) else emptyList()
                val receivers = if (packageName != null && operation == IntentOperation.Broadcast) provider.getReceivers(packageName) else emptyList()
                source = withContext(Dispatchers.Default) {
                    val entries = apps.distinctBy { it.packageName }.map {
                        CatalogItem(
                            it.packageName,
                            it.packageName,
                            it.label,
                            it.lastUpdateTime,
                            firstInstallTime = it.firstInstallTime,
                            isSystem = it.isSystemApp,
                            isOverlay = it.isOverlay,
                        )
                    } + activities.distinctBy { it.className }.map {
                        CatalogItem(
                            it.className,
                            it.packageName,
                            it.label,
                            it.lastUpdateTime,
                            exported = it.exported,
                            enabled = it.enabled,
                            permission = it.permission,
                        )
                    } + receivers.distinctBy { it.className }.map {
                        CatalogItem(
                            it.className,
                            it.packageName,
                            it.label,
                            it.lastUpdateTime,
                            exported = it.exported,
                            enabled = it.enabled,
                            permission = it.permission,
                            operation = IntentOperation.Broadcast,
                        )
                    }
                    entries
                }
                filter()
                Timber.d("Catalog loaded: entries=%d, elapsedMs=%d", source.size, (System.nanoTime() - started) / 1_000_000)
                if (packageName == null && _uiState.value.searchActivities) startActivityIndex()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Catalog loading failed: package=%s", packageName)
                _uiState.update { it.copy(loadFailed = true, isLoading = false) }
            }
        }
    }

    private fun filter(debounce: Boolean = false) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            if (debounce) delay(120.milliseconds)
            val snapshot = _uiState.value
            val entries = if (snapshot.packageName == null && snapshot.searchActivities && snapshot.query.isNotBlank()) {
                source + activitySource
            } else {
                source
            }
            val filtered = withContext(Dispatchers.Default) {
                filterCatalogItems(
                    entries,
                    snapshot.query,
                    snapshot.showSystem || snapshot.packageName != null,
                    snapshot.hideOverlays,
                    snapshot.sortOrder,
                    snapshot.reverseOrder,
                    snapshot.packageName == null,
                    snapshot.activitySortOrder,
                    snapshot.activityExportedFilter,
                    snapshot.activityEnabledFilter,
                )
            }
            _uiState.update { it.copy(items = filtered, isLoading = loadJob?.isActive == true) }
        }
    }

    private fun startActivityIndex() {
        if (_uiState.value.packageName != null || activityIndexJob?.isActive == true || activityIndexReady) return
        activityIndexJob = viewModelScope.launch {
            val started = System.nanoTime()
            Timber.d("Building optional activity search index")
            try {
                val activities = provider.getAllActivities()
                activitySource = withContext(Dispatchers.Default) {
                    activities.distinctBy { it.packageName to it.className }.map {
                        CatalogItem(
                            id = it.className,
                            packageName = it.packageName,
                            label = it.label,
                            lastUpdateTime = it.lastUpdateTime,
                            isSystem = it.isSystemApp,
                            isOverlay = it.isOverlay,
                            exported = it.exported,
                            enabled = it.enabled,
                            permission = it.permission,
                        )
                    }
                }
                activityIndexReady = true
                Timber.d("Activity index ready: entries=%d, elapsedMs=%d", activitySource.size, (System.nanoTime() - started) / 1_000_000)
                filter()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Optional activity search index failed")
                // Optional activity search must not affect the app catalog.
            }
        }
    }
}
