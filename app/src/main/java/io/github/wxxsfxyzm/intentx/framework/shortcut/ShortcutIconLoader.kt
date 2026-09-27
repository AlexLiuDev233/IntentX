// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.framework.shortcut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import java.io.InputStream
import timber.log.Timber

internal class ShortcutIconLoader(private val context: Context) {
    fun load(iconUri: String?, packageName: String?): IconCompat {
        if (!iconUri.isNullOrBlank()) return IconCompat.createWithBitmap(loadBitmap(iconUri.toUri()))
        if (!packageName.isNullOrBlank()) {
            try {
                val icon = context.packageManager.getApplicationIcon(packageName).toBitmap(192, 192)
                return IconCompat.createWithBitmap(icon)
            } catch (error: Exception) {
                Timber.w(error, "Shortcut target icon unavailable; using IntentX icon: package=%s", packageName)
                // The target app may have been removed since the editor opened.
            }
        }
        return IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon)
    }

    fun loadBitmap(uri: Uri): Bitmap {
        Timber.d("Loading custom shortcut icon: scheme=%s, provider=%s", uri.scheme, uri.authority)
        require(uri.scheme == "content" || uri.scheme == "android.resource") { "Use a content or android.resource icon URI" }
        return decodeBitmap { context.contentResolver.openInputStream(uri) }
    }

    internal fun decodeBitmap(openInput: () -> InputStream?): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openInput()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Cannot read shortcut icon" }
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / inSampleSize > 512) inSampleSize *= 2
        }
        val bitmap = openInput()?.use { BitmapFactory.decodeStream(it, null, options) }
        requireNotNull(bitmap) { "Cannot decode shortcut icon" }
        val side = minOf(bitmap.width, bitmap.height)
        val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        if (square !== bitmap) bitmap.recycle()
        Timber.d(
            "Shortcut icon decoded: source=%dx%d, sample=%d, output=%dx%d",
            bounds.outWidth,
            bounds.outHeight,
            options.inSampleSize,
            square.width,
            square.height,
        )
        return square
    }
}
