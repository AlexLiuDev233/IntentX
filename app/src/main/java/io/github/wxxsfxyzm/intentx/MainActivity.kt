// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.wxxsfxyzm.intentx.ui.AppViewModel
import io.github.wxxsfxyzm.intentx.ui.AuthorizationUi
import io.github.wxxsfxyzm.intentx.ui.IntentXRoot
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization.AuthorizationViewAction
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization.AuthorizationViewEvent
import io.github.wxxsfxyzm.intentx.ui.page.main.settings.preferred.authorization.AuthorizationViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModel()
    private val authorization: AuthorizationViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var themeLoaded = false
        splashScreen.setKeepOnScreenCondition { !themeLoaded }
        super.onCreate(savedInstanceState)
        Timber.d("Main Activity created; restoring=%s", savedInstanceState != null)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                Timber.d("Starting foreground authorization observation")
                authorization.dispatch(AuthorizationViewAction.StartObserving)
                try {
                    awaitCancellation()
                } finally {
                    Timber.d("Stopping foreground authorization observation")
                    authorization.dispatch(AuthorizationViewAction.StopObserving)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authorization.eventFlow.collect { event ->
                    when (event) {
                        is AuthorizationViewEvent.ShowMessage -> Toast.makeText(
                            this@MainActivity,
                            event.message,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
        setContent {
            val themeState by appViewModel.uiState.collectAsStateWithLifecycle()
            val state by authorization.uiState.collectAsStateWithLifecycle()
            IntentXRoot(
                state = themeState,
                authorization = AuthorizationUi(state, authorization::dispatch),
                onThemeLoaded = {
                    if (!themeLoaded) Timber.d("Theme loaded; releasing splash screen")
                    themeLoaded = true
                },
            )
        }
    }
}
