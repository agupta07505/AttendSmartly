/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.attendsmartly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

fun rgbToHsl(r: Float, g: Float, b: Float): FloatArray {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    var h = 0f
    val l = (max + min) / 2f
    val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))

    if (delta != 0f) {
        h = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        } * 60f
        if (h < 0f) h += 360f
    }

    return floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

fun hslToColor(h: Float, s: Float, l: Float): Color {
    val clampedH = ((h % 360f) + 360f) % 360f
    val clampedS = s.coerceIn(0f, 1f)
    val clampedL = l.coerceIn(0f, 1f)

    val c = (1f - abs(2f * clampedL - 1f)) * clampedS
    val x = c * (1f - abs((clampedH / 60f) % 2f - 1f))
    val m = clampedL - c / 2f

    val (rPrime, gPrime, bPrime) = when {
        clampedH < 60f -> Triple(c, x, 0f)
        clampedH < 120f -> Triple(x, c, 0f)
        clampedH < 180f -> Triple(0f, c, x)
        clampedH < 240f -> Triple(0f, x, c)
        clampedH < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val r = ((rPrime + m) * 255f).toInt().coerceIn(0, 255)
    val g = ((gPrime + m) * 255f).toInt().coerceIn(0, 255)
    val b = ((bPrime + m) * 255f).toInt().coerceIn(0, 255)

    return Color(0xFF000000.toInt() or (r shl 16) or (g shl 8) or b)
}

fun hslColor(h: Float, s: Float, l: Float): Color = hslToColor(h, s, l)

fun generateColorSchemeFromSeed(seedColor: Color, darkTheme: Boolean): ColorScheme {
    val hsl = rgbToHsl(seedColor.red, seedColor.green, seedColor.blue)
    val h = hsl[0]
    val s = hsl[1].coerceIn(0.2f, 0.95f)

    val secH = (h + 15f) % 360f
    val secS = (s * 0.45f).coerceIn(0.15f, 0.55f)

    val tertH = (h + 60f) % 360f
    val tertS = (s * 0.65f).coerceIn(0.25f, 0.75f)

    return if (darkTheme) {
        darkColorScheme(
            primary = hslColor(h, s * 0.85f, 0.80f),
            onPrimary = hslColor(h, s, 0.20f),
            primaryContainer = hslColor(h, s, 0.32f),
            onPrimaryContainer = hslColor(h, s * 0.6f, 0.90f),

            secondary = hslColor(secH, secS, 0.75f),
            onSecondary = hslColor(secH, secS, 0.20f),
            secondaryContainer = hslColor(secH, secS, 0.30f),
            onSecondaryContainer = hslColor(secH, secS * 0.6f, 0.90f),

            tertiary = hslColor(tertH, tertS, 0.75f),
            onTertiary = hslColor(tertH, tertS, 0.20f),
            tertiaryContainer = hslColor(tertH, tertS, 0.30f),
            onTertiaryContainer = hslColor(tertH, tertS * 0.6f, 0.90f),

            surface = hslColor(h, 0.08f, 0.08f),
            onSurface = hslColor(h, 0.04f, 0.92f),
            surfaceVariant = hslColor(h, 0.12f, 0.18f),
            onSurfaceVariant = hslColor(h, 0.08f, 0.78f),
            outline = hslColor(h, 0.08f, 0.55f),
            outlineVariant = hslColor(h, 0.08f, 0.30f)
        )
    } else {
        lightColorScheme(
            primary = hslColor(h, s, 0.40f),
            onPrimary = Color.White,
            primaryContainer = hslColor(h, s * 0.75f, 0.90f),
            onPrimaryContainer = hslColor(h, s, 0.15f),

            secondary = hslColor(secH, secS, 0.45f),
            onSecondary = Color.White,
            secondaryContainer = hslColor(secH, secS * 0.6f, 0.92f),
            onSecondaryContainer = hslColor(secH, secS, 0.15f),

            tertiary = hslColor(tertH, tertS, 0.45f),
            onTertiary = Color.White,
            tertiaryContainer = hslColor(tertH, tertS * 0.6f, 0.92f),
            onTertiaryContainer = hslColor(tertH, tertS, 0.15f),

            surface = hslColor(h, 0.04f, 0.98f),
            onSurface = hslColor(h, 0.08f, 0.12f),
            surfaceVariant = hslColor(h, 0.10f, 0.92f),
            onSurfaceVariant = hslColor(h, 0.08f, 0.30f),
            outline = hslColor(h, 0.08f, 0.60f),
            outlineVariant = hslColor(h, 0.06f, 0.85f)
        )
    }
}

fun parseColorHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        val clean = hex.trim().removePrefix("#")
        val argb = when (clean.length) {
            6 -> (0xFF000000.toLong() or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            3 -> {
                val r = clean.substring(0, 1).repeat(2)
                val g = clean.substring(1, 2).repeat(2)
                val b = clean.substring(2, 3).repeat(2)
                (0xFF000000.toLong() or "$r$g$b".toLong(16)).toInt()
            }
            else -> null
        }
        argb?.let { Color(it) }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun AttendSmartlyTheme(
    themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    dynamicColor: Boolean = true,
    themeColorStyle: String = "DYNAMIC", // "DYNAMIC", "DEFAULT", "CUSTOM"
    customColorHex: String = "#6750A4",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val parsedCustomColor = remember(customColorHex) { parseColorHex(customColorHex) }

    val colorScheme = when {
        themeColorStyle == "CUSTOM" && parsedCustomColor != null -> {
            generateColorSchemeFromSeed(parsedCustomColor, darkTheme)
        }
        themeColorStyle == "DEFAULT" -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        parsedCustomColor != null && themeColorStyle == "CUSTOM" -> {
            generateColorSchemeFromSeed(parsedCustomColor, darkTheme)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
