package de.belkis.spyhound.ui

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.belkis.spyhound.R

object SpyColors {
    val Ink = de.belkis.spyhound.ui.design.SpyColors.Ink
    val Surface = de.belkis.spyhound.ui.design.SpyColors.Surface
    val SurfaceRaised = de.belkis.spyhound.ui.design.SpyColors.SurfaceRaised
    val Border = de.belkis.spyhound.ui.design.SpyColors.Border
    val Ivory = de.belkis.spyhound.ui.design.SpyColors.Ivory
    val IvoryMuted = de.belkis.spyhound.ui.design.SpyColors.IvoryMuted
    val Amber = de.belkis.spyhound.ui.design.SpyColors.Amber
    val Danger = de.belkis.spyhound.ui.design.SpyColors.Danger
    val Clear = de.belkis.spyhound.ui.design.SpyColors.Clear
}

private val Display = FontFamily(Font(R.font.space_grotesk, FontWeight.Bold))
private val Body = FontFamily(Font(R.font.inter, FontWeight.Normal), Font(R.font.inter, FontWeight.Medium))

private val SpyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
        color = SpyColors.Ivory,
    ),
    headlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.56).sp,
        color = SpyColors.Ivory,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp,
        color = SpyColors.Ivory,
    ),
    titleMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = SpyColors.Ivory,
    ),
    bodyLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = SpyColors.Ivory,
    ),
    bodyMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = SpyColors.IvoryMuted,
    ),
    labelLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
        color = SpyColors.Ivory,
    ),
    headlineSmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.15).sp,
        color = SpyColors.Ivory,
    ),
)

object SpyTypographyExtras {
    val sectionLabel = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.44.sp,
        color = SpyColors.IvoryMuted,
    )
    val tabularHeadline = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.56).sp,
        color = SpyColors.Ivory,
        fontFeatureSettings = "tnum",
    )
    val tabularMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        color = SpyColors.Ivory,
        fontFeatureSettings = "tnum",
    )
}

private val SpyColorScheme = darkColorScheme(
    primary = SpyColors.Amber,
    onPrimary = SpyColors.Ink,
    background = SpyColors.Ink,
    surface = SpyColors.Surface,
    onBackground = SpyColors.Ivory,
    onSurface = SpyColors.Ivory,
    outline = SpyColors.Border,
    error = SpyColors.Danger,
    tertiary = SpyColors.Clear,
)

val LocalMotionEnabled = staticCompositionLocalOf { true }

@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return !de.belkis.spyhound.ui.design.rememberReducedMotion()
}

@Composable
fun SpyhoundTheme(content: @Composable () -> Unit) {
    val motion = rememberMotionEnabled()
    CompositionLocalProvider(LocalMotionEnabled provides motion) {
        MaterialTheme(
            colorScheme = SpyColorScheme,
            typography = SpyTypography,
            content = content,
        )
    }
}

@Composable
fun SpyhoundScaffold(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val edge = if (LocalConfiguration.current.screenWidthDp >= 600) 32.dp else 20.dp
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpyColors.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(horizontal = edge, vertical = 12.dp),
            content = { content() },
        )
    }
}
