/*
 *   Copyright © 2021-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.pspdfkit.catalog.ui.model.Action
import com.pspdfkit.catalog.ui.model.Dispatcher
import com.pspdfkit.catalog.ui.model.Page
import com.pspdfkit.catalog.ui.model.State

/**
 * Our starting Composable. This is the single entrypoint for the whole app, called from the MainActivity.
 *
 * The example list fills the whole screen and scrolls underneath the floating [CatalogAppBar] pill,
 * which is overlaid on top (drawn last) so it stays visible above the example list and the settings
 * drawer alike.
 */
@ExperimentalFoundationApi
@Composable
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
fun TabletCatalog(state: State, dispatcher: Dispatcher) {
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
            val drawerState = rememberDrawerState(DrawerValue.Closed)

            EndPositionedModalDrawer(
                drawerContent = { Preferences(state, dispatcher, contentPadding = clearance) },
                drawerState = drawerState,
            ) {
                Box(Modifier.fillMaxSize().nestedScroll(scrollState.nestedScrollConnection)) {
                    Examples(state, dispatcher, contentPadding = clearance)
                }
            }

            CatalogAppBar(state, dispatcher, scrollState = scrollState, isTablet = true)

            LaunchedEffect(state.currentPage) {
                // Ensure state is synced when the user hits any navigation buttons
                when (state.currentPage) {
                    Page.ExampleList -> drawerState.close()
                    Page.Settings -> drawerState.open()
                }
            }

            LaunchedEffect(drawerState.currentValue) {
                // Ensure state is synced when the user dismisses manually
                if (drawerState.currentValue == DrawerValue.Closed) {
                    dispatcher(Action.UpButtonTapped)
                }
            }
        }
    }
}

@Composable
private fun EndPositionedModalDrawer(
    drawerContent: @Composable () -> Unit,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: @Composable () -> Unit,
) {
    // HACK: There is currently no way to force the drawer to come from
    // the trailing part of the screen. I'm changing the layout direction
    // to RTL for the drawer to render at the end and then switching back
    // for the remaining content.
    val originalLayoutDirection = LocalLayoutDirection.current
    val oppositeLayoutDirection =
        when (originalLayoutDirection) {
            LayoutDirection.Ltr -> LayoutDirection.Rtl
            LayoutDirection.Rtl -> LayoutDirection.Ltr
        }

    WithLayoutDirection(oppositeLayoutDirection) {
        ModalNavigationDrawer(
            drawerContent = {
                WithLayoutDirection(originalLayoutDirection) {
                    drawerContent()
                }
            },
            drawerState = drawerState,
        ) {
            WithLayoutDirection(originalLayoutDirection) {
                content()
            }
        }
    }
}

@Composable
private fun WithLayoutDirection(layoutDirection: LayoutDirection, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
    ) {
        content()
    }
}
