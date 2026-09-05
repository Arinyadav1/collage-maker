package com.example.travenorcustomer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.travenorcustomer.R


@Composable
private fun sfUiDisplayFontFamily(): FontFamily {
    return FontFamily(
        Font(R.font.sf_ui_display_regular, FontWeight.Normal),
        Font(R.font.sf_ui_display_semibold, FontWeight.SemiBold),
        Font(R.font.sf_ui_display_bold, FontWeight.Bold),
        Font(R.font.sf_ui_display_light, FontWeight.Light),
        Font(R.font.sf_ui_display_black, FontWeight.Black),
        Font(R.font.sf_ui_display_medium, FontWeight.Medium),

        )
}

@Composable
private fun gillSansMtFontFamily(): FontFamily {
    return FontFamily(
        Font(R.font.gill_sans_mt, FontWeight.Normal),
    )
}

@Composable
private fun sfProRoundedFontFamily(): FontFamily {
    return FontFamily(
        Font(R.font.sf_pro_rounded_medium, FontWeight.Medium),
        Font(R.font.sf_pro_rounded_regular, FontWeight.Normal),
    )
}

@Composable
private fun geometrBlkBtFontFamily(): FontFamily {
    return FontFamily(
        Font(R.font.geometr415_blk_bt, FontWeight.Black),
    )
}

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

object AppTypography{

    val onBoardTitle: TextStyle
        @Composable get() = TextStyle(
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontFamily = geometrBlkBtFontFamily(),
            fontWeight = FontWeight.Black,
        )

    val onBoardDescription: TextStyle
        @Composable get() = TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontFamily = gillSansMtFontFamily(),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )

    val textButton: TextStyle
        @Composable get() = TextStyle(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

    val signInTitle: TextStyle
        @Composable get() = TextStyle(
            fontSize = 26.sp,
            lineHeight = 34.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.SemiBold  ,
        )

    val signInBody: TextStyle
        @Composable get() = TextStyle(
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )

    val signInLabel: TextStyle
        @Composable get() = TextStyle(
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.Normal  ,
        )

    val homeTitleLargeLight: TextStyle
        @Composable get() = TextStyle(
            fontSize = 38.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.Light  ,
        )

    val homeTitleLargeBold: TextStyle
        @Composable get() = TextStyle(
            fontSize = 38.sp,
            lineHeight = 50.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.Bold  ,
        )

    val homeTitleSemiBold: TextStyle
        @Composable get() = TextStyle(
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.SemiBold  ,
        )

    val homeTitleMedium : TextStyle
        @Composable get() = TextStyle(
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontFamily = sfProRoundedFontFamily(),
            fontWeight = FontWeight.Medium  ,
        )

    val homeMediumTitleMedium : TextStyle
        @Composable get() = TextStyle(
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontFamily = sfProRoundedFontFamily(),
            fontWeight = FontWeight.Medium  ,
        )

    val homeSmallTitleMedium : TextStyle
        @Composable get() = TextStyle(
            fontSize = 15.sp,
            lineHeight = 20 .sp,
            fontFamily = sfProRoundedFontFamily(),
            fontWeight = FontWeight.Normal  ,
        )

    val homeLabel : TextStyle
        @Composable get() = TextStyle(
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp
        )

    val homeSmallLabel : TextStyle
        @Composable get() = TextStyle(
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.Normal  ,
        )

    val detailTitle : TextStyle
        @Composable get() = TextStyle(
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontFamily = sfProRoundedFontFamily(),
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )

    val detailSmallTitle : TextStyle
        @Composable get() = TextStyle(
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )

    val detailBody : TextStyle
        @Composable get() = TextStyle(
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
        )

    val detailSmallBody : TextStyle
        @Composable get() = TextStyle(
            fontSize = 13.sp,
            lineHeight = 22.sp,
            fontFamily = sfUiDisplayFontFamily(),
            fontWeight = FontWeight.Normal,
        )
}