// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.framework.shortcut

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShortcutIconLoaderTest {
    @Test
    fun decodesSmallAndLargeImagesWithoutZeroSamplingAndCropsToSquare() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = ShortcutIconLoader(context)
        for ((width, height) in listOf(48 to 32, 2048 to 1024)) {
            val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val bytes = ByteArrayOutputStream().use { output ->
                assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
            source.recycle()
            val decoded = loader.decodeBitmap { bytes.inputStream() }
            assertEquals(decoded.width, decoded.height)
            assertTrue(decoded.width in 1..512)
            if (width < 512) assertEquals(height, decoded.width)
            decoded.recycle()
        }
    }
}
