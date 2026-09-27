// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors
package io.github.wxxsfxyzm.intentx.data.catalog

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import io.github.wxxsfxyzm.intentx.domain.catalog.ActivityTarget
import io.github.wxxsfxyzm.intentx.domain.catalog.InstalledAppTarget
import io.github.wxxsfxyzm.intentx.domain.catalog.ReceiverTarget
import io.github.wxxsfxyzm.intentx.domain.catalog.SystemAppProvider
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class SystemAppProviderImpl(private val context: Context) : SystemAppProvider {
    private data class CachedLabel(val updated: Long, val source: String?, val label: String)

    private val labels = mutableMapOf<String, CachedLabel>()
    private val changedPackages = ConcurrentHashMap.newKeySet<String>()
    private val mutex = Mutex()
    private var configuration = ""

    override val packageChanges = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Timber.d("Package change received: action=%s, package=%s", intent?.action, intent?.data?.schemeSpecificPart)
                intent?.data?.schemeSpecificPart?.let { changedPackages.add(it) }
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate()

    override suspend fun getInstalledApps(): List<InstalledAppTarget> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val resources = context.resources.configuration
            val currentConfiguration = "${resources.locales.toLanguageTags()}:${resources.densityDpi}"
            if (configuration != currentConfiguration) {
                labels.clear()
                configuration = currentConfiguration
            }
            val pm = context.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }
            Timber.d("PackageManager returned %d installed packages", packages.size)
            val result = packages.mapNotNull { info ->
                currentCoroutineContext().ensureActive()
                val app = info.applicationInfo ?: return@mapNotNull null
                val cached = labels[info.packageName]
                val changed = changedPackages.remove(info.packageName)
                val label = if (!changed && cached?.updated == info.lastUpdateTime && cached.source == app.sourceDir) {
                    cached.label
                } else {
                    val loaded = try {
                        app.loadLabel(pm).toString().ifBlank { info.packageName }
                    } catch (error: RuntimeException) {
                        Timber.w(error, "Application label unavailable: package=%s", info.packageName)
                        info.packageName
                    }
                    labels[info.packageName] = CachedLabel(info.lastUpdateTime, app.sourceDir, loaded)
                    loaded
                }
                InstalledAppTarget(
                    info.packageName,
                    label,
                    info.versionName,
                    PackageInfoCompat.getLongVersionCode(info),
                    info.firstInstallTime,
                    info.lastUpdateTime,
                    app.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                    app.isResourceOverlay,
                )
            }
            labels.keys.retainAll(result.mapTo(hashSetOf()) { it.packageName })
            changedPackages.retainAll(labels.keys)
            result
        }
    }

    override suspend fun getActivities(packageName: String): List<ActivityTarget> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
        val info: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, flags)
        }
        mapActivities(pm, info)
    }

    override suspend fun getReceivers(packageName: String): List<ReceiverTarget> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = PackageManager.GET_RECEIVERS or PackageManager.MATCH_DISABLED_COMPONENTS
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, flags)
        }
        val app = info.applicationInfo
        val applicationEnabled = app != null && isApplicationEnabled(pm, app)
        info.receivers.orEmpty().distinctBy(ActivityInfo::name).map { receiver ->
            val label = try {
                receiver.loadLabel(pm).toString().ifBlank { receiver.name }
            } catch (error: RuntimeException) {
                Timber.w(error, "Receiver label unavailable: %s/%s", packageName, receiver.name)
                receiver.name
            }
            ReceiverTarget(
                packageName = info.packageName,
                className = receiver.name,
                label = label,
                exported = receiver.exported,
                enabled = applicationEnabled && isComponentEnabled(pm, receiver),
                permission = receiver.permission,
                lastUpdateTime = info.lastUpdateTime,
            )
        }
    }

    override suspend fun getAllActivities(): List<ActivityTarget> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }
        buildList {
            packages.forEach { info ->
                currentCoroutineContext().ensureActive()
                addAll(mapActivities(pm, info))
            }
        }
    }

    override suspend fun isActivityEnabled(packageName: String, className: String): Boolean? = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val component = ComponentName(packageName, className)
        val info = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getActivityInfo(component, PackageManager.ComponentInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getActivityInfo(component, PackageManager.MATCH_DISABLED_COMPONENTS)
            }
        } catch (error: PackageManager.NameNotFoundException) {
            Timber.w(error, "Activity enabled state unavailable: %s/%s", packageName, className)
            return@withContext null
        } catch (error: SecurityException) {
            Timber.w(error, "Activity enabled-state query denied: %s/%s", packageName, className)
            return@withContext null
        }
        isApplicationEnabled(pm, info.applicationInfo) && isComponentEnabled(pm, info)
    }

    override suspend fun isReceiverEnabled(packageName: String, className: String): Boolean? = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val info = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getReceiverInfo(ComponentName(packageName, className), PackageManager.ComponentInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getReceiverInfo(ComponentName(packageName, className), PackageManager.MATCH_DISABLED_COMPONENTS)
            }
        } catch (error: PackageManager.NameNotFoundException) {
            Timber.w(error, "Receiver enabled state unavailable: %s/%s", packageName, className)
            return@withContext null
        } catch (error: SecurityException) {
            Timber.w(error, "Receiver enabled-state query denied: %s/%s", packageName, className)
            return@withContext null
        }
        isApplicationEnabled(pm, info.applicationInfo) && isComponentEnabled(pm, info)
    }

    private fun isApplicationEnabled(pm: PackageManager, info: ApplicationInfo): Boolean = when (pm.getApplicationEnabledSetting(info.packageName)) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> info.enabled
        else -> false
    }

    private fun isComponentEnabled(pm: PackageManager, info: ActivityInfo): Boolean = when (pm.getComponentEnabledSetting(ComponentName(info.packageName, info.name))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> info.enabled
        else -> false
    }

    private fun mapActivities(pm: PackageManager, info: PackageInfo): List<ActivityTarget> {
        val packageName = info.packageName
        val applicationInfo = info.applicationInfo
        val applicationEnabled = applicationInfo != null && isApplicationEnabled(pm, applicationInfo)
        val isSystemApp = applicationInfo != null &&
            applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return info.activities.orEmpty().map { activity ->
            val label = try {
                activity.loadLabel(pm).toString().ifBlank { activity.name }
            } catch (error: RuntimeException) {
                Timber.w(error, "Activity label unavailable: %s/%s", info.packageName, activity.name)
                activity.name
            }
            val enabled = applicationEnabled && isComponentEnabled(pm, activity)
            ActivityTarget(
                packageName,
                activity.name,
                label,
                activity.exported,
                enabled,
                activity.permission,
                info.lastUpdateTime,
                isSystemApp,
                applicationInfo?.isResourceOverlay == true,
            )
        }
    }
}
