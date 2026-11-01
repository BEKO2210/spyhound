package de.belkis.spyhound.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SpyColors {
    val Ink = Color(0xFF0A0E14)
    val Surface = Color(0xFF111723)
    val SurfaceRaised = Color(0xFF18202E)
    val Border = Color(0xFF243042)
    val Ivory = Color(0xFFF2EBE0)
    val IvoryMuted = Ivory.copy(alpha = 0.7f)
    val Amber = Color(0xFFFFB547)
    val Clear = Color(0xFF3FC9A8)
    val Danger = Color(0xFFFF5A5F)
}

object SpySpacing {
    val Unit = 4.dp
    val PhoneEdge = 20.dp
    val TabletEdge = 32.dp
    val Section = 28.dp
}

object SpyRadii {
    val Card = 12.dp
    val Pill = 24.dp
}

object SpyTypeScale {
    val Tiny = 12.sp
    val Small = 14.sp
    val Body = 16.sp
    val Title = 20.sp
    val Heading = 28.sp
    val Display = 40.sp
    val Hero = 56.sp
}
