package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val KonozDarkColorScheme = darkColorScheme(
    primary = AntiqueGold,
    onPrimary = AntiqueGoldDark,
    primaryContainer = AntiqueGoldContainer,
    onPrimaryContainer = ParchmentInkLight,
    secondary = WarmBronze,
    onSecondary = ObsidianManuscript,
    secondaryContainer = WarmBronzeContainer,
    onSecondaryContainer = ParchmentInkLight,
    tertiary = SandstoneSage,
    onTertiary = ObsidianManuscript,
    tertiaryContainer = SandstoneSageContainer,
    onTertiaryContainer = ParchmentInkLight,
    background = ObsidianManuscript,
    onBackground = ParchmentInkLight,
    surface = BasaltSurface,
    onSurface = ParchmentInkLight,
    surfaceVariant = ElevatedParchmentCard,
    onSurfaceVariant = MutedStoneText,
    surfaceContainerLowest = ObsidianManuscript,
    surfaceContainerLow = BasaltSurface,
    surfaceContainer = ElevatedParchmentCard,
    surfaceContainerHigh = StoneContainerHigh,
    outline = BrassOutline,
    outlineVariant = BrassOutlineSubtle,
    error = HazardCrimson,
    onError = OnHazardCrimson,
    errorContainer = HazardCrimsonContainer,
    onErrorContainer = OnHazardCrimson
)

private val KonozLightColorScheme = lightColorScheme(
    primary = DeepBronzePrimary,
    onPrimary = ParchmentLightCard,
    primaryContainer = DeepBronzeContainer,
    onPrimaryContainer = ParchmentDarkInk,
    secondary = WarmBronze,
    onSecondary = ParchmentLightCard,
    secondaryContainer = ParchmentLightSurface,
    onSecondaryContainer = ParchmentDarkInk,
    tertiary = SandstoneSage,
    onTertiary = ParchmentLightCard,
    background = ParchmentLightBg,
    onBackground = ParchmentDarkInk,
    surface = ParchmentLightSurface,
    onSurface = ParchmentDarkInk,
    surfaceVariant = ParchmentLightCard,
    onSurfaceVariant = ParchmentMutedInk,
    surfaceContainer = ParchmentLightCard,
    surfaceContainerHigh = ParchmentLightSurface,
    outline = LightBrassOutline,
    outlineVariant = LightBrassOutline,
    error = HazardCrimson,
    onError = ParchmentLightCard,
    errorContainer = Color(0xFFFFDAD5),
    onErrorContainer = Color(0xFF410E0B)
)

private val KonozShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KonozTheme(
    darkTheme: Boolean = true,
    isRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KonozDarkColorScheme else KonozLightColorScheme
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KonozTypography,
            shapes = KonozShapes,
            content = content
        )
    }
}
