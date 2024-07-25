/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.ui.theming

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
object Animations {

    private const val sectionExpandingDuration = 250

    fun catalogEnterAnimation() = slideInHorizontally()

    fun catalogExitAnimation() = slideOutHorizontally()

    @Composable
    fun expandSectionButtonRotation(expanded: Boolean): State<Float> =
        animateFloatAsState(
            targetValue = if (expanded) -180f else 0f,
            animationSpec = tween(
                durationMillis = sectionExpandingDuration,
                easing = FastOutSlowInEasing
            )
        )

    @Composable
    fun sectionExpandingAlpha(expanded: Boolean): State<Float> =
        animateFloatAsState(
            targetValue = if (expanded) 1.0f else AlphaDefs.title,
            animationSpec = tween(
                durationMillis = sectionExpandingDuration,
                easing = LinearEasing
            )
        )

    @Composable
    fun sectionExpandingIconAlpha(expanded: Boolean): State<Float> =
        animateFloatAsState(
            targetValue = if (expanded) 1.0f else AlphaDefs.iconUnselected,
            animationSpec = tween(
                durationMillis = sectionExpandingDuration,
                easing = LinearEasing
            )
        )

    @Composable
    fun sectionExpandingColor(expanded: Boolean): State<Color> =
        animateColorAsState(
            targetValue = if (expanded) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.onBackground
            },
            animationSpec = tween(
                durationMillis = sectionExpandingDuration,
                easing = LinearEasing
            )
        )
}

@Composable
@ExperimentalAnimationApi
fun CircularReveal(
    isVisible: Boolean,
    rippleOrigin: Offset,
    content: @Composable () -> Unit
) {
    val height = 56.dp
    val (widthInPx, heightInPx) = with(LocalConfiguration.current) {
        with(LocalDensity.current) { screenWidthDp.dp.toPx() to height.toPx() }
    }

    // Calculate the minimum size needed for the ripple effect
    val maxRadiusPx = kotlin.math.hypot(widthInPx.toDouble(), heightInPx.toDouble())
    val backgroundColor = MaterialTheme.colors.background
    val radius by animateFloatAsState(
        targetValue = if (isVisible) maxRadiusPx.toFloat() else 0f,
        animationSpec = tween()
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.Transparent)
            // Clip the circle to the size of the toolbar
            .clipToBounds()
            .drawBehind {
                drawCircle(
                    color = backgroundColor,
                    radius = radius,
                    center = Offset(size.width - rippleOrigin.x, rippleOrigin.y)
                )
            }
    ) {
        AnimatedVisibility(
            visible = isVisible,
            // Any animation other than fading looks wonky
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            content()
        }
    }
}
