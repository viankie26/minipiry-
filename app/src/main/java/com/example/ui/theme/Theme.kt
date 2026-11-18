package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val PaperInkLightColorScheme = lightColorScheme(
  primary = LightPaperInkPalette.foreground,
  onPrimary = LightPaperInkPalette.primaryForeground,
  primaryContainer = LightPaperInkPalette.muted,
  onPrimaryContainer = LightPaperInkPalette.foreground,
  secondary = LightPaperInkPalette.muted,
  onSecondary = LightPaperInkPalette.foreground,
  secondaryContainer = LightPaperInkPalette.muted,
  onSecondaryContainer = LightPaperInkPalette.foreground,
  background = LightPaperInkPalette.warmBg,
  onBackground = LightPaperInkPalette.foreground,
  surface = LightPaperInkPalette.surface,
  onSurface = LightPaperInkPalette.foreground,
  surfaceVariant = LightPaperInkPalette.muted,
  onSurfaceVariant = LightPaperInkPalette.mutedForeground,
  surfaceContainerLow = LightPaperInkPalette.surface,
  surfaceContainer = LightPaperInkPalette.muted,
  outline = LightPaperInkPalette.border,
  outlineVariant = LightPaperInkPalette.border,
  error = LightPaperInkPalette.expired,
  onError = LightPaperInkPalette.statusBadgeText,
  errorContainer = LightPaperInkPalette.expiredSoft,
  onErrorContainer = LightPaperInkPalette.expired,
)

private val PaperInkDarkColorScheme = darkColorScheme(
  primary = DarkPaperInkPalette.foreground,
  onPrimary = DarkPaperInkPalette.primaryForeground,
  primaryContainer = DarkPaperInkPalette.muted,
  onPrimaryContainer = DarkPaperInkPalette.foreground,
  secondary = DarkPaperInkPalette.muted,
  onSecondary = DarkPaperInkPalette.foreground,
  secondaryContainer = DarkPaperInkPalette.muted,
  onSecondaryContainer = DarkPaperInkPalette.foreground,
  background = DarkPaperInkPalette.warmBg,
  onBackground = DarkPaperInkPalette.foreground,
  surface = DarkPaperInkPalette.surface,
  onSurface = DarkPaperInkPalette.foreground,
  surfaceVariant = DarkPaperInkPalette.muted,
  onSurfaceVariant = DarkPaperInkPalette.mutedForeground,
  surfaceContainerLow = DarkPaperInkPalette.surface,
  surfaceContainer = DarkPaperInkPalette.muted,
  outline = DarkPaperInkPalette.border,
  outlineVariant = DarkPaperInkPalette.border,
  error = DarkPaperInkPalette.expired,
  onError = DarkPaperInkPalette.statusBadgeText,
  errorContainer = DarkPaperInkPalette.expiredSoft,
  onErrorContainer = DarkPaperInkPalette.expired,
)

/**
 * Matches `--radius: 0px` in `viankie26/baozhiqi/src/styles.css` — sharp Swiss editorial corners.
 */
val PaperInkShapes = Shapes(
  extraSmall = RoundedCornerShape(0.dp),
  small = RoundedCornerShape(0.dp),
  medium = RoundedCornerShape(0.dp),
  large = RoundedCornerShape(0.dp),
  extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun MinipiryTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val palette = if (darkTheme) DarkPaperInkPalette else LightPaperInkPalette
  val colorScheme = if (darkTheme) PaperInkDarkColorScheme else PaperInkLightColorScheme

  CompositionLocalProvider(LocalPaperInkPalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = PaperInkShapes,
      content = content,
    )
  }
}
