/*
 *   Copyright © 2021-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import com.pspdfkit.catalog.ui.model.Dispatcher
import com.pspdfkit.catalog.ui.model.Page
import com.pspdfkit.catalog.ui.model.State
import com.pspdfkit.catalog.ui.theming.Animations

/**
 * Our starting Composable. This is the single entrypoint for the whole app, called from the MainActivity.
 */
@ExperimentalFoundationApi
@Composable
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
fun PhoneCatalog(
    state: State,
    dispatcher: Dispatcher
) {
    Scaffold(
        topBar = { CatalogAppBar(state, dispatcher) }
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            // List of examples.
            Examples(state, dispatcher)

            AnimatedVisibility(
                visible = state.currentPage == Page.Settings,
                enter = Animations.catalogEnterAnimation(),
                exit = Animations.catalogExitAnimation()
            ) {
                // List of options.
                Preferences(state, dispatcher)
            }
        }
    }
}
