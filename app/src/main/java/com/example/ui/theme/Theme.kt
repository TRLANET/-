package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TenDarkColorScheme = darkColorScheme(
  primary = TenPureWhite,
  onPrimary = TenVoidBlack,
  primaryContainer = TenElevated,
  onPrimaryContainer = TenPureWhite,
  secondary = TenIce,
  onSecondary = TenVoidBlack,
  secondaryContainer = TenIceDark,
  onSecondaryContainer = TenPureWhite,
  tertiary = TenCrimson,
  onTertiary = TenPureWhite,
  background = TenObsidian,
  onBackground = TenPureWhite,
  surface = TenCharcoal,
  onSurface = TenPureWhite,
  surfaceVariant = TenSlate,
  onSurfaceVariant = TenSilver,
  surfaceContainerLowest = TenObsidian,
  surfaceContainerLow = TenCharcoal,
  surfaceContainer = TenSlate,
  surfaceContainerHigh = TenElevated,
  outline = TenMuted,
  outlineVariant = TenBorder,
  error = TenCrimson,
  onError = TenPureWhite,
)

private val TenLightColorScheme = lightColorScheme(
  primary = TenLightInk,
  onPrimary = TenLightSurface,
  primaryContainer = TenLightCard,
  onPrimaryContainer = TenLightInk,
  secondary = TenIceDark,
  onSecondary = TenLightSurface,
  tertiary = TenCrimson,
  onTertiary = TenLightSurface,
  background = TenLightBg,
  onBackground = TenLightInk,
  surface = TenLightSurface,
  onSurface = TenLightInk,
  surfaceVariant = TenLightCard,
  onSurfaceVariant = TenLightSecondary,
  outline = TenLightSecondary,
  outlineVariant = TenLightBorder,
  error = TenCrimson,
  onError = TenLightSurface,
)

val TenShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TenTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) TenDarkColorScheme else TenLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = TenTypography,
    shapes = TenShapes,
    content = content,
  )
}
