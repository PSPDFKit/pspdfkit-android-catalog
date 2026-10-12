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
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
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
import com.pspdfkit.compose.toolbar.composables.NutrientToolbarItemButton
import com.pspdfkit.compose.toolbar.state.ContextualToolbarKind
import com.pspdfkit.compose.toolbar.state.ContextualToolbarState
import com.pspdfkit.compose.toolbar.state.ToolbarAction
import com.pspdfkit.compose.toolbar.state.ToolbarIcon
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.compose.toolbar.state.withContextualKind
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.utils.getSupportParcelableExtra
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * A user-customizable annotation bar: the SDK's contextual toolbars are replaced by a floating
 * bottom bar whose tools the user picks and arranges — with drag and drop — and whose
 * arrangement is permanent:
 *
 * - `defaultContextualToolbarEnabled(false)` stops the SDK from mounting its contextual bars;
 *   it keeps *populating* the shared [com.pspdfkit.compose.toolbar.adapter.ToolbarCoordinator]
 *   (annotation mode state, items) either way. The SDK main toolbar stays.
 * - The bar shows the annotation tools flat and ungrouped (no submenu folding, no "More"
 *   group) — long tool rows scroll. Other contextual bars (document editing, content editing)
 *   render in the same bar with their default items.
 * - A custom "Annotation tools" main-toolbar item (added through
 *   [DocumentState.setToolbarItemsProvider]) opens a **bottom sheet** with all annotation tools
 *   in a grid. Tapping a tool toggles its selection — the bar shows exactly the selected tools.
 *   Long-press and **drag a tool onto the bar** to place it at an exact position.
 * - The selection and order are saved to [SharedPreferences] and restored on the next launch.
 *   Tool colors are the SDK's annotation preferences (shown live in the style-indicator
 *   icons), which the SDK already persists — so the arrangement and the colors are permanent.
 *
 * For the plain hosting shapes (custom bars without the authoring UI) see
 * [CustomToolbarsDocumentViewExample] and [CustomToolbarsPdfActivityExample].
 */
class AnnotationToolbarUserEditableExample(context: Context) :
    SdkExample(context, R.string.annotationToolbarUserEditableExampleTitle, R.string.annotationToolbarUserEditableExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, AnnotationToolbarUserEditableActivity::class.java)
                .putExtra(AnnotationToolbarUserEditableActivity.EXTRA_URI, Uri.fromFile(documentFile))
                // The catalog-prepared configuration carries the catalog theme and the user's
                // preference toggles — pass it through instead of rebuilding.
                .putExtra(AnnotationToolbarUserEditableActivity.EXTRA_CONFIG, configuration.build())
            context.startActivity(intent)
        }
    }
}

class AnnotationToolbarUserEditableActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val baseConfiguration = intent.getSupportParcelableExtra(EXTRA_CONFIG, PdfActivityConfiguration::class.java)
        // The SDK renders the main toolbar in this example, and it takes its colors from the
        // *activity's* XML theme — which a plain AppCompatActivity never gets from the
        // configuration. Apply the configured catalog theme before any content inflates, so the
        // main toolbar matches the rest of the catalog.
        applyConfigurationTheme(baseConfiguration)
        enableConfigurationEdgeToEdge(baseConfiguration, statusBarIsDark = hasDarkMainToolbar())
        super.onCreate(savedInstanceState)

        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!
        val configuration = (baseConfiguration?.let { PdfActivityConfiguration.Builder(it) } ?: PdfActivityConfiguration.Builder(this))
            // The SDK main toolbar stays; only the contextual bars are host-rendered below.
            .defaultContextualToolbarEnabled(false)
            .documentTitleOverlayEnabled(false)
            .build()

        setContent {
            CatalogTheme(darkTheme = baseConfiguration.isNightTheme) {
                AnnotationToolbarUserEditableDocument(uri, configuration)
            }
        }
    }

    companion object {
        const val EXTRA_URI = "AnnotationToolbarUserEditableActivity.DocumentUri"
        const val EXTRA_CONFIG = "AnnotationToolbarUserEditableActivity.Configuration"
    }
}

/** A tool being dragged out of the sheet: the item and the finger position in root coordinates. */
private data class ToolDrag(val item: ToolbarItem, val position: Offset)

@Composable
private fun AnnotationToolbarUserEditableDocument(uri: Uri, configuration: PdfActivityConfiguration) {
    val documentState = rememberDocumentState(uri, configuration)
    // The DocumentView publishes its fragment-scoped toolbar coordinator once the UI is ready —
    // the same instance the SDK populates, so the custom bar below stays in sync with it.
    val coordinator by documentState.toolbarCoordinator.collectAsState()

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(TOOLBAR_PREFS_NAME, Context.MODE_PRIVATE) }
    // The permanent bar arrangement: the selected tools' actionIds in bar order — membership AND
    // order in one list. Written on every grid toggle and every successful drop, reloaded on the
    // next launch. `null` means "never customized": every tool shows in its default order.
    var toolOrder by remember { mutableStateOf(loadToolOrder(prefs)) }
    var toolsSheetShown by rememberSaveable { mutableStateOf(false) }

    // The custom "Annotation tools" main-toolbar item and its click handling. Values are read
    // through documentState at click time, so a single registration stays correct.
    LaunchedEffect(documentState) {
        documentState.setToolbarItemsProvider { defaultItems ->
            defaultItems.toMutableList().apply {
                add(
                    1,
                    ToolbarItem.Button(
                        actionId = TOOLS_SHEET_ACTION_ID,
                        id = R.id.custom_action1,
                        contentDescription = "Annotation tools",
                        icon = ToolbarIcon.Vector(Icons.Default.Build),
                        overflowBehavior = ToolbarItem.OverflowBehavior.AlwaysShow,
                    ),
                )
            }
        }
        documentState.setOnToolbarItemClickListener { id ->
            if (id == R.id.custom_action1) {
                toolsSheetShown = true
                // The tool grid is built from the annotation toolbar's items, so make sure
                // annotation mode is active — same as tapping the default annotate item.
                val c = documentState.toolbarCoordinator.value
                if (c != null && c.contextualToolbar.value == null) {
                    c.dispatch(ToolbarAction.ItemClicked(PdfActivity.MENU_OPTION_EDIT_ANNOTATIONS))
                }
                true
            } else {
                false
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        DocumentView(
            documentState = documentState,
            modifier = Modifier.fillMaxSize(),
        )

        coordinator?.let { c ->
            val contextual by c.contextualToolbar.collectAsState()

            // AnimatedVisibility keeps composing during the exit fade after the flow flipped to
            // null — latch the last non-null state so the fade-out still draws the bar it
            // dismisses (the same pattern NutrientContextualToolbar uses).
            var lastShownContextual by remember { mutableStateOf<ContextualToolbarState?>(null) }
            contextual?.let { lastShownContextual = it }
            val shownContextual = contextual ?: lastShownContextual
            val isAnnotationBar = shownContextual?.kind == ContextualToolbarKind.Annotation

            // "No grouping": flatten any pre-grouped submenu into its children and render every
            // visible item as its own button. (The annotation list carries an empty undo/redo
            // group shell next to the flat undo/redo buttons — flattening drops the shell.)
            val flatItems = shownContextual?.items.orEmpty()
                .flatMap { item -> if (item is ToolbarItem.Submenu) item.children else listOf(item) }
                .filter { it.state.isVisible }
            val tools = flatItems.filter { it.position == ToolbarItem.ItemEdge.Start }
            val endItems = flatItems.filter { it.position == ToolbarItem.ItemEdge.End }
            // The persisted selection and arrangement only apply to the annotation bar's tools;
            // other contextual bars (document editing, content editing) keep their defaults.
            val orderedTools = if (isAnnotationBar) barToolsFor(tools, toolOrder) else tools

            // Drop-zone geometry, all in root coordinates: the bar's bounds decide whether a
            // release is a drop, the per-slot bounds decide the insertion index.
            var barBounds by remember { mutableStateOf<Rect?>(null) }
            val slotBounds = remember { mutableStateMapOf<String, Rect>() }
            var drag by remember { mutableStateOf<ToolDrag?>(null) }
            val dropSlopPx = with(LocalDensity.current) { DROP_ZONE_SLOP.toPx() }
            val hoverIndex = drag?.let {
                dropIndexFor(it.position, barBounds, orderedTools, slotBounds, dropSlopPx)
            }

            // Dismiss the sheet when annotation mode exits (bar closed) — but not while it is
            // still being entered, so track the non-null → null transition explicitly. The
            // change listener is the callback counterpart of collecting
            // `coordinator.contextualToolbar` — used here for the show/hide *transitions*
            // (rendering above stays state-driven via collectAsState).
            var wasInMode by remember { mutableStateOf(false) }
            DisposableEffect(c) {
                val subscription = c.addContextualToolbarChangeListener { state ->
                    if (state == null && wasInMode) toolsSheetShown = false
                    wasInMode = state != null
                }
                onDispose { subscription.close() }
            }
            // Back unwinds like PdfActivity: exit the active mode (annotation / document /
            // content editing) before backing out of the example. Dispatching the close-button
            // click routes through the same SDK mode binder the bar's own X uses. Registered
            // BEFORE the sheet handler — the last enabled BackHandler wins, so an open tools
            // sheet closes first, then the mode, then the activity.
            BackHandler(enabled = contextual != null) {
                contextual?.let { state ->
                    c.dispatch(
                        ToolbarAction.ItemClicked(com.pspdfkit.R.id.pspdf__toolbar_close_button)
                            .withContextualKind(state.kind),
                    )
                }
            }
            BackHandler(enabled = toolsSheetShown) { toolsSheetShown = false }

            var sheetHeightPx by remember { mutableIntStateOf(0) }
            // Whether the sheet is dragged up to its full height; reopening starts at the peek.
            var sheetExpanded by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(toolsSheetShown) { if (!toolsSheetShown) sheetExpanded = false }
            val expandedGridHeight = with(LocalDensity.current) {
                (constraints.maxHeight * SHEET_EXPANDED_FRACTION).toDp()
            }
            val barToolIds = orderedTools.map { it.actionId }
            AnnotationToolsSheet(
                visible = toolsSheetShown && contextual != null && isAnnotationBar,
                tools = tools,
                isInBar = { tool -> tool.actionId in barToolIds },
                expanded = sheetExpanded,
                onExpandedChange = { sheetExpanded = it },
                expandedGridHeight = expandedGridHeight,
                onDismiss = { toolsSheetShown = false },
                onToolToggle = { tool ->
                    // Selection is membership: selected tools appear on the bar, in the
                    // persisted order; deselected ones are removed from it.
                    val ids = if (tool.actionId in barToolIds) barToolIds - tool.actionId else barToolIds + tool.actionId
                    toolOrder = ids
                    saveToolOrder(prefs, ids)
                },
                onDragStart = { tool, position -> drag = ToolDrag(tool, position) },
                onDragBy = { delta -> drag = drag?.let { it.copy(position = it.position + delta) } },
                onDragEnd = {
                    drag?.let { d ->
                        val index = dropIndexFor(d.position, barBounds, orderedTools, slotBounds, dropSlopPx)
                        if (index != null) {
                            val ids = orderedTools.map { it.actionId }.toMutableList()
                            val from = ids.indexOf(d.item.actionId)
                            var insertAt = index
                            if (from >= 0) {
                                ids.removeAt(from)
                                if (from < insertAt) insertAt--
                            }
                            ids.add(insertAt.coerceIn(0, ids.size), d.item.actionId)
                            toolOrder = ids
                            // The drop is permanent: the arrangement is reloaded on next launch.
                            saveToolOrder(prefs, ids)
                        }
                    }
                    drag = null
                },
                onDragCancel = { drag = null },
                onHeightChanged = { sheetHeightPx = it },
            )

            // The floating annotation bar, drawn above the sheet so tools can be dropped on it.
            // While the sheet is open the bar floats just above it; otherwise it rests against
            // the bottom edge.
            val density = LocalDensity.current
            // The placement chain below already pads the bar by the bottom system inset, and the
            // sheet's measured height includes that same inset (its content column is
            // navigationBarsPadding-ded) — subtract it once so the lifted bar floats exactly
            // 12dp above the sheet instead of 12dp plus a second inset.
            val bottomInset = with(density) { WindowInsets.systemBarsIgnoringVisibility.getBottom(density).toDp() }
            val barBottomPadding by animateDpAsState(
                targetValue = if (toolsSheetShown && contextual != null) {
                    (with(density) { sheetHeightPx.toDp() } + 12.dp - bottomInset).coerceAtLeast(0.dp)
                } else {
                    24.dp
                },
                animationSpec = tween(durationMillis = BAR_LIFT_ANIMATION_MS),
                label = "barBottomPadding",
            )
            NutrientTheme(colorScheme = annotationBarColors()) {
                AnimatedVisibility(
                    visible = contextual != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    // STABLE system-bar insets (systemBarsIgnoringVisibility, not the live
                    // systemBars): the SDK hides the system UI while creating annotations
                    // (hideUserInterfaceWhenCreatingAnnotations), and live insets animate to
                    // zero then — the bar would slide down flush to the screen edge. Stable
                    // insets keep the full bar heights while hidden — the same policy the SDK
                    // toolbar scaffold uses — so the docked bar holds its position through
                    // immersive transitions.
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = barBottomPadding)
                        .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility),
                ) {
                    // Never null once the bar composes: visibility requires a shown state, and
                    // the latched state stays non-null through the exit fade.
                    val shown = shownContextual ?: return@AnimatedVisibility
                    FloatingAnnotationBar(
                        orderedTools = orderedTools,
                        endItems = endItems,
                        hoverIndex = hoverIndex,
                        // Clicks from a host-rendered contextual bar must carry the *shown* bar's
                        // kind — each SDK mode binder only handles clicks stamped with its own kind
                        // (and ignores unstamped main-toolbar clicks), so stamping a fixed kind
                        // would break the document/content editing bars this example also renders.
                        dispatch = { action -> c.dispatch(action.withContextualKind(shown.kind)) },
                        onBarBounds = { barBounds = it },
                        onSlotBounds = { actionId, bounds -> slotBounds[actionId] = bounds },
                    )
                }
            }

            // The drag ghost following the finger, always on top.
            drag?.let { DragGhost(it) }
        }
    }
}

/**
 * The floating annotation bar: every selected tool as its own button (no grouping), long tool
 * runs scroll; picker / stylus / undo / redo and the close button pin to the end. Reports its
 * own bounds and each tool slot's bounds (in root coordinates) for the sheet's drop targeting,
 * and renders the insertion indicator while a dragged tool hovers it.
 */
@Composable
private fun FloatingAnnotationBar(
    orderedTools: List<ToolbarItem>,
    endItems: List<ToolbarItem>,
    hoverIndex: Int?,
    dispatch: (ToolbarAction) -> Unit,
    onBarBounds: (Rect) -> Unit,
    onSlotBounds: (String, Rect) -> Unit,
) {
    val colors = UiTheme.colors.mainToolbar
    val toolScrollState = rememberScrollState()
    // One-time peek when a scrollable bar appears: scroll a few tools in and settle back, so
    // the user SEES the row move before ever touching it.
    ScrollPeekEffect(toolScrollState)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.backgroundColor,
        shadowElevation = 8.dp,
        modifier = Modifier.onGloballyPositioned { onBarBounds(it.boundsInRoot()) },
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
                    // The scroll affordance: tools fade into the bar edge and a chevron points
                    // at the hidden ones — otherwise nothing suggests the row scrolls at all.
                    .horizontalScrollEdgeFade(
                        toolScrollState,
                        color = colors.backgroundColor,
                        chevronColor = colors.textColor,
                    )
                    .horizontalScroll(toolScrollState),
            ) {
                orderedTools.forEachIndexed { index, tool ->
                    if (hoverIndex == index) DropIndicator()
                    NutrientToolbarItemButton(
                        item = tool,
                        onAction = dispatch,
                        invertedSelection = true,
                        modifier = Modifier.onGloballyPositioned { onSlotBounds(tool.actionId, it.boundsInRoot()) },
                    )
                }
                if (hoverIndex == orderedTools.size) DropIndicator()
            }
            endItems.forEach { item ->
                NutrientToolbarItemButton(item = item, onAction = dispatch, invertedSelection = true)
            }
            NutrientToolbarItemButton(
                item = ToolbarItem.Button(
                    actionId = "catalog.annotation_bar.close",
                    id = com.pspdfkit.R.id.pspdf__toolbar_close_button,
                    contentDescription = "Close annotation toolbar",
                    icon = ToolbarIcon.Resource(com.pspdfkit.R.drawable.pspdf__ic_close_circled),
                ),
                onAction = dispatch,
            )
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
 * the tools fade into [color] and a small chevron points toward the hidden ones.
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

/** The insertion marker shown between bar slots while a dragged tool hovers the bar. */
@Composable
private fun DropIndicator() {
    Box(
        Modifier
            .size(width = 3.dp, height = 36.dp)
            .background(Color.White, RoundedCornerShape(2.dp)),
    )
}

/**
 * The bottom sheet listing every annotation tool in a grid. Tap a tool to select or deselect
 * it — selected tools (marked with a check badge) are the ones shown on the floating bar.
 * Long-press and drag one onto the floating bar to place it at an exact position.
 *
 * The sheet's header is draggable, like a standard bottom sheet: drag up to expand the grid to
 * [expandedGridHeight], drag down to collapse an expanded sheet back to its peek height, and
 * drag further down (or tap the scrim / press back) to dismiss it.
 */
@Composable
private fun BoxScope.AnnotationToolsSheet(
    visible: Boolean,
    tools: List<ToolbarItem>,
    isInBar: (ToolbarItem) -> Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    expandedGridHeight: Dp,
    onDismiss: () -> Unit,
    onToolToggle: (ToolbarItem) -> Unit,
    onDragStart: (ToolbarItem, Offset) -> Unit,
    onDragBy: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onHeightChanged: (Int) -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        )
    }
    // How far the sheet is pulled down from its resting position, in pixels. Dragging translates
    // the sheet live; releasing either settles it back, collapses an expanded sheet, or
    // dismisses it, depending on how far it traveled.
    val sheetPull = remember { Animatable(0f) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val dismissThresholdPx = with(LocalDensity.current) { SHEET_DISMISS_DRAG_THRESHOLD.toPx() }
    val expandThresholdPx = with(LocalDensity.current) { SHEET_EXPAND_DRAG_THRESHOLD.toPx() }
    // The exit animation plays from wherever the sheet was released — reset the pull once hidden.
    LaunchedEffect(visible) { if (!visible) sheetPull.snapTo(0f) }
    // The gesture coroutine below never restarts (pointerInput(Unit)), so read everything it
    // decides on through updated state — otherwise it would settle against a stale expansion.
    val currentExpanded by rememberUpdatedState(expanded)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val gridMaxHeight by animateDpAsState(
        targetValue = if (expanded) expandedGridHeight else SHEET_PEEK_GRID_HEIGHT,
        label = "sheetGridHeight",
    )
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { onHeightChanged(it.height) }
                .graphicsLayer { translationY = sheetPull.value },
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
            ) {
                // The draggable header: grab handle, title, and hint.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            fun settle() = scope.launch { sheetPull.animateTo(0f) }
                            detectVerticalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onVerticalDrag = { change, delta ->
                                    change.consume()
                                    totalDrag += delta
                                    // Only downward travel moves the sheet; an upward drag
                                    // expands on release instead (the grid grows in place).
                                    scope.launch { sheetPull.snapTo((sheetPull.value + delta).coerceAtLeast(0f)) }
                                },
                                onDragEnd = {
                                    when {
                                        // A long downward pull: collapse an expanded sheet to
                                        // its peek, dismiss a peeked one.
                                        sheetPull.value > dismissThresholdPx -> {
                                            if (currentExpanded) {
                                                currentOnExpandedChange(false)
                                                settle()
                                            } else {
                                                currentOnDismiss()
                                            }
                                        }

                                        // A deliberate upward drag: expand to full height.
                                        totalDrag < -expandThresholdPx -> {
                                            currentOnExpandedChange(true)
                                            settle()
                                        }

                                        // A short downward pull on an expanded sheet: collapse.
                                        currentExpanded && sheetPull.value > expandThresholdPx -> {
                                            currentOnExpandedChange(false)
                                            settle()
                                        }

                                        else -> settle()
                                    }
                                },
                                onDragCancel = { settle() },
                            )
                        },
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 10.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                    )
                    Text(
                        text = "All annotation tools",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = "Tap a tool to show or hide it on the bar. Long-press and drag it onto the bar " +
                            "to place it exactly — the selection and arrangement are saved permanently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 12.dp)
                        .heightIn(max = gridMaxHeight),
                ) {
                    items(tools, key = { it.actionId }) { tool ->
                        ToolGridCell(
                            tool = tool,
                            inBar = isInBar(tool),
                            onToggle = { onToolToggle(tool) },
                            onDragStart = { position -> onDragStart(tool, position) },
                            onDragBy = onDragBy,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One grid cell: the tool's icon (with its live style-indicator color) above its name. Cells
 * are selectable — [inBar] marks the tool as shown on the floating bar with a tinted circle
 * and a check badge; tapping toggles it.
 */
@Composable
private fun ToolGridCell(
    tool: ToolbarItem,
    inBar: Boolean,
    onToggle: () -> Unit,
    onDragStart: (positionInRoot: Offset) -> Unit,
    onDragBy: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var originInRoot by remember { mutableStateOf(Offset.Zero) }
    // The pointerInput coroutines below capture their lambdas ONCE, when they launch — but the
    // callbacks are recreated on every recomposition (the toggle handler closes over the current
    // selection). Without rememberUpdatedState a cell keeps dispatching through the callback
    // from the composition it launched in, toggling against a stale selection — which reads as
    // "a different icon flipped". Read the callbacks through updated state so the gesture always
    // calls the latest ones.
    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragBy by rememberUpdatedState(onDragBy)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onGloballyPositioned { originInRoot = it.positionInRoot() }
            .pointerInput(tool.actionId) { detectTapGestures { currentOnToggle() } }
            .pointerInput(tool.actionId) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> currentOnDragStart(originInRoot + offset) },
                    onDrag = { change, delta ->
                        change.consume()
                        currentOnDragBy(delta)
                    },
                    onDragEnd = { currentOnDragEnd() },
                    onDragCancel = { currentOnDragCancel() },
                )
            }
            .padding(vertical = 8.dp),
    ) {
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (inBar) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                        CircleShape,
                    ),
            ) {
                ToolIcon(
                    icon = tool.icon,
                    tint = if (inBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (inBar) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Shown on the floating bar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                )
            }
        }
        Text(
            text = tool.contentDescription,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The dragged tool's ghost following the finger. */
@Composable
private fun DragGhost(drag: ToolDrag) {
    val halfSizePx = with(LocalDensity.current) { (DRAG_GHOST_SIZE / 2).toPx() }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp,
        modifier = Modifier
            .absoluteOffset {
                IntOffset(
                    (drag.position.x - halfSizePx).roundToInt(),
                    (drag.position.y - halfSizePx).roundToInt(),
                )
            }
            .size(DRAG_GHOST_SIZE),
    ) {
        Box(contentAlignment = Alignment.Center) {
            ToolIcon(icon = drag.item.icon, tint = Color.White)
        }
    }
}

/** Renders a [ToolbarIcon] outside the SDK's item button (grid cells and the drag ghost). */
@Composable
private fun ToolIcon(icon: ToolbarIcon, tint: Color) {
    when (icon) {
        is ToolbarIcon.Resource -> Icon(
            painter = painterResource(icon.id),
            contentDescription = null,
            tint = icon.tint ?: tint,
        )

        is ToolbarIcon.Vector -> Icon(
            imageVector = icon.image,
            contentDescription = null,
            tint = icon.tint ?: tint,
        )

        is ToolbarIcon.Composed -> icon.content(tint)

        is ToolbarIcon.None -> {}
    }
}

/**
 * The insertion index a release at [position] maps to, or `null` when the position is not a
 * drop (outside the bar plus [slopPx] of forgiveness). The index counts the tool slots whose
 * center lies before the finger along the bar's main axis.
 */
private fun dropIndexFor(
    position: Offset,
    barBounds: Rect?,
    orderedTools: List<ToolbarItem>,
    slotBounds: Map<String, Rect>,
    slopPx: Float,
): Int? {
    val bar = barBounds ?: return null
    if (!bar.inflate(slopPx).contains(position)) return null
    return orderedTools.count { tool ->
        slotBounds[tool.actionId]?.let { slot -> slot.center.x < position.x } == true
    }
}

/**
 * The tools the floating bar shows: exactly the ones in the persisted [order] (selection and
 * position in one list), or every tool in its default order when the user has never customized
 * the bar ([order] is `null`).
 */
private fun barToolsFor(tools: List<ToolbarItem>, order: List<String>?): List<ToolbarItem> {
    if (order == null) return tools
    val byActionId = tools.associateBy { it.actionId }
    return order.mapNotNull { byActionId[it] }
}

/** The persisted bar arrangement, or `null` when the user has never customized the bar. */
private fun loadToolOrder(prefs: SharedPreferences): List<String>? = if (prefs.contains(KEY_TOOL_ORDER)) {
    prefs.getString(KEY_TOOL_ORDER, null)?.split(',')?.filter { it.isNotBlank() }.orEmpty()
} else {
    null
}

private fun saveToolOrder(prefs: SharedPreferences, order: List<String>) {
    prefs.edit { putString(KEY_TOOL_ORDER, order.joinToString(",")) }
}

/**
 * The annotation bar's color scheme, layered on top of the XML-derived defaults. Swapping these
 * colors is the single place to re-skin the bar.
 */
@Composable
private fun annotationBarColors(): UiColorScheme {
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

/** Duration of the annotation bar's lift above the opening/closing tools sheet. */
private const val BAR_LIFT_ANIMATION_MS = 250

/** Extra catch area around the bar within which a release still counts as a drop. */
private val DROP_ZONE_SLOP = 24.dp

/** The tool grid's height while the sheet rests at its peek. */
private val SHEET_PEEK_GRID_HEIGHT = 320.dp

/** Fraction of the window height the tool grid grows to when the sheet is dragged up. */
private const val SHEET_EXPANDED_FRACTION = 0.72f

/** Downward pull beyond which a release collapses an expanded sheet or dismisses a peeked one. */
private val SHEET_DISMISS_DRAG_THRESHOLD = 140.dp

/** Drag travel that counts as a deliberate expand (upward) or collapse (downward) gesture. */
private val SHEET_EXPAND_DRAG_THRESHOLD = 56.dp

/** Diameter of the circular ghost following the finger during a drag. */
private val DRAG_GHOST_SIZE = 56.dp

/** actionId of the custom main-toolbar item opening the tools sheet. */
private const val TOOLS_SHEET_ACTION_ID = "catalog.annotation_tools_sheet"

/** SharedPreferences file holding the permanent bar arrangement. */
private const val TOOLBAR_PREFS_NAME = "catalog.custom_annotation_bar"

/** Preference key: comma-joined actionIds of the selected tools, in bar order. */
private const val KEY_TOOL_ORDER = "tool_order"
