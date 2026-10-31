package com.lilac.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LilacColors = darkColorScheme(
    primary = Lilac.Neon,
    secondary = Lilac.Violet,
    background = Lilac.Background,
    surface = Lilac.Surface,
    surfaceVariant = Lilac.SurfaceElevated,
    outline = Lilac.Border,
    onPrimary = Lilac.Background,
    onBackground = Lilac.TextPrimary,
    onSurface = Lilac.TextPrimary,
    onSurfaceVariant = Lilac.TextSecondary,
)

// Roboto del sistema (sans-serif): cero costo extra de memoria o APK.
private val LilacTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 76.sp,
        letterSpacing = (-2).sp,
        fontFeatureSettings = "tnum",
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
    ),
)

@Composable
fun LilacTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LilacColors,
        typography = LilacTypography,
        content = content,
    )
}
