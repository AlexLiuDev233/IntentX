// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.data.catalog

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber

class AppIconLoader(private val context: Context) {
    private val permits = Semaphore(4)
    private val locks = Array(16) { Mutex() }
    private val cache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    suspend fun load(packageName: String, lastUpdateTime: Long): Bitmap? = withContext(Dispatchers.IO) {
        val config = context.resources.configuration
        val key = "$packageName:$lastUpdateTime:${config.densityDpi}:${config.uiMode}"
        cache.get(key)?.let { return@withContext it }
        locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            cache.get(key)?.let { return@withLock it }
            permits.withPermit {
                val bitmap = try {
                    context.packageManager.getApplicationIcon(packageName).toBitmap(144, 144)
                } catch (error: PackageManager.NameNotFoundException) {
                    Timber.d(error, "Application icon target no longer exists: %s", packageName)
                    null
                } catch (error: RuntimeException) {
                    Timber.w(error, "Application icon loading failed: %s", packageName)
                    null
                }
                currentCoroutineContext().ensureActive()
                if (bitmap != null) cache.put(key, bitmap)
                bitmap
            }
        }
    }
}
