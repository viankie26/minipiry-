package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Exact "Paper & Ink" — Swiss Editorial color system ported from `viankie26/baozhiqi/src/styles.css`.
 * Warm off-white paper (`#F3F1EC`), deep ink (`#0E0E0E`), crisp hairlines (`#D2CEC5`),
 * editorial crimson expired (`#A82417`), and warm ochre soon (`#9C6615`).
 */
@Immutable
data class PaperInkPalette(
  val warmBg: Color,
  val surface: Color,
  val foreground: Color,
  val primaryForeground: Color,
  val muted: Color,
  val mutedForeground: Color,
  val border: Color,
  val rule: Color,
  val expired: Color,
  val expiredSoft: Color,
  val soon: Color,
  val soonSoft: Color,
  val statusBadgeText: Color,
)

val LightPaperInkPalette = PaperInkPalette(
  warmBg = Color(0xFFF3F1EC),
  surface = Color(0xFFFAF9F5),
  foreground = Color(0xFF0E0E0E),
  primaryForeground = Color(0xFFF6F4F0),
  muted = Color(0xFFE5E2DA),
  mutedForeground = Color(0xFF5C5A55),
  border = Color(0xFFD2CEC5),
  rule = Color(0xFF0E0E0E),
  expired = Color(0xFFA82417),
  expiredSoft = Color(0xFFF6E3DE),
  soon = Color(0xFF9C6615),
  soonSoft = Color(0xFFF5EAD0),
  statusBadgeText = Color(0xFFFAF9F5),
)

val DarkPaperInkPalette = PaperInkPalette(
  warmBg = Color(0xFF0E0E0E),
  surface = Color(0xFF171717),
  foreground = Color(0xFFF1EFEA),
  primaryForeground = Color(0xFF0E0E0E),
  muted = Color(0xFF242424),
  mutedForeground = Color(0xFFA6A49F),
  border = Color(0x2EFFFFFF),
  rule = Color(0xFFF1EFEA),
  expired = Color(0xFFE96D5B),
  expiredSoft = Color(0xFF2E1714),
  soon = Color(0xFFE3B15A),
  soonSoft = Color(0xFF2B2212),
  statusBadgeText = Color(0xFF0E0E0E),
)

val LocalPaperInkPalette = staticCompositionLocalOf { LightPaperInkPalette }
