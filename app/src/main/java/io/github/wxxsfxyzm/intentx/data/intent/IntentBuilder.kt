// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.data.intent

import android.content.Intent
import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation
import io.github.wxxsfxyzm.intentx.domain.intent.IntentSpec

interface IntentBuilder {
    fun build(spec: IntentSpec): Intent
    fun build(spec: IntentSpec, operation: IntentOperation): Intent
}
