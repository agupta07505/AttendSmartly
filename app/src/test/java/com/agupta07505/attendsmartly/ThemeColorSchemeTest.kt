/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly

import androidx.compose.ui.graphics.Color
import com.agupta07505.attendsmartly.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class ThemeColorSchemeTest {

    @Test
    fun testParseColorHexValid() {
        val color1 = parseColorHex("#6750A4")
        assertNotNull(color1)

        val color2 = parseColorHex("00639B")
        assertNotNull(color2)

        val color3 = parseColorHex("#FF006C51")
        assertNotNull(color3)

        val color4 = parseColorHex("#FFF")
        assertNotNull(color4)
    }

    @Test
    fun testParseColorHexInvalid() {
        assertNull(parseColorHex(null))
        assertNull(parseColorHex(""))
        assertNull(parseColorHex("   "))
        assertNull(parseColorHex("XYZ123"))
        assertNull(parseColorHex("#12"))
    }

    @Test
    fun testHslColorCalculation() {
        val color = hslColor(240f, 1f, 0.5f) // Pure Blue
        assertNotNull(color)
    }

    @Test
    fun testGenerateColorSchemeFromSeedLight() {
        val seed = Color(0xFF00639B)
        val scheme = generateColorSchemeFromSeed(seed, darkTheme = false)

        assertNotNull(scheme)
        assertNotNull(scheme.primary)
        assertNotNull(scheme.primaryContainer)
        assertNotNull(scheme.secondary)
        assertNotNull(scheme.secondaryContainer)
        assertNotNull(scheme.tertiary)
        assertNotNull(scheme.surface)
        assertNotNull(scheme.onSurface)
    }

    @Test
    fun testGenerateColorSchemeFromSeedDark() {
        val seed = Color(0xFF006C51)
        val scheme = generateColorSchemeFromSeed(seed, darkTheme = true)

        assertNotNull(scheme)
        assertNotNull(scheme.primary)
        assertNotNull(scheme.primaryContainer)
        assertNotNull(scheme.secondary)
        assertNotNull(scheme.secondaryContainer)
        assertNotNull(scheme.tertiary)
        assertNotNull(scheme.surface)
        assertNotNull(scheme.onSurface)
    }

    @Test
    fun testPresetThemeColorsContainValidHexes() {
        assertTrue(PresetThemeColors.isNotEmpty())
        for (preset in PresetThemeColors) {
            val parsed = parseColorHex(preset.hex)
            assertNotNull("Preset ${preset.name} (${preset.hex}) must be a valid hex", parsed)
        }
    }
}
