package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Exact typography pairing from `viankie26/baozhiqi/src/styles.css`:
 * - Display (`--font-display`): Archivo Black (falls back cleanly to heavy CJK bold for Chinese)
 * - Sans (`--font-sans`): Hind
 */
val ArchivoBlackFamily = FontFamily(
  Font(R.font.archivo_black, FontWeight.Normal),
  Font(R.font.archivo_black, FontWeight.Bold),
  Font(R.font.archivo_black, FontWeight.Black),
)

val HindFamily = FontFamily(
  Font(R.font.hind, FontWeight.Normal),
  Font(R.font.hind, FontWeight.Medium),
  Font(R.font.hind, FontWeight.SemiBold),
  Font(R.font.hind, FontWeight.Bold),
)

/**
 * Matches `.label-kicker` in `baozhiqi/src/styles.css`:
 * `font-size: 0.625rem (10sp); font-weight: 600; letter-spacing: 0.18em; text-transform: uppercase;`
 */
val EditorialKickerStyle = TextStyle(
  fontFamily = HindFamily,
  fontWeight = FontWeight.SemiBold,
  fontSize = 10.5.sp,
  lineHeight = 14.sp,
  letterSpacing = 1.6.sp,
)

/**
 * Giant tabular countdown numeral on the right of `ItemCard` (`text-[2.5rem] leading-none font-display`).
 */
val EditorialCountdownStyle = TextStyle(
  fontFamily = ArchivoBlackFamily,
  fontWeight = FontWeight.Black,
  fontSize = 38.sp,
  lineHeight = 38.sp,
  letterSpacing = (-0.8).sp,
  fontFeatureSettings = "tnum",
)

/**
 * Stat strip numeral (`text-[2rem] leading-none font-display tabular-nums`).
 */
val EditorialStatNumeralStyle = TextStyle(
  fontFamily = ArchivoBlackFamily,
  fontWeight = FontWeight.Black,
  fontSize = 32.sp,
  lineHeight = 34.sp,
  letterSpacing = (-0.5).sp,
  fontFeatureSettings = "tnum",
)

val Typography = Typography(
  displayMedium = TextStyle(
    fontFamily = ArchivoBlackFamily,
    fontWeight = FontWeight.Black,
    fontSize = 36.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.6).sp,
  ),
  displaySmall = TextStyle(
    fontFamily = ArchivoBlackFamily,
    fontWeight = FontWeight.Black,
    fontSize = 32.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.5).sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = ArchivoBlackFamily,
    fontWeight = FontWeight.Black,
    fontSize = 28.sp,
    lineHeight = 31.sp,
    letterSpacing = (-0.4).sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = ArchivoBlackFamily,
    fontWeight = FontWeight.Black,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.3).sp,
  ),
  titleLarge = TextStyle(
    fontFamily = ArchivoBlackFamily,
    fontWeight = FontWeight.Black,
    fontSize = 18.sp,
    lineHeight = 23.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = HindFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  titleSmall = TextStyle(
    fontFamily = HindFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = HindFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = HindFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = HindFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = HindFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = HindFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
  labelSmall = EditorialKickerStyle,
)
