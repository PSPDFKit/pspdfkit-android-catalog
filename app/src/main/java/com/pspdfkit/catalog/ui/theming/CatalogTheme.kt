/*
 *   Copyright © 2021-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui.theming

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Put colors and anything theme-related here, then pass it to the default MaterialTheme below.

object AlphaDefs {
    const val title = 0.85f
    const val titleSdkVersion = 0.7f
    const val half = 0.5f
    const val iconUnselected = 0.65f
    const val onSurface = 0.87f
}

private val primaryPurple = Color(0xff4537de)
private val variantPurple = Color(0xff2a1cb4)
private val lightPurple = Color(0xff777cf0)
private val white = Color(0xffffffff)
private val secondaryLightGray = Color(0xffd7dce4)
private val variantDarkerGray = Color(0xff717885)
private val variantDark = Color(0xff051628)
private val textDarkGray = Color(0xff051628)
private val textLightGray = Color(0xffd4d4d4)
private val black = Color(0xff000000)
private val snackbarOnSurface = Color(0xff142132)

val LightColorScheme = lightColorScheme(
    primary = primaryPurple,
    onPrimary = white,
    secondary = secondaryLightGray,
    background = white,
    onBackground = textDarkGray,
    onSurface = snackbarOnSurface
)

val DarkColorScheme = darkColorScheme(
    primary = lightPurple,
    onPrimary = white,
    secondary = textLightGray,
    background = black,
    onBackground = white,
    surface = variantDark,
    onSurface = white
)

val snackbarText = Color(0xffA8BBF8)

@Composable
fun CatalogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Dynamic color is available on Android 12+
    content: @Composable () -> Unit
) {
    MaterialTheme(
        typography = catalogTypography,

        colorScheme = when {
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) && dynamicColor -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    ) {
        content()
    }
}
