// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.domain.shortcut

import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation
import io.github.wxxsfxyzm.intentx.domain.intent.IntentSpec
import io.github.wxxsfxyzm.intentx.executor.Authorizer
import kotlinx.serialization.Serializable

/** A private executable snapshot; desktop intents only carry its ID and capability token. */
@Serializable
data class IntentShortcut(
    val id: String,
    val token: String,
    val name: String,
    val intent: IntentSpec,
    val operation: IntentOperation,
    val authorizer: Authorizer,
    val version: Int = 1,
)
