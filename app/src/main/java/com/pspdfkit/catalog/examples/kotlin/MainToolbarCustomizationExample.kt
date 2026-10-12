/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.catalog.ui.theming.enableConfigurationEdgeToEdge
import com.pspdfkit.catalog.ui.theming.isNightTheme
import com.pspdfkit.compose.theme.MainToolbarColors
import com.pspdfkit.compose.theme.NutrientTheme
import com.pspdfkit.compose.theme.ToolbarPopupColors
import com.pspdfkit.compose.toolbar.composables.NutrientMainToolbar
import com.pspdfkit.compose.toolbar.composables.NutrientMainToolbarDefaults
import com.pspdfkit.compose.toolbar.composables.nutrientBackdropSource
import com.pspdfkit.compose.toolbar.composables.rememberNutrientBackdropState
import com.pspdfkit.compose.toolbar.state.ToolbarIcon
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.toolbar.NutrientDocumentStatusBarScrim
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.utils.getSupportParcelableExtra
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shows the main-toolbar customization surface in one place:
 *
 *  1. **Custom toolbar layout** — instead of mounting the toolbar edge-to-edge at the top of the
 *     window (the SDK default), the [NutrientMainToolbar] is placed inside a rounded, clipped,
 *     elevated [Surface] so it reads as a *floating pill* over the document, with a custom color
 *     scheme.
 *  2. **Custom toolbar items** — [DocumentState.setToolbarItemsProvider] inserts a custom resize
 *     action into the second toolbar slot that toggles the pill between a reduced (200dp) and an
 *     extended (300dp) width, with [DocumentState.invalidateToolbarItems] re-running the provider
 *     so the action's icon and label follow the current width.
 *  3. **Click routing** — [DocumentState.setOnToolbarItemClickListener] handles the custom item
 *     by its `R.id` and returns `false` for everything else so the SDK defaults keep working.
 *  4. **Companion chrome** — a floating close button sits next to the pill and finishes the
 *     example. It scales to its center when hiding, sequences with the search transition (it
 *     scales out before the pill expands, and back in only after the pill has collapsed), and
 *     follows the toolbar's immersive-mode visibility.
 *  5. **Status-bar scrim** — a floating toolbar sits *below* the status bar, so unlike the SDK's
 *     own edge-to-edge toolbar it leaves the strip above it showing the scrolling document.
 *     [NutrientDocumentStatusBarScrim] blurs and tints that strip, sampling the document through
 *     the [nutrientBackdropSource] modifier on [DocumentView].
 */
class MainToolbarCustomizationExample(context: Context) :
    SdkExample(context, R.string.mainToolbarCustomizationExampleTitle, R.string.mainToolbarCustomizationExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, MainToolbarCustomizationActivity::class.java)
            intent.putExtra(MainToolbarCustomizationActivity.EXTRA_URI, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}

class MainToolbarCustomizationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!
        val configuration = PdfActivityConfiguration.Builder(this)
            .defaultToolbarEnabled(false)
            // Keep the floating pill to a single row: don't render the document title overlay
            // strip below the toolbar items.
            .documentTitleOverlayEnabled(false)
            .build()

        enableConfigurationEdgeToEdge(configuration)
        setContent {
            CatalogTheme(darkTheme = configuration.isNightTheme) {
                FloatingToolbarDocument(uri, configuration, onClose = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_URI = "MainToolbarCustomizationActivity.DocumentUri"
    }
}

@Composable
private fun FloatingToolbarDocument(uri: Uri, configuration: PdfActivityConfiguration, onClose: () -> Unit) {
    val documentState = rememberDocumentState(uri, configuration)

    // The floating pill's current width. The custom toolbar item toggles it between the reduced
    // and extended widths; the provider below reads it to pick the item's icon and label.
    var pillWidthDp by rememberSaveable { mutableIntStateOf(EXTENDED_PILL_WIDTH_DP) }

    // Customize the toolbar items and route the custom item's clicks. Safe to set before the
    // DocumentView attaches — DocumentState applies both once the fragment connects.
    LaunchedEffect(documentState) {
        documentState.setToolbarItemsProvider { defaultItems -> customizeToolbarItems(defaultItems, pillWidthDp) }
        documentState.setOnToolbarItemClickListener { id ->
            when (id) {
                R.id.custom_action1 -> {
                    pillWidthDp = if (pillWidthDp == REDUCED_PILL_WIDTH_DP) EXTENDED_PILL_WIDTH_DP else REDUCED_PILL_WIDTH_DP
                    // Re-run the provider so the resize action flips between reduce and extend.
                    documentState.invalidateToolbarItems()
                    true
                }

                // Everything else keeps the SDK's default handling (search, outline, settings, …).
                else -> false
            }
        }
    }

    // The DocumentView publishes its fragment-scoped toolbar coordinator here once the UI is ready.
    // It is null until then, so render the toolbar only once it is non-null.
    val coordinator by documentState.toolbarCoordinator.collectAsState()

    // Shared between the document (which records into it) and the status-bar scrim (which blurs it).
    val backdrop = rememberNutrientBackdropState()

    // True while an overlay view (thumbnail grid / outline / document info) is shown. We use it to
    // push that overlay's content below the floating toolbar so it isn't hidden behind the pill.
    val overlayViewShown by documentState.viewWithOverlappingToolbarShown.collectAsState()
    val toolbarSizePx = documentState.getContextualToolbarSizePx()
    SideEffect {
        documentState.setContentViewTopPadding(if (overlayViewShown) toolbarSizePx else 0)
    }

    // Let the SDK handle back the layered way: close the contextual toolbar / overlay first.
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = overlayViewShown && !imeVisible) {
        documentState.handleBackPress()
    }

    // No box-wide contentAlignment: by default children sit top-start (top-left). Only the main
    // toolbar is explicitly centered below. This keeps the contextual toolbar and any other overlay
    // left-aligned at their natural position rather than being pinned to the top-center.
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        // The document fills the whole screen and sits *behind* the floating toolbar.
        // nutrientBackdropSource records its pixels so the status-bar scrim below can blur them —
        // it has to be the first child, because the scrim samples whatever drew before it.
        DocumentView(
            documentState = documentState,
            modifier = Modifier
                .fillMaxSize()
                .nutrientBackdropSource(backdrop),
        )

        // The pill is offset by statusBarsPadding(), so without this the document scrolls visibly
        // under the system clock. This variant works out when to show itself: only with the SDK
        // toolbar disabled, only once the page has actually moved up into the strip, and never in
        // immersive mode — so zooming out to fit the page takes the scrim away with it.
        NutrientDocumentStatusBarScrim(
            documentState = documentState,
            state = backdrop,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // The floating, rounded, elevated toolbar. Its colors are a parameter below; NutrientTheme
        // supplies everything else the SDK pieces read (icons, the XML-derived scheme).
        NutrientTheme {
            coordinator?.let { c ->
                // The inline-search session is registered whenever inline search is enabled; its
                // isActive flow flips while the search bar is showing. Expand the pill to full
                // width for the search UI and collapse it back afterwards.
                val searchSession by c.searchSession.collectAsState()
                val searchActive = searchSession?.isActive?.collectAsState()?.value == true

                // The frame's visibility flips with immersive mode (tap-to-hide chrome) — the same
                // flag NutrientMainToolbar animates on. The cross follows it so it hides and shows
                // together with the toolbar.
                val frame by c.frame.collectAsState()

                // Sequence the search transition instead of running both animations at once:
                // opening search first scales the cross out, *then* expands the pill; closing
                // search first collapses the pill, *then* scales the cross back in.
                var crossShownForSearch by remember { mutableStateOf(true) }
                var pillExpandedForSearch by remember { mutableStateOf(false) }
                LaunchedEffect(searchActive) {
                    if (searchActive) {
                        crossShownForSearch = false
                        pillExpandedForSearch = true
                    } else {
                        pillExpandedForSearch = false
                        delay(PILL_WIDTH_ANIMATION_MS.milliseconds)
                        crossShownForSearch = true
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    // Animate the width value itself: animateContentSize only reacts to the
                    // *content* measuring differently, so a constraint swap like
                    // width(300.dp) ↔ fillMaxWidth snaps instead of animating — which also read
                    // as a flicker together with the toolbar's internal row↔search-bar crossfade.
                    val pillWidth by animateDpAsState(
                        targetValue = if (pillExpandedForSearch) maxWidth else pillWidthDp.dp,
                        animationSpec = tween(durationMillis = PILL_WIDTH_ANIMATION_MS),
                        label = "pillWidth",
                    )
                    // The Row is end-aligned, so removing the cross keeps the pill pinned to the
                    // end edge and only the row's start edge moves.
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth(),
                    ) {
                        // Scales in/out around its own center (the default transform origin).
                        AnimatedVisibility(
                            modifier = Modifier.align(Alignment.CenterStart),
                            visible = crossShownForSearch && frame.isVisible,
                            enter = scaleIn(animationSpec = tween(durationMillis = CROSS_ANIMATION_MS)),
                            exit = scaleOut(animationSpec = tween(durationMillis = CROSS_ANIMATION_MS)),
                        ) {
                            Surface(
                                onClick = onClose,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 8.dp,
                                modifier = Modifier.padding(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close example",
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                        // The pill is the toolbar's own chrome — shape, shadow and colors are
                        // parameters — and the animated width is a plain modifier on the
                        // toolbar's outermost node. The wrapper already offsets the status bar, so
                        // the toolbar reserves no insets of its own.
                        NutrientMainToolbar(
                            coordinator = c,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .width(pillWidth),
                            shape = RoundedCornerShape(28.dp),
                            shadowElevation = 8.dp,
                            windowInsets = NutrientMainToolbarDefaults.NoWindowInsets,
                            colors = floatingToolbarColors(),
                            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                        )
                    }
                }
            }
        }
    }
}

/** Duration of the pill's width animation (resize action and search expansion). */
private const val PILL_WIDTH_ANIMATION_MS = 500

/** Duration of the close button's scale-to-center show/hide animation. */
private const val CROSS_ANIMATION_MS = 300

/** The pill width the custom resize action shrinks the toolbar to. */
private const val REDUCED_PILL_WIDTH_DP = 200

/** The pill width the custom resize action grows the toolbar back to. */
private const val EXTENDED_PILL_WIDTH_DP = 300

/**
 * Builds the customized item list from the SDK defaults: inserts a single custom resize action
 * into the second toolbar slot. While the pill is extended the action offers to reduce it to
 * [REDUCED_PILL_WIDTH_DP]; while reduced it offers to extend it back to [EXTENDED_PILL_WIDTH_DP].
 * The action stays pinned in the row ([ToolbarItem.OverflowBehavior.AlwaysShow]) so it never
 * disappears into the overflow menu at the reduced width.
 */
private fun customizeToolbarItems(defaultItems: List<ToolbarItem>, pillWidthDp: Int): List<ToolbarItem> {
    val isReduced = pillWidthDp == REDUCED_PILL_WIDTH_DP
    val resizeAction = ToolbarItem.Button(
        actionId = "catalog.resize_toolbar",
        id = R.id.custom_action1,
        contentDescription = if (isReduced) {
            "Extend toolbar to ${EXTENDED_PILL_WIDTH_DP}dp"
        } else {
            "Reduce toolbar to ${REDUCED_PILL_WIDTH_DP}dp"
        },
        // The pill is pinned to the end of the screen, so extending grows it leftwards and
        // reducing shrinks it rightwards — mirror that in the arrow direction.
        icon = ToolbarIcon.Resource(
            if (isReduced) com.pspdfkit.R.drawable.pspdf__ic_chevron_left else com.pspdfkit.R.drawable.pspdf__ic_chevron_right,
        ),
        overflowBehavior = ToolbarItem.OverflowBehavior.AlwaysShow,
    )
    // The SDK bakes the XML theme's action-bar icon tint into every default item, and at render
    // time a baked tint wins over the Compose scheme. Strip it so every icon follows
    // MainToolbarColors.iconColor from floatingToolbarColors() — otherwise the pill recolors with
    // the Material scheme (e.g. on a dark-mode switch) while the default icons keep the XML tint.
    return defaultItems.map { it.withUntintedIcon() }.toMutableList().apply { add(1, resizeAction) }
}

/** Returns a copy of the item whose icon has no baked tint, deferring to the Compose scheme. */
private fun ToolbarItem.withUntintedIcon(): ToolbarItem {
    val untintedIcon = when (val icon = icon) {
        is ToolbarIcon.Resource -> icon.copy(tint = null)
        is ToolbarIcon.Vector -> icon.copy(tint = null)
        else -> icon
    }
    return when (this) {
        is ToolbarItem.Button -> copy(icon = untintedIcon)
        is ToolbarItem.Toggle -> copy(icon = untintedIcon)
        is ToolbarItem.Submenu -> copy(icon = untintedIcon)
    }
}

/**
 * The floating toolbar's colors — passed straight to `NutrientMainToolbar(colors = …)`, which
 * provides them to its items, title, overflow popup and any slot content. This is the single
 * place to re-skin the pill.
 */
@Composable
private fun floatingToolbarColors(): MainToolbarColors {
    /** Background color shared by the floating pill and the close button. */
    val background = MaterialTheme.colorScheme.primary
    return MainToolbarColors(
        backgroundColor = background,
        textColor = Color.White,
        popup = ToolbarPopupColors(backgroundColor = background),
        titleTextColor = Color.White,
    )
}
