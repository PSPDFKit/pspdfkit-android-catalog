/*
 *   Copyright © 2021-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.ui.model.Action
import com.pspdfkit.catalog.ui.model.Dispatcher
import com.pspdfkit.catalog.ui.model.Page
import com.pspdfkit.catalog.ui.model.SearchState
import com.pspdfkit.catalog.ui.model.State
import com.pspdfkit.catalog.ui.model.searchQueryOrBlank
import com.pspdfkit.catalog.ui.theming.AlphaDefs
import com.pspdfkit.utils.getSupportPackageInfo
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * A Material 3 Expressive **floating pill** top bar for the catalog's main screen.
 *
 * Instead of a pinned [androidx.compose.material3.TopAppBar], the branding, search and settings
 * live inside a rounded [Surface] that hovers over the example list — the list is visible behind
 * its side margins and scrolls underneath it. The pill collapses (slides up and fades) while the
 * user scrolls the list down and springs back on scroll up, driven by [CatalogToolbarScrollState]
 * fed from the list's nested-scroll. It stays pinned while searching or on a sub-page so the back
 * button is always reachable.
 *
 * Its look and motion mirror the SDK's `NutrientFloatingToolbar`: full-corner pill shape, tonal
 * `surfaceContainer` color, a soft 3dp shadow, 16dp screen-edge margins and the Material 3
 * expressive spring tokens (see [ExpressivePillMotion]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
fun CatalogAppBar(
    state: State,
    dispatcher: Dispatcher,
    scrollState: CatalogToolbarScrollState = rememberCatalogToolbarScrollState(),
    isTablet: Boolean = false,
) {
    val isOnMainPage = state.currentPage == Page.ExampleList
    val isSearching = state.searchState != SearchState.Hidden

    // Keep the bar visible while searching or on a sub-page: the search field and the back button
    // must never be scrolled out of reach.
    val pinned = isSearching || !isOnMainPage
    LaunchedEffect(pinned) {
        if (pinned) scrollState.collapsed = false
    }

    val hidden = scrollState.collapsed && !pinned
    var barHeightPx by remember { mutableIntStateOf(0) }
    val translationY by animateFloatAsState(
        targetValue = if (hidden) -barHeightPx.toFloat() else 0f,
        animationSpec = ExpressivePillMotion.spatial(),
        label = "catalogToolbarTranslation",
    )
    val barAlpha by animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = ExpressivePillMotion.effects(),
        label = "catalogToolbarAlpha",
    )

    Crossfade(
        targetState = isSearching,
        animationSpec = ExpressivePillMotion.effects(),
        label = "catalogToolbarContent",
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { barHeightPx = it.height }
            .graphicsLayer {
                this.translationY = translationY
                this.alpha = barAlpha
            },
    ) { searching ->
        if (searching) {
            SearchToolbar(
                searchState = state.searchState,
                onSearchQueryChanged = { dispatcher(Action.SearchQueryChanged(it)) },
                onSearchCancelled = { dispatcher(Action.CancelSearchButtonTapped) },
            )
        } else {
            CatalogToolbarPill(
                dispatcher = dispatcher,
                isOnMainPage = isOnMainPage,
                isTablet = isTablet,
            )
        }
    }
}

@Composable
private fun CatalogToolbarPill(dispatcher: Dispatcher, isOnMainPage: Boolean, isTablet: Boolean) {
    ToolbarPillSurface {
        Row(
            modifier = Modifier
                .heightIn(min = PillContentMinHeight)
                .padding(start = if (isOnMainPage) 20.dp else 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isOnMainPage) {
                BackNavigationIconButton(
                    isTablet = isTablet,
                    onClick = { dispatcher(Action.UpButtonTapped) },
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = "${stringResource(R.string.company_name)} ${stringResource(R.string.app_name)}",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val context = LocalContext.current
                val version = remember { context.packageManager.getSupportPackageInfo(context.packageName, 0).versionName }
                Text(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .alpha(AlphaDefs.TITLE_SDK_VERSION),
                    text = version ?: "",
                    fontWeight = FontWeight.W400,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }

            IconButton(onClick = { dispatcher(Action.SearchButtonTapped) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_topbar_search),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "Search button in the top bar.",
                )
            }

            if (isOnMainPage) {
                IconButton(onClick = { dispatcher(Action.SettingsButtonTapped) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_topbar_settings),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Settings button in the top bar.",
                    )
                }
            }
        }
    }
}

@Composable
private fun BackNavigationIconButton(isTablet: Boolean, onClick: () -> Unit) {
    val icon =
        if (isTablet) {
            Icons.Default.Close
        } else {
            Icons.AutoMirrored.Filled.ArrowBack
        }

    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = "Back navigation button in the top bar.",
        )
    }
}

@ExperimentalComposeUiApi
@Composable
private fun SearchToolbar(searchState: SearchState, onSearchQueryChanged: (String) -> Unit, onSearchCancelled: () -> Unit) {
    ToolbarPillSurface {
        Row(
            modifier = Modifier
                .heightIn(min = PillContentMinHeight)
                .padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusRequester = remember { FocusRequester() }

            // Seed the field from the query once, when the search pill appears. We must NOT re-seed on
            // every query change, or a single keystroke would turn into a repetition of itself through
            // the debounced state round-trip. The pill only exists while searching, so a keyless
            // remember seeds exactly once.
            var textFieldValue by remember {
                mutableStateOf(TextFieldValue(searchState.searchQueryOrBlank()))
            }

            TextField(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                placeholder = { Text(text = "Search") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusRequester.freeFocus()
                    },
                ),
            )

            // Debounce keyboard input slightly to improve search performance.
            LaunchedEffect(key1 = textFieldValue) {
                delay(300.milliseconds)
                onSearchQueryChanged(textFieldValue.text)
            }

            IconButton(
                onClick = { onSearchCancelled() },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                )
            }

            LaunchedEffect(Unit) {
                // Focus and display the software keyboard when the search pill appears.
                focusRequester.requestFocus()
            }
        }
    }
}

/** The shared floating-pill container: full-corner rounded [Surface], tonal color, soft shadow, screen-edge margins. */
@Composable
private fun ToolbarPillSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            // Top strip plus the horizontal bars: in landscape the 3-button navigation bar docks
            // to a side, and a pill inset only from the top puts its trailing icon (the settings
            // gear) behind it, where it never receives a touch.
            .windowInsetsPadding(CatalogToolbarInsets)
            .fillMaxWidth()
            .padding(horizontal = PillHorizontalMargin, vertical = PillVerticalMargin),
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = PillElevation,
        content = content,
    )
}

/**
 * Drives the floating pill's collapse-on-scroll. Fed from the example list through
 * [nestedScrollConnection]: scrolling the content up collapses the pill, scrolling back down
 * reveals it. The caller pins the pill (forces [collapsed] back to false) while searching or on a
 * sub-page.
 */
@Stable
class CatalogToolbarScrollState {
    var collapsed by mutableStateOf(false)

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            when {
                available.y < -COLLAPSE_SCROLL_THRESHOLD -> collapsed = true
                available.y > COLLAPSE_SCROLL_THRESHOLD -> collapsed = false
            }
            return Offset.Zero
        }
    }
}

@Composable
fun rememberCatalogToolbarScrollState(): CatalogToolbarScrollState = remember { CatalogToolbarScrollState() }

/**
 * Content padding to keep list content clear of the floating pill (top) and the system bars,
 * so the first row is not hidden under the pill at rest and the last row clears the nav bar.
 */
@Composable
fun catalogToolbarClearancePadding(): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    // Horizontal too, so a landscape side navigation bar does not sit on top of the row
    // chevrons and language badges at the trailing edge.
    val systemBars = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    return PaddingValues(
        top = systemBars.calculateTopPadding() + PillRestingHeight,
        bottom = systemBars.calculateBottomPadding(),
        start = systemBars.calculateStartPadding(layoutDirection),
        end = systemBars.calculateEndPadding(layoutDirection),
    )
}

/**
 * The window insets the catalog's floating toolbar pill keeps clear of: the status bar above it,
 * and the navigation bar / display cutout beside it in landscape.
 */
private val CatalogToolbarInsets: WindowInsets
    @Composable get() = WindowInsets.statusBars.union(
        WindowInsets.navigationBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    )

/**
 * The Material 3 **expressive** motion scheme's spring tokens, inlined to match the SDK's
 * `NutrientFloatingToolbar` (material3 1.4.0 keeps its `MotionScheme` API internal). Spatial
 * springs move things (position/size/scale) with a visible bounce; effects springs (fades) are
 * critically damped so they never overshoot.
 */
private object ExpressivePillMotion {
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
}

// Pill metrics, mirroring the SDK floating toolbar (full-corner shape, 16dp screen-edge margin).
private val PillShape = RoundedCornerShape(percent = 50)
private val PillContentMinHeight = 56.dp
private val PillHorizontalMargin = 16.dp
private val PillVerticalMargin = 8.dp
private val PillElevation = 3.dp

/** The pill's resting height (content + its top and bottom margins), excluding the status-bar inset. */
private val PillRestingHeight: Dp = PillContentMinHeight + PillVerticalMargin * 2

// Ignore sub-pixel scroll jitter so the pill does not flicker between collapsed/expanded.
private const val COLLAPSE_SCROLL_THRESHOLD = 2f
