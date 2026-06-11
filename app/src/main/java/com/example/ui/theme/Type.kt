package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp

// Dynamically generate M3 typography with custom premium styles and configurations
fun getDynamicTypography(fontName: String): Typography {
    val baseFamily = when (fontName.lowercase()) {
        "helvetica" -> FontFamily.SansSerif
        "futura" -> FontFamily.SansSerif
        "garamond" -> FontFamily.Serif
        "playfair" -> FontFamily.Serif
        "cinzel" -> FontFamily.Serif
        "bodoni" -> FontFamily.Serif
        "baskerville" -> FontFamily.Serif
        "firacode" -> FontFamily.Monospace
        "comicsans" -> FontFamily.Cursive
        "sf_pro_display" -> FontFamily.SansSerif
        "proxima_nova" -> FontFamily.SansSerif
        "circular_std" -> FontFamily.SansSerif
        "avenir_next" -> FontFamily.SansSerif
        "apercu_pro" -> FontFamily.SansSerif
        "caslon_pro" -> FontFamily.Serif
        "optima_premium" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

    val baseWeight = when (fontName.lowercase()) {
        "futura" -> FontWeight.Bold
        "playfair" -> FontWeight.ExtraBold
        "cinzel" -> FontWeight.SemiBold
        "bodoni" -> FontWeight.Black
        "circular_std" -> FontWeight.SemiBold
        "sf_pro_display" -> FontWeight.Medium
        "proxima_nova" -> FontWeight.Normal
        else -> FontWeight.Normal
    }

    val style = when (fontName.lowercase()) {
        "garamond", "caslon_pro" -> FontStyle.Italic
        else -> FontStyle.Normal
    }

    val letterSpacingVal = when (fontName.lowercase()) {
        "futura", "cinzel", "circular_std" -> 1.5.sp
        "helvetica", "sf_pro_display" -> (-0.5).sp
        "proxima_nova", "avenir_next" -> 0.4.sp
        else -> 0.sp
    }

    return Typography(
        bodyLarge = TextStyle(
            fontFamily = baseFamily,
            fontWeight = baseWeight,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = letterSpacingVal,
            fontStyle = style
        ),
        bodyMedium = TextStyle(
            fontFamily = baseFamily,
            fontWeight = baseWeight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = letterSpacingVal,
            fontStyle = style
        ),
        bodySmall = TextStyle(
            fontFamily = baseFamily,
            fontWeight = baseWeight,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = letterSpacingVal,
            fontStyle = style
        ),
        titleLarge = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = letterSpacingVal
        ),
        titleMedium = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = letterSpacingVal
        ),
        titleSmall = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = letterSpacingVal
        ),
        labelLarge = TextStyle(
            fontFamily = baseFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = letterSpacingVal
        )
    )
}

val Typography = getDynamicTypography("default")
