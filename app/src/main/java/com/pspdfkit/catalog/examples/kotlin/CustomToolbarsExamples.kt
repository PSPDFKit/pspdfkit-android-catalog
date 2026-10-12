/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
@file:OptIn(ExperimentalLayoutApi::class)

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.catalog.ui.theming.applyConfigurationTheme
import com.pspdfkit.catalog.ui.theming.enableConfigurationEdgeToEdge
import com.pspdfkit.catalog.ui.theming.hasDarkMainToolbar
import com.pspdfkit.catalog.ui.theming.isNightTheme
import com.pspdfkit.compose.theme.MainToolbarColors
import com.pspdfkit.compose.theme.NutrientTheme
import com.pspdfkit.compose.theme.ToolbarPopupColors
import com.pspdfkit.compose.theme.UiColorScheme
import com.pspdfkit.compose.theme.UiTheme
import com.pspdfkit.compose.theme.getUiColors
import com.pspdfkit.compose.toolbar.adapter.ToolbarCoordinator
import com.pspdfkit.compose.toolbar.adapter.ToolbarSubscription
import com.pspdfkit.compose.toolbar.composables.LocalToolbarContentInset
import com.pspdfkit.compose.toolbar.composables.NutrientContextualToolbar
import com.pspdfkit.compose.toolbar.composables.NutrientMainToolbar
import com.pspdfkit.compose.toolbar.composables.NutrientMainToolbarDefaults
import com.pspdfkit.compose.toolbar.composables.NutrientToolbarItemButton
import com.pspdfkit.compose.toolbar.composables.NutrientToolbarItems
import com.pspdfkit.compose.toolbar.composables.NutrientToolbars
import com.pspdfkit.compose.toolbar.state.ContextualToolbarKind
import com.pspdfkit.compose.toolbar.state.ContextualToolbarState
import com.pspdfkit.compose.toolbar.state.ToolbarAction
import com.pspdfkit.compose.toolbar.state.ToolbarIcon
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.compose.toolbar.state.ToolbarItems
import com.pspdfkit.compose.toolbar.state.withContextualKind
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.toolbar.AnnotationToolbarBinder
import com.pspdfkit.jetpack.compose.toolbar.PdfFragmentToolbarBinder
import com.pspdfkit.jetpack.compose.toolbar.nutrientToolbarCoordinator
import com.pspdfkit.jetpack.compose.toolbar.nutrientToolbarsComposeView
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.annotations.OnAnnotatingModeChangeListener
import com.pspdfkit.ui.inspector.PropertyInspectorCoordinatorLayout
import com.pspdfkit.ui.inspector.annotation.AnnotatingInspectorController
import com.pspdfkit.ui.inspector.annotation.DefaultAnnotationCreationInspectorController
import com.pspdfkit.ui.inspector.annotation.DefaultAnnotationEditingInspectorController
import com.pspdfkit.ui.special_mode.controller.AnnotatingController
import com.pspdfkit.utils.getSupportParcelableExtra
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/*
 * Four catalog entries covering the toolbar hosting shapes the SDK supports beyond the stock
 * `PdfActivity`, all sharing **one inset scheme** ([HostInsetScheme]) and one set of custom
 * chrome composables ([HostedMainToolbar], [HostedContextualBar]) — proving the chrome and its
 * inset handling are host-independent:
 *
 * - [CustomToolbarsPdfActivityExample]: `PdfActivity` with both SDK bars disabled and custom
 *   Compose chrome overlaid on the activity-scoped [ToolbarCoordinator].
 * - [CustomToolbarsPdfFragmentExample]: a bare [PdfFragment] with the SDK bars mounted by the
 *   host through [NutrientToolbars], plus the annotation property inspector.
 * - [CustomToolbarsPdfFragmentViewExample]: the same integration for classic View layouts, mounting
 *   the SDK toolbar stack as a `ComposeView` via [nutrientToolbarsComposeView].
 * - [CustomToolbarsDocumentViewExample]: the Compose [DocumentView] wrapper with both SDK bars
 *   disabled and the same custom chrome rendered from [ToolbarCoordinator.contextualToolbar].
 */

/**
 * `PdfActivity` with fully custom toolbars: `defaultToolbarEnabled(false)` +
 * `defaultContextualToolbarEnabled(false)` on a [PdfActivity] subclass. The SDK keeps
 * populating the activity-scoped [ToolbarCoordinator] (default items, contextual state on mode
 * changes), and the subclass overlays its own Compose bars on the same coordinator — search,
 * outline, annotation tools, and undo/redo stay fully functional.
 *
 * Known limits: overlay views (thumbnail grid, outline) keep their built-in ~56dp top inset,
 * and the popup toolbars stay SDK-rendered (see `PopupToolbarCustomItemsExample`).
 *
 * For a production-styled custom main toolbar see [MainToolbarCustomizationExample]; for
 * user-customizable contextual tools see `AnnotationToolbarUserEditableExample`.
 */
class CustomToolbarsPdfActivityExample(context: Context) :
    SdkExample(context, R.string.customToolbarsPdfActivityExampleTitle, R.string.customToolbarsPdfActivityExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(
                    configuration
                        .defaultToolbarEnabled(false)
                        .defaultContextualToolbarEnabled(false)
                        .documentTitleOverlayEnabled(false)
                        // Flip the main-toolbar frame off while ANY contextual bar is shown —
                        // including side-docked ones (a top-docked bar always takes the frame
                        // over). The host-rendered pill follows the frame, so it hides while
                        // the document/content editing bars are up.
                        .hideMainToolbarWhenContextualToolbarIsDocked(true)
                        .build(),
                )
                .activityClass(CustomChromePdfActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * A bare [PdfFragment] hosted inside [NutrientToolbars] — the same single-tree scaffold
 * `PdfActivity` uses internally, self-hosted, plus the annotation property inspector.
 *
 * Known limit: only the annotation bar has a public binder — the document/content editing bars
 * can't be driven from a bare fragment.
 */
class CustomToolbarsPdfFragmentExample(context: Context) :
    SdkExample(context, R.string.customToolbarsPdfFragmentExampleTitle, R.string.customToolbarsPdfFragmentExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, ScaffoldHostedFragmentActivity::class.java)
                .putExtra(ScaffoldHostedFragmentActivity.EXTRA_URI, Uri.fromFile(documentFile))
                // The full catalog-prepared configuration: the host applies its theme itself
                // (a bare fragment gets no PdfActivity theme handling) and hands the
                // fragment-level slice to the PdfFragment.
                .putExtra(ScaffoldHostedFragmentActivity.EXTRA_CONFIGURATION, configuration.build())
            context.startActivity(intent)
        }
    }
}

/**
 * The same bare-[PdfFragment] integration for classic View layouts: the SDK toolbar stack
 * mounts as a `ComposeView` via [nutrientToolbarsComposeView], with the full docking contract
 * `PdfActivity` has — drag handle, drop-zone chrome, dock persistence.
 */
class CustomToolbarsPdfFragmentViewExample(context: Context) :
    SdkExample(context, R.string.customToolbarsPdfFragmentViewExampleTitle, R.string.customToolbarsPdfFragmentViewExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, ScaffoldHostedFragmentActivity::class.java)
                .putExtra(ScaffoldHostedFragmentActivity.EXTRA_URI, Uri.fromFile(documentFile))
                .putExtra(ScaffoldHostedFragmentActivity.EXTRA_CONFIGURATION, configuration.build())
                .putExtra(ScaffoldHostedFragmentActivity.EXTRA_VIEW_INTEROP, true)
            context.startActivity(intent)
        }
    }
}

/**
 * The Compose [DocumentView] wrapper with both SDK bars disabled: the host renders the main
 * toolbar *and* a bottom-docked contextual bar (a dock the SDK itself doesn't offer) from
 * [ToolbarCoordinator.contextualToolbar] — the same chrome composables
 * [CustomToolbarsPdfActivityExample] overlays on `PdfActivity`.
 */
class CustomToolbarsDocumentViewExample(context: Context) :
    SdkExample(context, R.string.customToolbarsDocumentViewExampleTitle, R.string.customToolbarsDocumentViewExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, CustomToolbarsDocumentViewActivity::class.java)
                .putExtra(CustomToolbarsDocumentViewActivity.EXTRA_URI, Uri.fromFile(documentFile))
                // The catalog-prepared configuration carries the catalog theme and the user's
                // preference toggles — pass it through instead of rebuilding.
                .putExtra(CustomToolbarsDocumentViewActivity.EXTRA_CONFIG, configuration.build())
            context.startActivity(intent)
        }
    }
}

// region The shared inset scheme

/**
 * The single inset policy every toolbar and toolbar container in these examples uses, at every
 * integration level:
 *
 * - **Placement insets are the *stable* system bars united with the display cutout.** Stable
 *   (ignoring-visibility) values keep the bars from jumping when the SDK hides the system UI
 *   (immersive mode, hide-UI-when-creating-annotations) — the same policy the SDK's own
 *   toolbar scaffold uses. Including the cutout keeps chrome out of camera notches in
 *   landscape.
 * - **Top-anchored chrome** consumes the top and horizontal sides of the placement insets.
 * - **Bottom-anchored chrome** consumes the bottom and horizontal sides, and additionally
 *   yields to the IME so the keyboard never covers it.
 * - **Containers, not bars, consume the insets.** Each toolbar composable is rendered with
 *   zero insets of its own ([NutrientMainToolbar]'s `windowInsets = WindowInsets(0,0,0,0)`),
 *   and its wrapping container applies the scheme exactly once — so no inset is ever
 *   double-counted no matter which host the chrome moves to.
 */
private object HostInsetScheme {
    /** Stable system bars ∪ display cutout — the placement basis for every bar. */
    val placement: WindowInsets
        @Composable get() = WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout)

    /** What a top-anchored toolbar container consumes. */
    val topBar: WindowInsets
        @Composable get() = placement.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

    /** What a bottom-anchored toolbar container consumes — additionally yields to the IME. */
    val bottomBar: WindowInsets
        @Composable get() = placement.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal).union(WindowInsets.ime)

    /** The scheme's top placement inset as a scalar, for [NutrientToolbars]. */
    @Composable
    fun topPlacementInset(): Dp = with(LocalDensity.current) { placement.getTop(this).toDp() }

    /** The scheme's bottom placement inset as a scalar, for [NutrientToolbars]. */
    @Composable
    fun bottomPlacementInset(): Dp = with(LocalDensity.current) { placement.getBottom(this).toDp() }

    /** The scheme's physical-left placement inset, for [NutrientToolbars]. */
    @Composable
    fun leftPlacementInset(): Dp = with(LocalDensity.current) {
        placement.getLeft(this, LocalLayoutDirection.current).toDp()
    }

    /** The scheme's physical-right placement inset, for [NutrientToolbars]. */
    @Composable
    fun rightPlacementInset(): Dp = with(LocalDensity.current) {
        placement.getRight(this, LocalLayoutDirection.current).toDp()
    }

    /** One margin for every floating chrome piece, so the bars keep consistent clearances. */
    val chromeMarginHorizontal: Dp = 16.dp

    /** The chrome's clearance from the inset edge it anchors to, shared by both bars. */
    val chromeMarginVertical: Dp = 12.dp

    /** Corner radius of every floating chrome piece (the main pill and the contextual bar). */
    val chromeCornerRadius: Dp = 28.dp
}

// endregion

// region The shared custom chrome

/**
 * The host-rendered main toolbar: a rounded pill docked to the top end corner, exactly as wide as
 * its icons, that expands to the full width while inline search is open and collapses again
 * afterwards. The container consumes [HostInsetScheme.topBar]; the toolbar itself opts out of
 * insets entirely, per the scheme.
 *
 * Nothing here sizes or animates the toolbar: it wraps its content, its default search bar fills
 * the width while search is open, and the toolbar animates that width change itself. (Don't add
 * `animateContentSize()` for it — that clips to the toolbar's bounds and cuts the pill's shadow.)
 */
@Composable
private fun BoxScope.HostedMainToolbar(coordinator: ToolbarCoordinator) {
    // The pill is the toolbar's own chrome, drawn inside its visibility animation, so chrome and
    // bar leave together. Its background must be opaque — an elevated transparent surface shows
    // its own shadow's silhouette straight through itself — which is what hostedToolbarColors()
    // sets on the theme below.
    NutrientTheme(colorScheme = hostedToolbarColors()) {
        NutrientMainToolbar(
            coordinator = coordinator,
            shape = RoundedCornerShape(HostInsetScheme.chromeCornerRadius),
            shadowElevation = NutrientMainToolbarDefaults.FloatingShadowElevation,
            // The container below already consumed the scheme insets — the bar adds none.
            windowInsets = NutrientMainToolbarDefaults.NoWindowInsets,
            // align is parent data: it reaches this Box only from the toolbar's outermost node.
            modifier = Modifier
                .align(Alignment.TopEnd)
                // The scheme's single inset consumption point for top-anchored chrome.
                .windowInsetsPadding(HostInsetScheme.topBar)
                .padding(NutrientMainToolbarDefaults.FloatingMargin),
            actions = { NutrientToolbarItems(coordinator, Modifier.weight(1f).padding(horizontal = 12.dp)) },
        )
    }
}

/**
 * The host-rendered contextual bar. Two treatments by kind, both free to ignore the main pill
 * because both hosts set `hideMainToolbarWhenContextualToolbarIsDocked(true)`:
 *
 * - The **document- and content-editing** bars are short, so they are the SDK's own
 *   [NutrientContextualToolbar] tucked into the top end corner. Nothing is hand-rolled: the pill
 *   shape, elevation and colors are parameters, the bar is as wide as its items, and its close
 *   button and kind-stamped dispatch come with it.
 * - The **annotation** bar carries a long tool run and docks to the **bottom** edge — a dock the
 *   SDK bar does not offer — so it stays a hand-rolled, flat, scrollable row
 *   ([HostedAnnotationBar]).
 */
@Composable
private fun BoxScope.HostedContextualBar(coordinator: ToolbarCoordinator) {
    val contextual by coordinator.contextualToolbar.collectAsState()
    // The bars keep composing during their exit fade after the flow flipped to null — latch the
    // last non-null state so the fade-out still draws the bar it dismisses.
    var lastShown by remember { mutableStateOf<ContextualToolbarState?>(null) }
    contextual?.let { lastShown = it }
    val shown = contextual ?: lastShown
    if (shown?.kind == ContextualToolbarKind.Annotation) {
        HostedAnnotationBar(coordinator, visible = contextual != null, bar = shown)
    } else {
        NutrientTheme {
            NutrientContextualToolbar(
                coordinator = coordinator,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // The scheme's single inset consumption point for top-anchored chrome.
                    .windowInsetsPadding(HostInsetScheme.topBar)
                    .padding(
                        horizontal = HostInsetScheme.chromeMarginHorizontal,
                        vertical = HostInsetScheme.chromeMarginVertical,
                    ),
                shape = RoundedCornerShape(HostInsetScheme.chromeCornerRadius),
                shadowElevation = 8.dp,
                colors = hostedToolbarColors().mainToolbar,
            )
        }
    }
}

/**
 * The annotation bar, docked to the bottom edge as a flat, scrollable row of every tool. The
 * container consumes [HostInsetScheme.bottomBar] (which includes the IME), per the scheme.
 */
@Composable
private fun BoxScope.HostedAnnotationBar(coordinator: ToolbarCoordinator, visible: Boolean, bar: ContextualToolbarState) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            // The scheme's single inset consumption point for bottom-anchored chrome.
            .windowInsetsPadding(HostInsetScheme.bottomBar)
            .padding(
                horizontal = HostInsetScheme.chromeMarginHorizontal,
                vertical = HostInsetScheme.chromeMarginVertical,
            ),
    ) {
        // Clicks from a host-rendered contextual bar must carry the shown bar's kind — each SDK
        // mode binder only handles clicks stamped with its own kind.
        val dispatch: (ToolbarAction) -> Unit = { action ->
            coordinator.dispatch(action.withContextualKind(bar.kind))
        }
        // Flatten pre-grouped submenus so every tool renders as its own button; long runs scroll.
        val flatItems = bar.items
            .flatMap { item -> if (item is ToolbarItem.Submenu) item.children else listOf(item) }
            .filter { it.state.isVisible }
        NutrientTheme(colorScheme = hostedToolbarColors()) {
            val barColor = UiTheme.colors.mainToolbar.backgroundColor
            val toolScrollState = rememberScrollState()
            // One-time peek when a scrollable bar appears: scroll a few tools in and settle
            // back, so the user SEES the row move before ever touching it. A user gesture
            // during the peek cancels it (animateScrollTo yields to touch input).
            ScrollPeekEffect(toolScrollState)
            Surface(
                shape = RoundedCornerShape(HostInsetScheme.chromeCornerRadius),
                color = barColor,
                shadowElevation = 8.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            // The scroll affordance: tools fade into the bar edge and a chevron
                            // points at the hidden ones while more content exists there.
                            .horizontalScrollEdgeFade(
                                toolScrollState,
                                color = barColor,
                                chevronColor = UiTheme.colors.mainToolbar.textColor,
                            )
                            .horizontalScroll(toolScrollState),
                    ) {
                        flatItems.filter { it.position == ToolbarItem.ItemEdge.Start }.forEach { item ->
                            NutrientToolbarItemButton(item = item, onAction = dispatch, invertedSelection = true)
                        }
                    }
                    flatItems.filter { it.position == ToolbarItem.ItemEdge.End }.forEach { item ->
                        NutrientToolbarItemButton(item = item, onAction = dispatch, invertedSelection = true)
                    }
                    NutrientToolbarItemButton(
                        item = ToolbarItem.Button(
                            actionId = "catalog.hosted_bar.close",
                            id = com.pspdfkit.R.id.pspdf__toolbar_close_button,
                            contentDescription = "Close toolbar",
                            icon = ToolbarIcon.Resource(com.pspdfkit.R.drawable.pspdf__ic_close_circled),
                        ),
                        onAction = dispatch,
                    )
                }
            }
        }
    }
}

/**
 * Plays the one-time scroll peek on [state]: once the row is measured and turns out to
 * overflow, it glides a couple of tool widths toward the end and settles back — the motion
 * itself demonstrates that the row scrolls.
 */
@Composable
private fun ScrollPeekEffect(state: ScrollState) {
    val peekPx = with(LocalDensity.current) { 72.dp.roundToPx() }
    LaunchedEffect(state) {
        // maxValue is Int.MAX_VALUE until the first measure pass reports the real overflow.
        val max = snapshotFlow { state.maxValue }.first { it != Int.MAX_VALUE }
        if (max > 0) {
            delay(SCROLL_PEEK_START_DELAY_MS.milliseconds)
            state.animateScrollTo(minOf(peekPx, max), tween(SCROLL_PEEK_LEG_MS))
            state.animateScrollTo(0, tween(SCROLL_PEEK_LEG_MS))
        }
    }
}

/** Pause before the peek, so the bar's own entrance animation finishes first. */
private const val SCROLL_PEEK_START_DELAY_MS = 450L

/** Duration of each leg of the peek (out and back). */
private const val SCROLL_PEEK_LEG_MS = 500

/**
 * The scroll affordance for a tool run: at whichever edge still has more content to scroll to,
 * the tools fade into [color] and a small chevron points toward the hidden ones. Without it a
 * full row just cuts off at the bar edge and nothing suggests the row scrolls at all.
 */
private fun Modifier.horizontalScrollEdgeFade(state: ScrollState, color: Color, chevronColor: Color, width: Dp = 36.dp): Modifier =
    drawWithContent {
        drawContent()
        val fadePx = width.toPx()
        // canScrollForward/Backward are logical (toward the end/start); map them to physical edges.
        val ltr = layoutDirection == LayoutDirection.Ltr
        val fadeRight = if (ltr) state.canScrollForward else state.canScrollBackward
        val fadeLeft = if (ltr) state.canScrollBackward else state.canScrollForward
        val chevron = 4.dp.toPx()
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        if (fadeRight) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    1f to color,
                    startX = size.width - fadePx,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - fadePx, 0f),
                size = Size(fadePx, size.height),
            )
            val x = size.width - chevron * 1.5f
            drawPath(
                path = Path().apply {
                    moveTo(x, center.y - chevron)
                    lineTo(x + chevron, center.y)
                    lineTo(x, center.y + chevron)
                },
                color = chevronColor,
                style = stroke,
            )
        }
        if (fadeLeft) {
            drawRect(
                brush = Brush.horizontalGradient(0f to color, 1f to Color.Transparent, startX = 0f, endX = fadePx),
                topLeft = Offset.Zero,
                size = Size(fadePx, size.height),
            )
            val x = chevron * 1.5f
            drawPath(
                path = Path().apply {
                    moveTo(x, center.y - chevron)
                    lineTo(x - chevron, center.y)
                    lineTo(x, center.y + chevron)
                },
                color = chevronColor,
                style = stroke,
            )
        }
    }

/** The custom chrome's color scheme, layered on the XML-derived defaults. */
@Composable
private fun hostedToolbarColors(): UiColorScheme {
    val background = MaterialTheme.colorScheme.primary
    return getUiColors().copy(
        mainToolbar = MainToolbarColors(
            backgroundColor = background,
            textColor = Color.White,
            popup = ToolbarPopupColors(backgroundColor = background),
            titleTextColor = Color.White,
        ),
    )
}

// endregion

// region PdfActivity with fully custom chrome

/**
 * A [PdfActivity] whose toolbars are entirely host-rendered. The configuration (passed by
 * [CustomToolbarsPdfActivityExample]) disables both SDK-mounted bars; the SDK still populates
 * the **activity-scoped** [ToolbarCoordinator] with the default items and the contextual state
 * on mode changes, so the overlay chrome below stays fully functional.
 */
class CustomChromePdfActivity : PdfActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // PdfActivity themes itself from its configuration, so the system bar icons have to
        // follow the same answer. Both SDK bars are off here and the host chrome floats below the
        // strip, so the icons land on the document — left to the platform they would follow the
        // device's night mode and turn white over this light page.
        //
        // Before super.onCreate, like every other host here: PdfActivity.getConfiguration() falls
        // back to the intent extra until its implementation exists, so it already answers here.
        enableConfigurationEdgeToEdge(configuration)
        super.onCreate(savedInstanceState)
        // The same coordinator instance PdfUiImpl populates for this activity.
        val coordinator = nutrientToolbarCoordinator()
        val overlay = ComposeView(this).apply {
            setContent {
                CatalogTheme(darkTheme = configuration.isNightTheme) {
                    Box(Modifier.fillMaxSize()) {
                        HostedMainToolbar(coordinator)
                        HostedContextualBar(coordinator)
                    }
                }
            }
        }
        // Overlay the custom chrome above the SDK content. Touches outside the bars fall
        // through to the document.
        addContentView(overlay, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
}

// endregion

// region Bare PdfFragment with SDK toolbars (Compose scaffold or View interop)

/**
 * Hosts a plain [PdfFragment] with the SDK toolbars, in one of two shapes:
 *
 * **Compose scaffold (default):** the host mounts [NutrientToolbars] — the same single-tree
 * scaffold `PdfActivity` uses internally:
 * - The host reads [HostInsetScheme] once and passes it as the scaffold's placement-inset
 *   scalars; the scaffold derives everything from them (top dock strip, side-dock bounds,
 *   drop zones, vertical-fit correction, content inset).
 * - The document container pads itself by [LocalToolbarContentInset] — the scaffold's contract
 *   for keeping content clear of a top-docked bar.
 *
 * **View interop ([EXTRA_VIEW_INTEROP]):** a classic View layout mounts the same toolbar stack
 * as a `ComposeView` via [nutrientToolbarsComposeView], with the full docking contract
 * `PdfActivity` has — drag handle, drop-zone chrome while dragging, dock placement and
 * persistence. The view paints nothing while no contextual toolbar is shown, and touches pass
 * through empty areas to the fragment below.
 *
 * Both shapes share every other piece, wired against the activity-scoped [ToolbarCoordinator]:
 * - [PdfFragmentToolbarBinder] keeps the host's undo/redo items and tool selection in sync;
 *   [AnnotationToolbarBinder] shows the SDK annotation bar when annotating starts (the
 *   fragment's [OnAnnotatingModeChangeListener] tells us when to attach and detach).
 * - [DefaultAnnotationCreationInspectorController] / [DefaultAnnotationEditingInspectorController]
 *   provide the annotation property inspector on a [PropertyInspectorCoordinatorLayout].
 */
class ScaffoldHostedFragmentActivity :
    AppCompatActivity(),
    OnAnnotatingModeChangeListener {
    private var fragment: PdfFragment? = null
    private lateinit var annotationToolbarBinder: AnnotationToolbarBinder
    private lateinit var fragmentBinder: PdfFragmentToolbarBinder
    private lateinit var inspectorCoordinatorLayout: PropertyInspectorCoordinatorLayout
    private lateinit var annotationCreationInspectorController: AnnotatingInspectorController
    private lateinit var annotationEditingInspectorController: AnnotatingInspectorController
    private val subscriptions = mutableListOf<ToolbarSubscription>()
    private var creationToolbarShown = false
    private var editingInspectorBound = false
    private var fragmentBinderAttached = false

    /** The resolved theme of the hosted configuration — see `CatalogTheme` in [mountComposeScaffold]. */
    private var isNightTheme = false

    /** Attaches/detaches the fragment binder at its documented lifecycle points. */
    private val lifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            if (f === fragment && !fragmentBinderAttached) {
                fragmentBinder.attach(f)
                fragmentBinderAttached = true
            }
        }

        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
            if (f === fragment && fragmentBinderAttached) {
                fragmentBinder.detach()
                fragmentBinderAttached = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val activityConfiguration = intent.getSupportParcelableExtra(EXTRA_CONFIGURATION, PdfActivityConfiguration::class.java)
            ?: PdfActivityConfiguration.Builder(this).build()
        // A bare-fragment host gets no PdfActivity theme handling — apply the configured theme
        // ourselves, so the SDK toolbars, the inspector, and the fragment pick up the catalog
        // styling instead of the application theme.
        isNightTheme = activityConfiguration.isNightTheme
        applyConfigurationTheme(activityConfiguration)
        enableConfigurationEdgeToEdge(activityConfiguration, statusBarIsDark = hasDarkMainToolbar())
        super.onCreate(savedInstanceState)
        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!
        val viewInterop = intent.getBooleanExtra(EXTRA_VIEW_INTEROP, false)
        val pdfConfiguration = activityConfiguration.configuration

        val coordinator = nutrientToolbarCoordinator()
        annotationToolbarBinder = AnnotationToolbarBinder(coordinator)
        fragmentBinder = PdfFragmentToolbarBinder(coordinator)
        supportFragmentManager.registerFragmentLifecycleCallbacks(lifecycleCallbacks, false)

        // The annotation property inspector: controllers bind to the AnnotatingController when
        // annotating starts (see the OnAnnotatingModeChangeListener callbacks below) and render
        // into this coordinator layout, overlaid above the document.
        inspectorCoordinatorLayout = PropertyInspectorCoordinatorLayout(this).apply {
            elevation = 16.dp.value * resources.displayMetrics.density
        }
        annotationCreationInspectorController = DefaultAnnotationCreationInspectorController(this, inspectorCoordinatorLayout)
        annotationEditingInspectorController = DefaultAnnotationEditingInspectorController(this, inspectorCoordinatorLayout)

        val fragment = (supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? PdfFragment)
            ?: PdfFragment.newInstance(uri, pdfConfiguration)
        this.fragment = fragment
        fragment.addOnAnnotatingModeChangeListener(this)

        // The host owns the main-toolbar contents in a bare-fragment integration: title plus
        // annotate / undo / redo. The undo and redo items reuse the binder's stable action IDs
        // so PdfFragmentToolbarBinder keeps their enabled state in sync automatically.
        // One activity backs both catalog entries, so name the shape it was launched for — in the
        // toolbar and in the task label the recents screen shows (the manifest can only carry one).
        val exampleTitle = getString(
            if (viewInterop) R.string.customToolbarsPdfFragmentViewExampleTitle else R.string.customToolbarsPdfFragmentExampleTitle,
        )
        setTitle(exampleTitle)
        coordinator.setMainToolbarTitle(
            getString(
                if (viewInterop) {
                    R.string.customToolbarsPdfFragmentViewMainToolbarTitle
                } else {
                    R.string.customToolbarsPdfFragmentMainToolbarTitle
                },
            ),
        )
        coordinator.setMainToolbarItems(
            ToolbarItems(
                visible = persistentListOf(
                    ToolbarItem.Button(
                        actionId = "catalog.hosted_fragment.annotate",
                        id = R.id.custom_action1,
                        contentDescription = "Toggle annotation mode",
                        icon = ToolbarIcon.Resource(com.pspdfkit.R.drawable.pspdf__ic_edit_annotations),
                        overflowBehavior = ToolbarItem.OverflowBehavior.AlwaysShow,
                    ),
                    ToolbarItem.Button(
                        actionId = PdfFragmentToolbarBinder.ACTION_UNDO,
                        id = R.id.custom_action2,
                        contentDescription = "Undo",
                        icon = ToolbarIcon.Resource(com.pspdfkit.R.drawable.pspdf__ic_undo),
                        overflowBehavior = ToolbarItem.OverflowBehavior.AlwaysShow,
                    ),
                    ToolbarItem.Button(
                        actionId = PdfFragmentToolbarBinder.ACTION_REDO,
                        id = R.id.custom_action3,
                        contentDescription = "Redo",
                        icon = ToolbarIcon.Resource(com.pspdfkit.R.drawable.pspdf__ic_redo),
                        overflowBehavior = ToolbarItem.OverflowBehavior.AlwaysShow,
                    ),
                ),
            ),
        )
        subscriptions += coordinator.addItemClickHandler { id ->
            val f = this.fragment ?: return@addItemClickHandler
            when (id) {
                R.id.custom_action1 -> if (creationToolbarShown) f.exitCurrentlyActiveMode() else f.enterAnnotatingMode()
                R.id.custom_action2 -> lifecycleScope.launch { f.undoManager.undo() }
                R.id.custom_action3 -> lifecycleScope.launch { f.undoManager.redo() }
            }
        }

        if (viewInterop) {
            mountViewInterop(coordinator, fragment)
        } else {
            mountComposeScaffold(coordinator, fragment)
        }
    }

    /** The Compose-scaffold shape: [NutrientToolbars] owns placement; content pads itself. */
    private fun mountComposeScaffold(coordinator: ToolbarCoordinator, fragment: PdfFragment) {
        setContent {
            CatalogTheme(darkTheme = isNightTheme) {
                NutrientToolbars(
                    coordinator = coordinator,
                    // The shared scheme, applied once at the scaffold seam — every slot inside
                    // (main toolbar, top/side contextual docks, drop zones, fit correction)
                    // derives from these scalars.
                    topWindowInset = HostInsetScheme.topPlacementInset(),
                    bottomWindowInset = HostInsetScheme.bottomPlacementInset(),
                    leftWindowInset = HostInsetScheme.leftPlacementInset(),
                    rightWindowInset = HostInsetScheme.rightPlacementInset(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // The scaffold's content contract: consume LocalToolbarContentInset so the
                    // document clears a top-docked bar; the document view itself adds nothing.
                    AndroidView(
                        factory = { context ->
                            FrameLayout(context).apply {
                                addView(
                                    FragmentContainerView(context).apply { id = R.id.fragmentContainer },
                                    matchParentParams(),
                                )
                                addView(inspectorCoordinatorLayout, matchParentParams())
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = LocalToolbarContentInset.current),
                    )
                    LaunchedEffect(Unit) {
                        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
                            supportFragmentManager.commit { add(R.id.fragmentContainer, fragment) }
                        }
                    }
                }
            }
        }
    }

    /** The View-interop shape: the toolbar stack is a `ComposeView` in a classic View tree. */
    private fun mountViewInterop(coordinator: ToolbarCoordinator, fragment: PdfFragment) {
        val root = FrameLayout(this)
        root.addView(FragmentContainerView(this).apply { id = R.id.fragmentContainer }, matchParentParams())
        // Mount the SDK toolbar stack over the document: docking wiring follows the view's
        // window-attach lifecycle, so there is nothing to release on teardown.
        root.addView(nutrientToolbarsComposeView(this, coordinator, showMainToolbar = true), matchParentParams())
        root.addView(inspectorCoordinatorLayout, matchParentParams())
        setContentView(root)
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            supportFragmentManager.commit { add(R.id.fragmentContainer, fragment) }
        }
    }

    private fun matchParentParams() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
    )

    override fun onDestroy() {
        // Release everything we bound before handing off to the framework teardown.
        fragment?.removeOnAnnotatingModeChangeListener(this)
        if (creationToolbarShown) annotationToolbarBinder.detach()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(lifecycleCallbacks)
        subscriptions.forEach { it.close() }
        subscriptions.clear()
        fragment = null
        super.onDestroy()
    }

    override fun onEnterAnnotatingMode(controller: AnnotatingController) {
        // An annotation tool is active: show the creation toolbar and bind its inspector.
        if (controller.activeAnnotationTool != null && !creationToolbarShown) {
            annotationCreationInspectorController.bindController(controller)
            annotationToolbarBinder.attach(controller)
            creationToolbarShown = true
        }
        // Annotations are selected: bind the editing inspector.
        if (controller.hasCurrentlySelectedAnnotations() && !editingInspectorBound) {
            annotationEditingInspectorController.bindController(controller)
            editingInspectorBound = true
        }
    }

    override fun onChangeAnnotatingMode(controller: AnnotatingController) {
        if (controller.hasCurrentlySelectedAnnotations() && !editingInspectorBound) {
            annotationEditingInspectorController.bindController(controller)
            editingInspectorBound = true
        } else if (!controller.hasCurrentlySelectedAnnotations() && editingInspectorBound) {
            annotationEditingInspectorController.unbindController()
            editingInspectorBound = false
        }
    }

    override fun onExitAnnotatingMode(controller: AnnotatingController) {
        if (controller.activeAnnotationTool == null && creationToolbarShown) {
            annotationToolbarBinder.detach()
            annotationCreationInspectorController.unbindController()
            creationToolbarShown = false
        }
        if (!controller.hasCurrentlySelectedAnnotations() && editingInspectorBound) {
            annotationEditingInspectorController.unbindController()
            editingInspectorBound = false
        }
    }

    companion object {
        const val EXTRA_URI = "ScaffoldHostedFragmentActivity.DocumentUri"
        const val EXTRA_CONFIGURATION = "ScaffoldHostedFragmentActivity.ActivityConfiguration"
        const val EXTRA_VIEW_INTEROP = "ScaffoldHostedFragmentActivity.ViewInterop"
    }
}

// endregion

// region DocumentView with fully custom toolbars

/**
 * The Compose [DocumentView] with both SDK bars disabled and everything host-rendered. The
 * fragment-scoped [ToolbarCoordinator] is published through
 * `documentState.toolbarCoordinator` once the SDK UI initializes; the SDK keeps populating it
 * (items, contextual state) with the defaults disabled.
 */
class CustomToolbarsDocumentViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val baseConfiguration = intent.getSupportParcelableExtra(EXTRA_CONFIG, PdfActivityConfiguration::class.java)
        // Both SDK toolbars are off here, but the fragment behind the DocumentView still renders
        // SDK UI that reads the *activity's* XML theme — the annotation inspector, the popup
        // toolbars, the page chrome. Apply the configured catalog theme before anything inflates.
        applyConfigurationTheme(baseConfiguration)
        enableConfigurationEdgeToEdge(baseConfiguration)
        super.onCreate(savedInstanceState)
        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!

        // Layer the example's needs on top of the catalog-prepared configuration.
        val configuration = (baseConfiguration?.let { PdfActivityConfiguration.Builder(it) } ?: PdfActivityConfiguration.Builder(this))
            .defaultToolbarEnabled(false)
            .defaultContextualToolbarEnabled(false)
            .documentTitleOverlayEnabled(false)
            // The host-rendered pill follows the frame: hide it while any contextual bar shows.
            .hideMainToolbarWhenContextualToolbarIsDocked(true)
            .build()

        setContent {
            CatalogTheme(darkTheme = baseConfiguration.isNightTheme) {
                val documentState = rememberDocumentState(uri, configuration)
                Box(Modifier.fillMaxSize()) {
                    DocumentView(
                        documentState = documentState,
                        modifier = Modifier.fillMaxSize(),
                    )
                    val coordinator by documentState.toolbarCoordinator.collectAsState()
                    coordinator?.let { c ->
                        HostedMainToolbar(c)
                        HostedContextualBar(c)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_URI = "CustomToolbarsDocumentViewActivity.DocumentUri"
        const val EXTRA_CONFIG = "CustomToolbarsDocumentViewActivity.Configuration"
    }
}

// endregion
