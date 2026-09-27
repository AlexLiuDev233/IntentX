// SPDX-License-Identifier: GPL-3.0
// Copyright (C) 2026 IntentX contributors

package io.github.wxxsfxyzm.intentx.ui.theme.material

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PureBlackColorSchemeTest {
    @Test
    fun pureBlackOnlyAppliesToDarkTheme() {
        val light = lightColorScheme()
        val dark = darkColorScheme()
        assertSame(light, light.withPureBlackBackground(isDark = false, enabled = true))
        assertSame(dark, dark.withPureBlackBackground(isDark = true, enabled = false))
    }

    @Test
    fun darkBackgroundBecomesBlackWhileCardsAndForegroundsKeepTheirColors() {
        val original = darkColorScheme()
        val black = original.withPureBlackBackground(isDark = true, enabled = true)

        assertEquals(Color.Black, black.background)
        assertEquals(Color.Black, black.surface)
        assertEquals(Color.Black, black.surfaceDim)
        assertEquals(Color.Black, black.surfaceContainer)
        assertEquals(Color.Black, black.surfaceContainerLowest)
        assertEquals(original.surfaceBright, black.surfaceBright)
        assertEquals(original.surfaceContainerLow, black.surfaceContainerLow)
        assertEquals(original.surfaceContainerHigh, black.surfaceContainerHigh)
        assertEquals(original.surfaceContainerHighest, black.surfaceContainerHighest)
        assertEquals(original.onSurface, black.onSurface)
        assertEquals(original.onBackground, black.onBackground)
        assertEquals(original.primary, black.primary)
    }
}
