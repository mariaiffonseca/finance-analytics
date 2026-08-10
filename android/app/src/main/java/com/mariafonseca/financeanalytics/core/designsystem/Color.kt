package com.mariafonseca.financeanalytics.core.designsystem

import androidx.compose.ui.graphics.Color

// Keep in sync with android/app/src/main/res/values/colors.xml (brand_primary) --
// Compose can't reference XML color resources as compile-time constants.
// BrandColorConsistencyTest (androidTest) asserts the two stay in sync.
val BrandPrimary = Color(0xFF1B5E20)

val Primary = BrandPrimary
val OnPrimary = Color(0xFFFFFFFF)
val Secondary = Color(0xFF33691E)
val OnSecondary = Color(0xFFFFFFFF)
val Background = Color(0xFFFFFBFE)
val OnBackground = Color(0xFF1C1B1F)
val Surface = Color(0xFFFFFBFE)
val OnSurface = Color(0xFF1C1B1F)

val DarkPrimary = Color(0xFF81C784)
val DarkOnPrimary = BrandPrimary
val DarkSecondary = Color(0xFFA5D6A7)
val DarkOnSecondary = BrandPrimary
val DarkBackground = Color(0xFF1C1B1F)
val DarkOnBackground = Color(0xFFE6E1E5)
val DarkSurface = Color(0xFF1C1B1F)
val DarkOnSurface = Color(0xFFE6E1E5)
