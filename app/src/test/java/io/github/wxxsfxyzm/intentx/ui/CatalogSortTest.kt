// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui

import io.github.wxxsfxyzm.intentx.domain.catalog.CatalogSortOrder
import io.github.wxxsfxyzm.intentx.ui.page.main.catalog.CatalogItem
import io.github.wxxsfxyzm.intentx.ui.page.main.catalog.filterCatalogItems
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSortTest {
    @Test
    fun chineseLabelsUsePinyinForAppsAndComponents() = runTest {
        val items = listOf("支付宝", "微信", "京东", "百度").map {
            CatalogItem(it, it, it, 0L, exported = true)
        }
        listOf(true, false).forEach { appCatalog ->
            val result = filterCatalogItems(
                items = items,
                query = "",
                showSystem = true,
                hideOverlays = false,
                sortOrder = CatalogSortOrder.Label,
                reverseOrder = false,
                isAppCatalog = appCatalog,
            )
            assertEquals(listOf("百度", "京东", "微信", "支付宝"), result.map { it.label })
        }
    }

    @Test
    fun equalLabelsIgnoreCaseAndUseStablePackageTieBreak() = runTest {
        val items = listOf(CatalogItem("z", "z", "Alpha", 0L), CatalogItem("a", "a", "alpha", 0L))
        val result = filterCatalogItems(items, "", true, false, CatalogSortOrder.Label, false, true)
        assertEquals(listOf("a", "z"), result.map { it.id })
    }

    @Test
    fun defaultDirectionDependsOnSortOrderAndReverseFlipsIt() = runTest {
        val items = listOf(
            CatalogItem("a", "a", "Alpha", 0L, firstInstallTime = 100L),
            CatalogItem("z", "z", "Zulu", 0L, firstInstallTime = 200L),
        )
        CatalogSortOrder.entries.forEach { order ->
            val expected = if (order == CatalogSortOrder.FirstInstallTime) listOf("z", "a") else listOf("a", "z")
            listOf(false, true).forEach { reverse ->
                val result = filterCatalogItems(
                    items = items,
                    query = "",
                    showSystem = true,
                    hideOverlays = false,
                    sortOrder = order,
                    reverseOrder = reverse,
                    isAppCatalog = true,
                )
                assertEquals(if (reverse) expected.reversed() else expected, result.map { it.id })
            }
        }
    }
}
