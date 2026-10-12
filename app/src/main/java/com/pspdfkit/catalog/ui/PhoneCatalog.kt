/*
 *   Copyright © 2021-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.pspdfkit.catalog.ui.model.Dispatcher
import com.pspdfkit.catalog.ui.model.Page
import com.pspdfkit.catalog.ui.model.State
import com.pspdfkit.catalog.ui.theming.Animations

/**
 * Our starting Composable. This is the single entrypoint for the whole app, called from the MainActivity.
 *
 * The example list fills the whole screen and scrolls underneath the floating [CatalogAppBar] pill,
 * which is overlaid on top (drawn last) so the list is visible behind its margins.
 */
@ExperimentalFoundationApi
@Composable
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
fun PhoneCatalog(state: State, dispatcher: Dispatcher) {
    val scrollState = rememberCatalogToolbarScrollState()
    val clearance = catalogToolbarClearancePadding()

    // No topBar slot: the pill floats over the content, so we handle window insets ourselves.
    // contentWindowInsets is zeroed, so innerPadding is empty — we consume it only to satisfy lint.
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // List of examples, scrolling behind the floating pill and driving its collapse.
            Box(Modifier.fillMaxSize().nestedScroll(scrollState.nestedScrollConnection)) {
                Examples(state, dispatcher, contentPadding = clearance)
            }

            AnimatedVisibility(
                visible = state.currentPage == Page.Settings,
                enter = Animations.catalogEnterAnimation(),
                exit = Animations.catalogExitAnimation(),
            ) {
                // List of options.
                Preferences(state, dispatcher, contentPadding = clearance)
            }

            CatalogAppBar(state, dispatcher, scrollState = scrollState)
        }
    }
}
