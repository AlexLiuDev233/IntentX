// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.executor

import android.content.Intent

interface IntentExecutor {
    suspend fun startActivity(authorizer: Authorizer, intent: Intent): Boolean
    suspend fun sendBroadcast(authorizer: Authorizer, intent: Intent): Boolean
}
