// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.di

import io.github.wxxsfxyzm.intentx.data.catalog.AppIconLoader
import io.github.wxxsfxyzm.intentx.data.catalog.SystemAppProviderImpl
import io.github.wxxsfxyzm.intentx.domain.catalog.SystemAppProvider
import org.koin.dsl.module

val catalogModule = module {
    single<SystemAppProvider> { SystemAppProviderImpl(get()) }
    single { AppIconLoader(get()) }
}
