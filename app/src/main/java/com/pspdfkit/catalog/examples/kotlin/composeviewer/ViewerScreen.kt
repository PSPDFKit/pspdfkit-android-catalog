/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.composeviewer

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.net.toUri
import com.pspdfkit.catalog.R
import com.pspdfkit.compose.theme.MainToolbarColors
import com.pspdfkit.compose.theme.NutrientTheme
import com.pspdfkit.compose.theme.ToolbarPopupColors
import com.pspdfkit.compose.theme.UiTheme
import com.pspdfkit.compose.toolbar.composables.NutrientMainToolbar
import com.pspdfkit.compose.toolbar.state.ToolbarIcon
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.theming.ThemeMode
import com.pspdfkit.document.providers.ContentResolverDataProvider
import com.pspdfkit.document.providers.DataProvider
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import kotlinx.collections.immutable.toPersistentList
import java.io.File

/**
 * The viewer: a [DocumentView] with a **host-rendered** [NutrientMainToolbar] and the inset
 * bookkeeping that requires, plus a live demonstration of [DocumentState.setCustomPdfSource].
 *
 * ### The `setCustomPdfSource` contract (demonstrated by the reload toolbar item)
 *
 * `setCustomPdfSource` swaps (or with `null`, reloads) the displayed document. Three things to
 * know:
 *
 * 1. **The swap is asynchronous.** The call triggers a fragment-level document reload and
 *    returns immediately — there is no return value or callback on the method itself. Completion
 *    is observed through the document listener: `DefaultListeners.documentListeners`'s
 *    `onDocumentLoaded` fires once the new document is displayed.
 * 2. **The zoom resets to 1.0 on every reload**, and the page index carries over as-is (with
 *    `restoreLastViewedPage` enabled, the incoming document's last-viewed page can take over).
 *    A caller that wants continuity captures [DocumentState.getViewState] *before* the swap
 *    and applies [DocumentState.setViewState] from `onDocumentLoaded`, exactly as the reload
 *    item below does.
 * 3. **`null` reloads the current source.** With no document loaded the call is a silent no-op
 *    (failures are caught and logged by the SDK). When swapping to a *different* document,
 *    guard the carried-over page index first (`documentConnection.setPageIndex(0)`) — a
 *    shorter document can crash on an out-of-range index; see `CustomPdfSourceExample`.
 */
@Composable
internal fun ViewerScreen(path: String, isDark: Boolean, onBack: () -> Unit) {
    val configuration = PdfActivityConfiguration
        .Builder(LocalContext.current)
        .defaultToolbarEnabled(false)
        .fitMode(PageFitMode.FIT_TO_WIDTH)
        .setUserInterfaceViewMode(UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_AUTOMATIC)
        // The app's persisted theme choice drives the document theme too.
        .themeMode(if (isDark) ThemeMode.NIGHT else ThemeMode.DEFAULT)
        .build()

    val dataProvider: DataProvider by remember(path) { mutableStateOf(ContentResolverDataProvider(File(path).toUri())) }
    val documentState = rememberDocumentState(dataProvider, configuration)

    // The main-toolbar coordinator is scoped to this document's fragment, so it is null until the UI
    // is initialized and is independent for each DocumentView shown in the activity.
    val toolbarCoordinator by documentState.toolbarCoordinator.collectAsState()

    // The setCustomPdfSource demonstration: the view state captured before the reload, applied
    // once the swapped document finishes loading (see the class KDoc for the full contract).
    var pendingViewStateRestore by remember { mutableStateOf<Bundle?>(null) }
    var isReloading by remember { mutableStateOf(false) }

    // Custom toolbar items and click routing through the public DocumentState API — applied by
    // the SDK once the DocumentView attaches, so it is safe to set them up front.
    LaunchedEffect(documentState) {
        documentState.setToolbarItemsProvider { defaults ->
            // The SDK bakes the XML theme's icon tint into every default item, and a per-item tint
            // wins over the color scheme at render time — so without this the icons would keep the
            // XML light-blue while the toolbar behind them follows the Compose theme below. Clearing
            // the tint makes the renderer fall back to MainToolbarColors.iconColor, so the icons
            // track the theme (including the dark and dynamic-color variants).
            defaults.map { it.withThemedIconTint() } + ToolbarItem.Button(
                actionId = "composeviewer.reload",
                id = R.id.custom_action1,
                contentDescription = "Reload document",
                icon = ToolbarIcon.Vector(Icons.Outlined.Refresh),
                overflowBehavior = ToolbarItem.OverflowBehavior.Never,
            )
        }
        documentState.setOnToolbarItemClickListener { id ->
            if (id == R.id.custom_action1 && !isReloading) {
                // 1. Capture the view state BEFORE the swap — the reload resets the zoom.
                pendingViewStateRestore = documentState.getViewState()
                isReloading = true
                // 2. Trigger the asynchronous reload. Passing null re-opens the current source;
                //    a different DocumentSource would swap the displayed document the same way.
                documentState.setCustomPdfSource(null)
                // 3. Completion arrives in onDocumentLoaded below — nothing to wait on here.
                true
            } else {
                // Everything else keeps the SDK's default handling.
                false
            }
        }
    }

    val toolbarColors = viewerToolbarColors()

    // True while an overlay view (thumbnail grid / outline / document info) is shown. Those views
    // are siblings of the document inside the SDK's own layout, so the host has to tell the SDK
    // how far down to push their content — nothing else knows this toolbar is there.
    val overlayViewShown by documentState.viewWithOverlappingToolbarShown.collectAsState()
    var toolbarHeightPx by remember { mutableIntStateOf(0) }
    // `PdfThumbnailGrid.fitSystemWindows` pads the grid by the window insets itself, so the
    // status-bar strip this toolbar spans is already accounted for downstream — passing the
    // toolbar's full height would count it twice.
    val statusBarPx = WindowInsets.statusBars.getTop(LocalDensity.current)
    LaunchedEffect(overlayViewShown, toolbarHeightPx, statusBarPx) {
        val padding = (toolbarHeightPx - statusBarPx).coerceAtLeast(0)
        documentState.setContentViewTopPadding(if (overlayViewShown) padding else 0)
    }

    Box(
        modifier = Modifier
            .background(Color.DarkGray)
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        DocumentView(
            documentState,
            modifier = Modifier.fillMaxSize(),
            documentManager = getDefaultDocumentManager(
                documentListener = DefaultListeners.documentListeners(onDocumentLoaded = {
                    // setCustomPdfSource completion: the swapped document is displayed. Restore
                    // the captured view state so the reader keeps their page and zoom.
                    if (isReloading) {
                        pendingViewStateRestore?.let { documentState.setViewState(it) }
                        pendingViewStateRestore = null
                        isReloading = false
                    }
                }),
            ),
        )

        NutrientTheme {
            toolbarCoordinator?.let { coordinator ->
                NutrientMainToolbar(
                    coordinator = coordinator,
                    // The toolbar overlays a full-bleed DocumentView, so the SDK's overlay views
                    // (thumbnail grid, outline) would render underneath it. Measure what this
                    // toolbar actually occupies — status-bar strip, row, and title overlay — and
                    // hand it to setContentViewTopPadding below; the SDK's own
                    // getContextualToolbarSizePx() is a single row and would under-pad here.
                    modifier = Modifier.fillMaxWidth().onSizeChanged { toolbarHeightPx = it.height },
                    // A back button in place of the document title; the items stay the SDK's. The
                    // icon tints itself off the toolbar's colors, which the bar provides to its slots.
                    navigationIcon = {
                        IconButton(onClick = {
                            if (documentState.isDefaultViewerActive()) onBack() else documentState.exitCurrentState()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = UiTheme.colors.mainToolbar.iconColor,
                            )
                        }
                    },
                    title = {},
                    // The host owns this toolbar's insets: pin them explicitly instead of
                    // relying on the shared frame defaults, so the toolbar keeps its status-bar
                    // padding no matter what the SDK computes for its own (unused) mount point.
                    windowInsets = WindowInsets.statusBars,
                    colors = toolbarColors,
                )
            }
        }

        if (isReloading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
    // Back unwinds like PdfActivity: exit the active state (search, annotation mode, thumbnail
    // grid) first, then leave the viewer.
    BackHandler(true) {
        if (documentState.isDefaultViewerActive()) onBack() else documentState.exitCurrentState()
    }
}

/**
 * Drops a default item's baked-in icon tint so the icon follows the ambient [NutrientTheme] color
 * scheme instead. `ToolbarIcon`'s `tint = null` means "use the toolbar's default icon tint".
 */
private fun ToolbarItem.withThemedIconTint(): ToolbarItem {
    val untinted = when (val current = icon) {
        is ToolbarIcon.Resource -> current.copy(tint = null)

        is ToolbarIcon.Vector -> current.copy(tint = null)

        // Composed icons are handed the resolved tint already, and None has nothing to tint.
        else -> return this
    }
    return when (this) {
        is ToolbarItem.Button -> copy(icon = untinted)
        is ToolbarItem.Toggle -> copy(icon = untinted)
        is ToolbarItem.Submenu -> copy(icon = untinted, children = children.map { it.withThemedIconTint() }.toPersistentList())
    }
}

/** The main toolbar's colors, derived from the ambient (dynamic-color aware) Material theme. */
@Composable
private fun viewerToolbarColors(): MainToolbarColors {
    val background = MaterialTheme.colorScheme.primaryContainer
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    return MainToolbarColors(
        backgroundColor = background,
        textColor = content,
        popup = ToolbarPopupColors(backgroundColor = background),
        titleTextColor = content,
    )
}
