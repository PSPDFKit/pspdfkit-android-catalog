/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.ui.theming.Dimens

@Composable
fun SpacerLine() {
    val spacerColor = remember { mutableStateOf(Color.Transparent) }

    if (spacerColor.value == Color.Transparent) {
        spacerColor.value = MaterialTheme.colors.onBackground.copy(alpha = 0.1f)
    }

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color = spacerColor.value)
    )
}

@Composable
fun SpacerTransparentFooter() {
    val height = remember { Dimens.preferencesSpacerHeight }

    Spacer(
        modifier = Modifier
            .height(height.value.dp)
            .fillMaxWidth()
            .background(Color.Transparent)
    )
}

@Composable
fun SpacerExamplesHeader() {
    val height = remember { Dimens.examplesSpacerHeight }

    Spacer(
        modifier = Modifier
            .height(height.value.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
    )
}
