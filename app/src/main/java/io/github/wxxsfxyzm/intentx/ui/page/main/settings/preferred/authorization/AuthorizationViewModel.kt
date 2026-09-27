// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wxxsfxyzm.intentx.R
import io.github.wxxsfxyzm.intentx.domain.authorization.AuthorizationStatus
import io.github.wxxsfxyzm.intentx.domain.authorization.AuthorizationStatusProvider
import io.github.wxxsfxyzm.intentx.domain.settings.repository.AppSettingsRepository
import io.github.wxxsfxyzm.intentx.executor.Authorizer
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthorizationViewModel(
    private val settings: AppSettingsRepository,
    private val statusProvider: AuthorizationStatusProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthorizationViewState())
    val uiState = _uiState.asStateFlow()
    private val events = Channel<AuthorizationViewEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()
    private var observeJob: Job? = null
    private var rootJob: Job? = null

    fun dispatch(action: AuthorizationViewAction) {
        when (action) {
            AuthorizationViewAction.StartObserving -> {
                if (observeJob?.isActive == true) return
                observeJob = viewModelScope.launch {
                    launch {
                        settings.authorizer.catch {
                            if (it !is IOException) throw it
                            Timber.e(it, "Unable to read preferred authorizer")
                            events.send(AuthorizationViewEvent.ShowMessage(R.string.settings_read_failed))
                        }.collect { authorizer -> _uiState.update { it.copy(selected = authorizer) } }
                    }
                    launch {
                        statusProvider.shizukuState.collect { state -> _uiState.update { it.copy(shizukuState = state) } }
                    }
                }
                refreshRoot()
            }

            AuthorizationViewAction.StopObserving -> {
                observeJob?.cancel()
                observeJob = null
                rootJob?.cancel()
                rootJob = null
            }

            is AuthorizationViewAction.Select -> viewModelScope.launch {
                Timber.d("Selecting authorizer: %s", action.authorizer)
                try {
                    settings.setAuthorizer(action.authorizer)
                } catch (error: IOException) {
                    Timber.e(error, "Unable to save preferred authorizer")
                    events.send(AuthorizationViewEvent.ShowMessage(R.string.settings_save_failed))
                    return@launch
                }
                when (action.authorizer) {
                    Authorizer.None -> Unit

                    Authorizer.Root, Authorizer.Auto -> refreshRoot()

                    Authorizer.Shizuku -> try {
                        statusProvider.requestShizukuPermission()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Timber.w(error, "Unable to request Shizuku permission")
                        events.send(AuthorizationViewEvent.ShowMessage(R.string.auth_request_failed))
                    }
                }
            }
        }
    }

    private fun refreshRoot() {
        if (rootJob?.isActive == true) return
        rootJob = viewModelScope.launch {
            Timber.d("Checking root availability")
            _uiState.update { it.copy(rootStatus = AuthorizationStatus.Checking, rootMode = null) }
            val mode = try {
                statusProvider.checkRoot()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Root is unavailable")
                null
            }
            _uiState.update {
                it.copy(
                    rootStatus = if (mode == null) AuthorizationStatus.Unavailable else AuthorizationStatus.Ready,
                    rootMode = mode,
                )
            }
            Timber.d("Root check completed: mode=%s", mode)
        }
    }
}
