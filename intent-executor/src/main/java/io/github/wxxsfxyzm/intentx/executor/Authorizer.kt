// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.executor

// Keep existing ordinals stable for editor drafts.
enum class Authorizer {
    None,
    Root,
    Shizuku,
    Auto,
    ;

    val requiresNewTask: Boolean get() = this == None || this == Auto
}
