package com.mariafonseca.financeanalytics.core.designsystem

import androidx.compose.ui.graphics.Color

// Keep in sync with android/app/src/main/res/values/colors.xml (brand_primary) --
// Compose can't reference XML color resources as compile-time constants.
// BrandColorConsistencyTest (androidTest) asserts the two stay in sync.
val BrandPrimary = Color(0xFF1B5E20)

// Values sourced from docs/project/05_DESIGN_SYSTEM.md — the approved palette
// extracted from the Finance Analytics Claude Design prototype. Do not invent
// new colours here; update the design system doc first if the palette changes.

val LightBackground = Color(0xFFF3F2F2)
val LightSurface = Color(0xFFEAE9E9)
val LightSurfaceElevated = Color(0xFFFFFFFF)
val LightText = Color(0xFF201E1D)
val LightTextSecondary = Color(0x9E201E1D) // rgba(32,30,29,0.62)
val LightDivider = Color(0x24201E1D) // rgba(32,30,29,0.14)
val LightDividerStrong = Color(0x66201E1D) // rgba(32,30,29,0.40)
val LightAccent = Color(0xFFEC3013)
val LightAccentDeep = Color(0xFFAE1800)
val LightAccentTint = Color(0xFFFFF2EF)
val LightPositive = Color(0xFF3F7D52)
val LightPositiveDeep = Color(0xFF2B5C3B)
val LightPositiveTint = Color(0xFFEAF3EC)
val LightWarningDeep = Color(0xFF8A5A06)
val LightWarningTint = Color(0xFFFBF1DE)
val LightErrorTint = Color(0xFFFFE0D9)

val DarkBackground = Color(0xFF131211)
val DarkSurface = Color(0xFF1E1C1B)
val DarkSurfaceElevated = Color(0xFF252322)
val DarkText = Color(0xFFF3F1F0)
val DarkTextSecondary = Color(0x9EF3F1F0) // rgba(243,241,240,0.62)
val DarkDivider = Color(0x29F3F1F0) // rgba(243,241,240,0.16)
val DarkDividerStrong = Color(0x52F3F1F0) // rgba(243,241,240,0.32)
val DarkAccent = Color(0xFFFF563C)
val DarkAccentDeep = Color(0xFFFFC4B8)
val DarkAccentTint = Color(0x29FF563C) // rgba(255,86,60,0.16)
val DarkPositive = Color(0xFF6FAE82)
val DarkPositiveDeep = Color(0xFF9CCAA9)
val DarkPositiveTint = Color(0x296FAE82) // rgba(111,174,130,0.16)
val DarkWarningDeep = Color(0xFFE0A53F)
val DarkWarningTint = Color(0x29E0A53F) // rgba(224,165,63,0.16)
val DarkErrorTint = Color(0x29FF563C) // rgba(255,86,60,0.16)
