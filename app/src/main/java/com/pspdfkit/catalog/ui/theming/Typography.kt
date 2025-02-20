/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui.theming

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Add different text styles here as vals that can be used elsewhere.

val catalogTypography = Typography(
    // Used for section headers.
    h1 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),

    // Used for example list titles.
    h2 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 21.09.sp,
        letterSpacing = 0.02.sp
    ),

    // Used by the PSPDFKit title on the settings screen.
    h3 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),

    // Used by settings list titles.
    h4 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.sp
    ),

    body1 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.sp
    ),

    body2 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.02.sp
    ),

    // Used for the language badges.
    subtitle1 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.02.sp
    ),

    // Used for example descriptions.
    subtitle2 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.sp
    )
)
