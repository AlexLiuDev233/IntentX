// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.data.intent

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.wxxsfxyzm.intentx.domain.intent.ClipDataSpec
import io.github.wxxsfxyzm.intentx.domain.intent.ClipItemSpec
import io.github.wxxsfxyzm.intentx.domain.intent.ExtraSpec
import io.github.wxxsfxyzm.intentx.domain.intent.ExtraType
import io.github.wxxsfxyzm.intentx.domain.intent.IntentOperation
import io.github.wxxsfxyzm.intentx.domain.intent.IntentSpec
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class AndroidIntentBuilderTest {
    private val json = GlobalContext.get().get<Json>()

    @Test
    fun broadcastTargetsPackageOrReceiverWithoutResolvingLauncherActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val builder = AndroidIntentBuilder(context, json)
        val spec = IntentSpec("example.app", null, "example.ACTION", null, null, emptyList(), 0, emptyList(), null)
        val packageBroadcast = builder.build(spec, IntentOperation.Broadcast)
        assertEquals("example.app", packageBroadcast.`package`)
        assertEquals(null, packageBroadcast.component)
        assertEquals(0, packageBroadcast.flags)

        val explicit = builder.build(spec.copy(className = "example.app.Receiver", action = null), IntentOperation.Broadcast)
        assertEquals("example.app.Receiver", explicit.component?.className)
        assertThrows(IllegalArgumentException::class.java) {
            builder.build(spec.copy(className = null, action = null), IntentOperation.Broadcast)
        }
    }

    @Test
    fun requiresComponentRegardlessOfAction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spec = IntentSpec(
            packageName = null,
            className = null,
            action = Intent.ACTION_MAIN,
            dataUri = null,
            mimeType = null,
            categories = emptyList(),
            flags = 0,
            extras = emptyList(),
            clipData = null,
        )

        assertThrows(IllegalArgumentException::class.java) { AndroidIntentBuilder(context, json).build(spec) }
        assertThrows(IllegalArgumentException::class.java) {
            AndroidIntentBuilder(context, json).build(spec.copy(action = Intent.ACTION_VIEW, categories = listOf(Intent.CATEGORY_DEFAULT)))
        }
        val explicit = spec.copy(packageName = context.packageName, className = "${context.packageName}.MainActivity")
        assertEquals(Intent.ACTION_MAIN, AndroidIntentBuilder(context, json).build(explicit).action)
        assertEquals(null, AndroidIntentBuilder(context, json).build(explicit.copy(action = null)).action)
    }

    @Test
    fun buildsTypedExtrasAndClipData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = AndroidIntentBuilder(context, json).build(
            IntentSpec(
                packageName = context.packageName,
                className = "${context.packageName}.MainActivity",
                action = Intent.ACTION_VIEW,
                dataUri = "content://example/item",
                mimeType = "text/plain",
                categories = listOf(Intent.CATEGORY_DEFAULT),
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION,
                extras = listOf(
                    ExtraSpec("number", ExtraType.Int, "42"),
                    ExtraSpec("enabled", ExtraType.Boolean, "true"),
                    ExtraSpec("names", ExtraType.StringArray, "one\ntwo"),
                    ExtraSpec("nested", ExtraType.Bundle, "{\"name\":\"value\",\"count\":2}"),
                ),
                clipData = ClipDataSpec(
                    label = "Selection",
                    mimeTypes = emptyList(),
                    items = listOf(
                        ClipItemSpec("hello", "<b>hello</b>", "content://example/clip", null),
                        ClipItemSpec(null, null, null, "intent:#Intent;action=example.ACTION;end"),
                    ),
                ),
            ),
        )

        assertEquals(42, intent.getIntExtra("number", -1))
        assertTrue(intent.getBooleanExtra("enabled", false))
        assertEquals(listOf("one", "two"), intent.getStringArrayExtra("names")?.toList())
        assertEquals("value", intent.getBundleExtra("nested")?.getString("name"))
        assertEquals(2L, intent.getBundleExtra("nested")?.getLong("count"))
        assertEquals("content://example/item", intent.dataString)
        assertEquals("text/plain", intent.type)
        assertEquals(2, intent.clipData?.itemCount)
        assertEquals("<b>hello</b>", intent.clipData?.getItemAt(0)?.htmlText)
        assertEquals("content://example/clip", intent.clipData?.getItemAt(0)?.uri?.toString())
        assertNotNull(intent.clipData?.getItemAt(1)?.intent)
    }
}
