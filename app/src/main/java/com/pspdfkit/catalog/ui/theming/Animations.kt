/*
 *   Copyright © 2021-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui.theming

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color

object Animations {
    private const val SECTION_EXPANDING_DURATION = 250
    private const val PAGE_TRANSITION_DURATION = 300

    fun catalogEnterAnimation() = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(durationMillis = PAGE_TRANSITION_DURATION, easing = FastOutSlowInEasing),
    )

    fun catalogExitAnimation() = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(durationMillis = PAGE_TRANSITION_DURATION, easing = FastOutSlowInEasing),
    )

    @Composable
    fun expandSectionButtonRotation(expanded: Boolean): State<Float> = animateFloatAsState(
        targetValue = if (expanded) -180f else 0f,
        animationSpec =
        tween(
            durationMillis = SECTION_EXPANDING_DURATION,
            easing = FastOutSlowInEasing,
        ),
    )

    @Composable
    fun sectionExpandingAlpha(expanded: Boolean): State<Float> = animateFloatAsState(
        targetValue = if (expanded) 1.0f else AlphaDefs.TITLE,
        animationSpec =
        tween(
            durationMillis = SECTION_EXPANDING_DURATION,
            easing = LinearEasing,
        ),
    )

    @Composable
    fun sectionExpandingIconAlpha(expanded: Boolean): State<Float> = animateFloatAsState(
        targetValue = if (expanded) 1.0f else AlphaDefs.ICON_UNSELECTED,
        animationSpec =
        tween(
            durationMillis = SECTION_EXPANDING_DURATION,
            easing = LinearEasing,
        ),
    )

    @Composable
    fun sectionExpandingColor(expanded: Boolean): State<Color> = animateColorAsState(
        targetValue =
        if (expanded) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        animationSpec =
        tween(
            durationMillis = SECTION_EXPANDING_DURATION,
            easing = LinearEasing,
        ),
    )
}
