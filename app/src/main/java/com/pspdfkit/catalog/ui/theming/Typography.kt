/*
 *   Copyright © 2021-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui.theming

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Add different text styles here as vals that can be used elsewhere.

val catalogTypography = Typography(
    // Used for section headers.
    displayLarge = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),

    // Used for example list titles.
    displayMedium = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 21.09.sp,
        letterSpacing = 0.02.sp
    ),

    // Used by the Nutrient title on the settings screen.
    displaySmall = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),

    // Used by settings list titles.
    headlineMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),

    bodyLarge = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.sp
    ),

    bodyMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.sp
    ),

    // Used for the language badges.
    titleMedium = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.02.sp
    ),

    // Used for example descriptions.
    titleSmall = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.sp
    )
)
