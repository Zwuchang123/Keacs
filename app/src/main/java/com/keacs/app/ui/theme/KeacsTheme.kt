package com.keacs.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object KeacsColors {
    val Primary = Color(0xFF3F82F6)
    val PrimaryLight = Color(0xFFEAF2FF)
    val Background = Color(0xFFF6F8FC)
    val Surface = Color(0xFFFCFDFF)
    val SurfaceSubtle = Color(0xFFF1F4F8)
    val TextPrimary = Color(0xFF1F2937)
    val TextSecondary = Color(0xFF6B7280)
    val TextTertiary = Color(0xFFA0A7B3)
    val Border = Color(0xFFE6EBF2)
    val Income = Color(0xFF35C785)
    val Expense = Color(0xFFFF5A5F)
    val Warning = Color(0xFFFFB020)
    val Error = Color(0xFFE5484D)

    val CategoryOrange = Color(0xFFFFB45F)
    val CategoryGreen = Color(0xFF54C88A)
    val CategoryBlue = Color(0xFF5B8EF7)
    val CategoryPurple = Color(0xFFA77CF8)
    val CategoryGray = Color(0xFFE9EDF3)
}

object KeacsSpacing {
    val PageHorizontal = 16.dp
    val PageVertical = 12.dp
    val Section = 12.dp
    val CardPadding = 16.dp
    val ControlGap = 10.dp
    val ItemGap = 8.dp
}

object KeacsRadius {
    val Card = 14.dp
    val Input = 10.dp
    val Button = 12.dp
    val Pill = 18.dp
}

object KeacsSize {
    val MinTouch = 48.dp
    val CategoryIcon = 40.dp
    val BottomBarHeight = 82.dp
    val AddButton = 56.dp
}

private val LightColors: ColorScheme = lightColorScheme(
    primary = KeacsColors.Primary,
    onPrimary = KeacsColors.Surface,
    primaryContainer = KeacsColors.PrimaryLight,
    onPrimaryContainer = KeacsColors.TextPrimary,
    secondary = KeacsColors.Income,
    onSecondary = KeacsColors.Surface,
    error = KeacsColors.Error,
    onError = KeacsColors.Surface,
    background = KeacsColors.Background,
    onBackground = KeacsColors.TextPrimary,
    surface = KeacsColors.Surface,
    onSurface = KeacsColors.TextPrimary,
    surfaceVariant = KeacsColors.SurfaceSubtle,
    onSurfaceVariant = KeacsColors.TextSecondary,
    outline = KeacsColors.Border,
)

private val KeacsTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 38.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp,
    ),
)

private val KeacsShapes = Shapes(
    extraSmall = RoundedCornerShape(KeacsRadius.Input),
    small = RoundedCornerShape(KeacsRadius.Input),
    medium = RoundedCornerShape(KeacsRadius.Button),
    large = RoundedCornerShape(KeacsRadius.Card),
    extraLarge = RoundedCornerShape(KeacsRadius.Pill),
)

@Composable
fun KeacsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = KeacsTypography,
        shapes = KeacsShapes,
        content = content,
    )
}
