// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.executor.connection

import io.github.wxxsfxyzm.intentx.executor.Authorizer
import io.github.wxxsfxyzm.intentx.executor.DirectPrivilegedOperations
import java.io.Closeable

internal class PrivilegedConnection(
    val authorizer: Authorizer,
    val operations: DirectPrivilegedOperations,
    private val release: () -> Unit = {},
) : Closeable {
    override fun close() = release()
}
