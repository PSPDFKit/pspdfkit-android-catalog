/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.ui

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.ui.theming.Animations

@Composable
fun ExpandSectionButton(
    sectionTitle: String,
    iconSize: Dp,
    expanded: Boolean,
    onExpandButtonClicked: () -> Unit
) {
    // The icon is rotated outwards when expanding.
    val rotateAnimation: Float by Animations.expandSectionButtonRotation(
        expanded = expanded
    )

    IconToggleButton(
        modifier = Modifier.defaultMinSize(iconSize),
        checked = expanded,
        onCheckedChange = { onExpandButtonClicked() }
    ) {
        Icon(
            modifier = Modifier
                .defaultMinSize(iconSize)
                .rotate(rotateAnimation),
            painter = painterResource(id = R.drawable.ic_expand_arrow),
            tint = MaterialTheme.colors.secondaryVariant,
            contentDescription = stringResource(R.string.section_button_content_desc, sectionTitle)
        )
    }
}
