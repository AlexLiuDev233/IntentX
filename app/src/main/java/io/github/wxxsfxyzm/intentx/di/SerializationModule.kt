// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.di

import kotlinx.serialization.json.Json
import org.koin.dsl.module

val serializationModule = module {
    single { Json { ignoreUnknownKeys = true } }
}
