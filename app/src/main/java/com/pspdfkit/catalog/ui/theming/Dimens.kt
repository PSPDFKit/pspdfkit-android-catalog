/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui.theming

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object Dimens {
    // General expandable lists.
    val listHeaderHeight = 48.dp
    val listHeaderShadowDepth = 2.dp

    // Examples page.
    val examplesListItemPadding = PaddingValues(start = 72.dp, top = 12.dp, end = 16.dp, bottom = 18.dp)
    val examplesListHeaderHorizontalPadding = PaddingValues(start = 16.dp, end = 16.dp)
    val examplesSpacerHeight = 14.dp
    val iconSize = 24.dp

    // Settings page.
    val preferencesPageHeaderPadding = PaddingValues(start = 16.dp, top = 8.dp, bottom = 11.dp)
    val preferencesItemHeaderPadding = PaddingValues(start = 16.dp, end = 16.dp)
    val preferencesListPadding = PaddingValues(start = 16.dp)
    val buttonSettingPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
    val inlineSettingHeight = 48.dp
    val buttonSettingMinHeight = 24.dp
    val popupRadioSettingHeight = 67.dp
    val preferencesSpacerHeight = 16.dp

    // Misc.
    val snackbarPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp)
    val tabletWidthCutout = 420.dp
}
