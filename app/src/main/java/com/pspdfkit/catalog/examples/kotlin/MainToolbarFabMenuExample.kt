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
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.enableConfigurationEdgeToEdge
import com.pspdfkit.compose.search.composables.NutrientInlineSearchBar
import com.pspdfkit.compose.theme.NutrientTheme
import com.pspdfkit.compose.toolbar.adapter.ToolbarCoordinator
import com.pspdfkit.compose.toolbar.state.ToolbarAction
import com.pspdfkit.compose.toolbar.state.ToolbarIcon
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.search.SearchType
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.utils.getSupportParcelableExtra
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Renders the main-toolbar model as a **Material 3 Expressive FAB menu** driven directly by the
 * document's [ToolbarCoordinator]:
 *
 *  - Collapsed, a single FAB shows the *active* item's icon (the last action the user ran).
 *  - Tapping the FAB springs open a labeled menu of every toolbar action, staggered expressive-style;
 *    the FAB morphs to a close affordance and a scrim dims the document behind the menu.
 *  - Tapping a menu item dispatches its [ToolbarAction] through the coordinator, remembers it as
 *    the active item, and collapses the menu back to the FAB. Pressing back or tapping the scrim
 *    collapses the menu without dispatching.
 *  - **Long-pressing the FAB starts a drag**: it follows the finger and, on release, springs into
 *    the nearer bottom corner — bottom-right or bottom-left. The menu opens upward from either
 *    corner, with its labels aligned toward the docked side.
 */
class MainToolbarFabMenuExample(context: Context) :
    SdkExample(context, R.string.mainToolbarFabMenuExampleTitle, R.string.mainToolbarFabMenuExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, MainToolbarFabMenuActivity::class.java)
            intent.putExtra(MainToolbarFabMenuActivity.EXTRA_URI, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}

class MainToolbarFabMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!
        val configuration = PdfActivityConfiguration.Builder(this)
            .defaultToolbarEnabled(false)
            .documentTitleOverlayEnabled(false)
            .setSearchType(SearchType.INLINE)
            .build()

        enableConfigurationEdgeToEdge(configuration)
        setContent {
            MaterialTheme(colorScheme = expressiveColorScheme()) {
                FabMenuDocument(uri, configuration)
            }
        }
    }

    companion object {
        const val EXTRA_URI = "MainToolbarFabMenuActivity.DocumentUri"
    }
}

/** Dynamic (Material You) color when available (API 31+), falling back to the baseline schemes. */
@Composable
private fun expressiveColorScheme(): ColorScheme {
    val dark = isSystemInDarkTheme()
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        dark -> darkColorScheme()

        else -> lightColorScheme()
    }
}

@Composable
private fun FabMenuDocument(uri: Uri, configuration: PdfActivityConfiguration) {
    val documentState = rememberDocumentState(uri, configuration)
    val coordinator by documentState.toolbarCoordinator.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        DocumentView(
            documentState = documentState,
            modifier = Modifier.fillMaxSize(),
        )

        coordinator?.let { c ->
            NutrientTheme {
                ExpressiveFabMenu(
                    coordinator = c,
                    modifier = Modifier.fillMaxSize(),
                )
                InlineSearchTakeover(
                    coordinator = c,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

/**
 * The FAB menu: a primary-container FAB that expands into a labeled action list. All items —
 * visible and overflow buckets alike — flatten into one menu, since the FAB is the single entry
 * point. While open, a scrim covers the document; tapping it (or pressing back) collapses the
 * menu, and the item list scrolls if it outgrows the space above the FAB. Long-pressing the FAB
 * drags it; release springs it into the nearer bottom corner (right or left).
 */
@Composable
private fun ExpressiveFabMenu(coordinator: ToolbarCoordinator, modifier: Modifier = Modifier) {
    val items by coordinator.items.collectAsState()
    val frame by coordinator.frame.collectAsState()
    val searchSession by coordinator.searchSession.collectAsState()
    val searchActive = searchSession?.isActive?.collectAsState()?.value ?: false
    val allItems = (items.visible + items.overflow).filter { it.state.isVisible }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var activeItemId by rememberSaveable { mutableStateOf(NO_ACTIVE_ITEM) }

    // Which bottom corner the FAB is docked in (survives rotation and process death), plus the
    // live pixel offset of an in-flight drag / settle spring.
    var dockedLeft by rememberSaveable { mutableStateOf(false) }
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val activeItem = allItems.firstOrNull { it.id == activeItemId }
        ?: allItems.firstOrNull { it.state.isSelected }

    BackHandler(enabled = expanded) { expanded = false }

    Box(modifier = modifier) {
        // Scrim: dims the document while the menu is open; tapping it collapses the menu.
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(expressiveEffects()),
            exit = fadeOut(expressiveEffects()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .pointerInput(Unit) { detectTapGestures { expanded = false } },
            )
        }

        // The FAB (and its menu) live in the safe area inset by the edge margin, pinned to one of
        // the two bottom corners, plus the drag offset while a drag is in flight.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(FabEdgeMargin),
        ) {
            val density = LocalDensity.current
            val fabPx = with(density) { FabSize.toPx() }
            val areaWidth = constraints.maxWidth.toFloat()
            val areaHeight = constraints.maxHeight.toFloat()

            /** Top-left pixel position of the FAB when docked in the [left] bottom corner. */
            fun dockedTopLeft(left: Boolean): Offset = Offset(if (left) 0f else areaWidth - fabPx, areaHeight - fabPx)

            /** Docks into whichever bottom corner is nearer to the released position. */
            fun settleAfterDrag() {
                val current = dockedTopLeft(dockedLeft) + dragOffset.value
                val targetLeft = current.x + fabPx / 2 < areaWidth / 2
                dockedLeft = targetLeft
                scope.launch {
                    dragOffset.snapTo(current - dockedTopLeft(targetLeft))
                    dragOffset.animateTo(Offset.Zero, expressiveSpatial())
                }
            }

            val menuAlignment = if (dockedLeft) AbsoluteAlignment.Left else AbsoluteAlignment.Right
            val menuMaxHeight = with(density) { (areaHeight - fabPx).toDp() } - MenuSpacing
            var contentSize by remember { mutableStateOf(IntSize.Zero) }

            AnimatedVisibility(
                visible = frame.isVisible && !searchActive && allItems.isNotEmpty(),
                enter = fadeIn(expressiveEffects()) + scaleIn(expressiveSpatial(), initialScale = 0.6f),
                exit = fadeOut(expressiveEffects()) + scaleOut(expressiveSpatial(), targetScale = 0.6f),
                modifier = Modifier
                    // Anchor the whole column so its FAB corner stays at the docked position while
                    // the menu grows upward.
                    .absoluteOffset {
                        val position = dockedTopLeft(dockedLeft) + dragOffset.value
                        IntOffset(
                            x = (if (dockedLeft) position.x else position.x + fabPx - contentSize.width).roundToInt(),
                            y = (position.y + fabPx - contentSize.height).roundToInt(),
                        )
                    }
                    .onSizeChanged { contentSize = it },
            ) {
                Column(
                    horizontalAlignment = menuAlignment,
                    verticalArrangement = Arrangement.spacedBy(MenuSpacing),
                ) {
                    // The menu items spring open above the FAB, expanding from the bottom. The list
                    // takes at most the height left over by the FAB and scrolls beyond that.
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(expressiveEffects()) +
                            expandVertically(expressiveSpatial(), expandFrom = Alignment.Bottom) +
                            slideInVertically(expressiveSpatial()) { it / 4 },
                        exit = fadeOut(expressiveEffects()) +
                            shrinkVertically(expressiveSpatial(), shrinkTowards = Alignment.Bottom) +
                            slideOutVertically(expressiveSpatial()) { it / 4 },
                    ) {
                        Column(
                            horizontalAlignment = menuAlignment,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .heightIn(max = menuMaxHeight)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            allItems.forEach { item ->
                                FabMenuItem(
                                    item = item,
                                    onClick = {
                                        expanded = false
                                        activeItemId = item.id
                                        coordinator.dispatch(ToolbarAction.ItemClicked(item.id))
                                    },
                                )
                            }
                        }
                    }

                    // The FAB itself: active item's icon while collapsed, close affordance while
                    // open. Long-press starts the drag; release snaps to the nearer bottom corner.
                    FloatingActionButton(
                        onClick = { expanded = !expanded },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(FabSize)
                            .pointerInput(areaWidth, areaHeight) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expanded = false
                                    },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        scope.launch { dragOffset.snapTo(dragOffset.value + delta) }
                                    },
                                    onDragEnd = { settleAfterDrag() },
                                    onDragCancel = {
                                        scope.launch { dragOffset.animateTo(Offset.Zero, expressiveSpatial()) }
                                    },
                                )
                            },
                    ) {
                        AnimatedContent(
                            targetState = expanded,
                            transitionSpec = {
                                (fadeIn(expressiveEffects()) + scaleIn(expressiveSpatial(), initialScale = 0.4f))
                                    .togetherWith(fadeOut(expressiveEffects()) + scaleOut(expressiveSpatial(), targetScale = 0.4f))
                            },
                            label = "fabIcon",
                        ) { open ->
                            when {
                                open -> Icon(
                                    painter = painterResource(com.pspdfkit.R.drawable.pspdf__ic_close),
                                    contentDescription = stringResource(R.string.mainToolbarFabMenuClose),
                                )

                                activeItem != null -> FabItemIcon(activeItem.icon)

                                else -> Icon(
                                    painter = painterResource(com.pspdfkit.R.drawable.pspdf__ic_more),
                                    contentDescription = stringResource(R.string.mainToolbarFabMenuOpen),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One menu row: a full-corner pill with the item's icon and label. Selected items (e.g. the active
 * annotation tool) render on the tertiary container so the coordinator's state stays visible.
 */
@Composable
private fun FabMenuItem(item: ToolbarItem, onClick: () -> Unit) {
    val selected = item.state.isSelected
    Surface(
        onClick = onClick,
        enabled = item.state.isEnabled,
        shape = RoundedCornerShape(percent = 50),
        color = if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FabItemIcon(item.icon)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item.contentDescription, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Renders a [ToolbarIcon] with the ambient content color (baked tints ignored — strictly M3). */
@Composable
private fun FabItemIcon(icon: ToolbarIcon) {
    when (icon) {
        is ToolbarIcon.Resource -> Icon(
            painter = painterResource(icon.id),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )

        is ToolbarIcon.Vector -> Icon(
            imageVector = icon.image,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )

        is ToolbarIcon.Composed -> icon.content(LocalContentColor.current)

        is ToolbarIcon.None -> Icon(
            painter = painterResource(com.pspdfkit.R.drawable.pspdf__ic_more),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * While the coordinator's inline-search session is active, the FAB menu hides and this full-width
 * search pill springs in at the top edge (same contract as NutrientFloatingToolbar).
 */
@Composable
private fun InlineSearchTakeover(coordinator: ToolbarCoordinator, modifier: Modifier = Modifier) {
    val searchSession by coordinator.searchSession.collectAsState()
    val searchActive = searchSession?.isActive?.collectAsState()?.value ?: false

    AnimatedVisibility(
        visible = searchActive,
        enter = slideInVertically(expressiveSpatial()) { -2 * it } + fadeIn(expressiveEffects()),
        exit = slideOutVertically(expressiveSpatial()) { -2 * it } + fadeOut(expressiveEffects()),
        modifier = modifier.safeDrawingPadding(),
    ) {
        searchSession?.let { session ->
            val searchState by session.state.collectAsState()
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 3.dp,
                modifier = Modifier.padding(16.dp),
            ) {
                NutrientInlineSearchBar(
                    state = searchState,
                    actions = session.actions,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                )
            }
        }
    }
}

/** Sentinel for "no active item yet" — toolbar item ids are `R.id` values, never negative. */
private const val NO_ACTIVE_ITEM = -1

private val FabSize = 56.dp
private val FabEdgeMargin = 16.dp
private val MenuSpacing = 16.dp

/** M3 expressive motion tokens — default spatial spring (position/size/scale, visible bounce). */
private fun <T> expressiveSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

/** M3 expressive motion tokens — default effects spring (fades, critically damped). */
private fun <T> expressiveEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
