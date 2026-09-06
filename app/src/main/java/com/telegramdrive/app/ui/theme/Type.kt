package com.telegramdrive.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.telegramdrive.app.R

val generalSansFamily = FontFamily(
    Font(R.font.general_sans_regular, FontWeight.Normal),
    Font(R.font.general_sans_bold, FontWeight.Bold),
    Font(R.font.general_sans_bold, FontWeight.SemiBold)
)

val satoshiFamily = FontFamily(
    Font(R.font.satoshi_regular, FontWeight.Normal),
    Font(R.font.satoshi_bold, FontWeight.Bold),
    Font(R.font.satoshi_bold, FontWeight.SemiBold),
    Font(R.font.satoshi_regular, FontWeight.Medium)
)

val TDriveTypography = Typography(
    displayLarge = TextStyle(fontFamily = generalSansFamily, fontSize = 57.sp, fontWeight = FontWeight.Bold, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = generalSansFamily, fontSize = 45.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = generalSansFamily, fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = generalSansFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = generalSansFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = generalSansFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = generalSansFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = generalSansFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = generalSansFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = satoshiFamily, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = satoshiFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = satoshiFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = satoshiFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = satoshiFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = satoshiFamily, fontSize = 10.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp)
)