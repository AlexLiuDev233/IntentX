// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.data.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemAppProviderDeviceTest {
    @Test
    fun installedAppsIncludesOtherPackagesAfterPermissionGrant() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val apps = SystemAppProviderImpl(context).getInstalledApps()
        assertTrue("Only ${apps.size} package(s) are visible", apps.size > 1)
        assertTrue(apps.any { it.packageName != context.packageName })
    }

    @Test
    fun allActivitiesCanBeIndexedFromInstalledPackages() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activities = SystemAppProviderImpl(context).getAllActivities()
        assertTrue("No declared activities were indexed", activities.isNotEmpty())
        assertTrue(activities.all { it.packageName.isNotBlank() && it.className.isNotBlank() })
    }
}
