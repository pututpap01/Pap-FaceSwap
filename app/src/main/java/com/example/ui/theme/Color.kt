package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Frosted Glass Dark Obsidian Palette
val DarkBg = Color(0xFF0D0D0F)
val DarkSurface = Color(0xFF14151E)
val DarkSurfaceVariant = Color(0xFF1E202D)
val DarkCardBg = Color(0xFF181A26)

// Atmospheric Orb Glow Colors
val GlowIndigo = Color(0xFF4F46E5).copy(alpha = 0.22f)
val GlowPurple = Color(0xFF9333EA).copy(alpha = 0.18f)
val GlowCyan = Color(0xFF06B6D4).copy(alpha = 0.15f)

// Vibrant Frosted Glass Accents (Indigo to Purple)
val PrimaryIndigo = Color(0xFF6366F1)
val PrimaryPurple = Color(0xFFA855F7)
val PrimaryNeon = Color(0xFF6366F1) // Indigo Primary
val PrimaryGlow = Color(0xFFA5B4FC) // Indigo Light Glow
val SecondaryCyan = Color(0xFF38BDF8) // Crisp Sky Blue Accent
val CyanGlow = Color(0xFFA5B4FC) // Soft Highlight
val AccentPink = Color(0xFFE879F9) // Frosted Magenta / Violet
val AccentAmber = Color(0xFFFBBF24) // Warm Amber
val SuccessGreen = Color(0xFF22C55E) // Radiant Live Green

// Frosted Glass Translucencies & Borders
val BorderGlass = Color(0x2BFFFFFF) // 17% White border
val BorderGlassHighlight = Color(0x4DFFFFFF) // 30% White highlight border
val BorderGlassSubtle = Color(0x1AFFFFFF) // 10% White border
val SurfaceGlass = Color(0x17FFFFFF) // 9% White frosted fill
val SurfaceGlassSubtle = Color(0x0DFFFFFF) // 5% White subtle glass
val SurfaceGlassHeavy = Color(0x2EFFFFFF) // 18% White heavy glass
val SurfaceGlassDark = Color(0xCC0D0D0F) // 80% Dark background glass

// Text & Monochromes
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Frosted Glass Gradients
val GlassGradientPrimary = Brush.horizontalGradient(
    listOf(Color(0xFF4F46E5), Color(0xFF9333EA))
)

val GlassGradientCard = Brush.linearGradient(
    listOf(Color(0x24FFFFFF), Color(0x0DFFFFFF))
)

val GlassGradientBorder = Brush.linearGradient(
    listOf(Color(0x47FFFFFF), Color(0x14FFFFFF))
)

// Light Theme Alternatives (Crisp Frosted Glass)
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightPrimary = Color(0xFF4F46E5)
val LightSecondary = Color(0xFF0284C7)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)

