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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.belkis.spyhound.R

object SpyColors {
    val Ink = Color(0xFF0A0E14)
    val Surface = Color(0xFF121820)
    val SurfaceRaised = Color(0xFF1A2230)
    val Border = Color(0xFF2A3344)
    val Ivory = Color(0xFFF2EBE0)
    val IvoryMuted = Color(0xFFB8B0A6)
    val Amber = Color(0xFFFFB547)
    val Danger = Color(0xFFE85D5D)
    val Clear = Color(0xFF3DBEA7)
}

private val Display = FontFamily(Font(R.font.space_grotesk, FontWeight.Bold))
private val Body = FontFamily(Font(R.font.inter, FontWeight.Normal), Font(R.font.inter, FontWeight.Medium))

private val SpyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
        color = SpyColors.Ivory,
    ),
    headlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
        color = SpyColors.Ivory,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
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
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.15).sp,
        color = SpyColors.Ivory,
    ),
)

object SpyTypographyExtras {
    val sectionLabel = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.2.sp,
        color = SpyColors.IvoryMuted,
    )
    val tabularHeadline = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp,
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
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            content = { content() },
        )
    }
}
