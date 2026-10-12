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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * Replacing and refreshing the displayed document **without recreating the composable**, through
 * [DocumentState.setCustomPdfSource] on a hoisted [DocumentState]. The contract:
 *
 * - **When it is safe to call:** any time after the [DocumentView] has composed and attached.
 *   Calling it before that is a silent no-op (no exception, no log).
 * - **The call is asynchronous.** Completion is observed through the `DocumentManager`'s
 *   `onDocumentLoaded` callback — it fires again for the new document once loading completes,
 *   the same way it does for the initial document.
 * - **Preserved:** the underlying fragment, the [PdfActivityConfiguration], and every listener
 *   registered through the `DocumentManager`.
 * - **Reset:** the zoom resets to 1.0 on every reload. The current page index carries over into
 *   the new document as-is; with `restoreLastViewedPage` enabled on the configuration (the
 *   default), the incoming document's own last-viewed page can then take over once loading
 *   completes.
 * - **Guard the page index before replacing:** because the index carries over unguarded,
 *   swapping to a document with fewer pages than the current index can crash. Set a valid page
 *   first — `documentState.documentConnection.setPageIndex(0)` — before calling
 *   [DocumentState.setCustomPdfSource] with a different, potentially shorter document.
 * - **Replace vs. refresh:** pass a [DocumentSource] to swap the displayed document; pass
 *   `null` to reload the current source (e.g. after its backing file changed on disk).
 * - **Keeping the reader's place:** capture [DocumentState.getViewState] before the call and
 *   apply [DocumentState.setViewState] from `onDocumentLoaded`. This example keeps one bundle
 *   per document, so switching back restores that document's page, zoom, and viewport — a
 *   bundle is only ever restored onto the same document it was captured from.
 */
class CustomPdfSourceExample(context: Context) :
    SdkExample(context, R.string.customPdfSourceExampleTitle, R.string.customPdfSourceExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Extract both documents up front so the swap button has a target ready.
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { firstDocument ->
            ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, title, context) { secondDocument ->
                val intent = Intent(context, CustomPdfSourceActivity::class.java)
                    .putExtra(CustomPdfSourceActivity.EXTRA_FIRST, Uri.fromFile(firstDocument))
                    .putExtra(CustomPdfSourceActivity.EXTRA_SECOND, Uri.fromFile(secondDocument))
                    // The catalog-prepared configuration carries the catalog theme and the
                    // user's preference toggles — pass it through instead of rebuilding.
                    .putExtra(CustomPdfSourceActivity.EXTRA_CONFIG, configuration.build())
                context.startActivity(intent)
            }
        }
    }
}

class CustomPdfSourceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val first = intent.getSupportParcelableExtra(EXTRA_FIRST, Uri::class.java)!!
        val second = intent.getSupportParcelableExtra(EXTRA_SECOND, Uri::class.java)!!
        val configuration = intent.getSupportParcelableExtra(EXTRA_CONFIG, PdfActivityConfiguration::class.java)

        setContent {
            CatalogTheme {
                CustomPdfSourceDocument(first, second, configuration)
            }
        }
    }

    companion object {
        const val EXTRA_FIRST = "CustomPdfSourceActivity.FirstDocument"
        const val EXTRA_SECOND = "CustomPdfSourceActivity.SecondDocument"
        const val EXTRA_CONFIG = "CustomPdfSourceActivity.Configuration"
    }
}

@Composable
private fun CustomPdfSourceDocument(first: Uri, second: Uri, baseConfiguration: PdfActivityConfiguration?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = remember(baseConfiguration) {
        // Layer the example's needs on top of the catalog-prepared configuration (theme,
        // preference toggles) instead of discarding it.
        (baseConfiguration?.let { PdfActivityConfiguration.Builder(it) } ?: PdfActivityConfiguration.Builder(context))
            // The buttons below occupy the strip where the document title overlay would render.
            .documentTitleOverlayEnabled(false)
            .build()
    }
    // The state is hoisted once with the FIRST document; swapping never recreates it — the
    // fragment, configuration, and DocumentManager listeners all survive the source change.
    // File-backed documents open through their Uri (native, memory-mapped); a stream-backed
    // DataProvider would buffer the whole document onto the Java heap.
    val documentState = rememberDocumentState(first, configuration)

    // Which source is currently requested, and whether a swap/refresh is still loading.
    var showingSecond by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadedDocumentTitle by remember { mutableStateOf<String?>(null) }

    // Continuity across the swap: each document's view state (page, zoom, viewport) is captured
    // when leaving it and re-applied from onDocumentLoaded when returning to it — a bundle is
    // only ever restored onto the SAME document it was captured from. A refresh restores the
    // state it captured right before the call, so the reader keeps their place.
    val savedViewStates = remember { mutableMapOf<Boolean, Bundle?>() }
    var pendingViewStateRestore by remember { mutableStateOf<Bundle?>(null) }

    // The buttons follow the SDK chrome: hidden while the UI is hidden (immersive mode) and
    // while any special mode is active (annotation / document / content editing — signaled by a
    // non-null contextual toolbar), so they never sit under the top-docked contextual bar or
    // distract from an active tool.
    var uiVisible by remember { mutableStateOf(true) }
    val coordinator by documentState.toolbarCoordinator.collectAsState()
    val modeActive = coordinator?.contextualToolbar?.collectAsState()?.value != null

    Box(Modifier.fillMaxSize()) {
        DocumentView(
            documentState = documentState,
            modifier = Modifier.fillMaxSize(),
            documentManager = getDefaultDocumentManager(
                documentListener = DefaultListeners.documentListeners(
                    // The completion signal for setCustomPdfSource: fires again for the new
                    // document once the asynchronous replacement/refresh finishes.
                    onDocumentLoaded = { document ->
                        isLoading = false
                        loadedDocumentTitle = document.title
                        // Restore the page, zoom, and viewport captured for THIS document —
                        // applied after load so it also wins over restoreLastViewedPage.
                        pendingViewStateRestore?.let { documentState.setViewState(it) }
                        pendingViewStateRestore = null
                    },
                ),
                uiListener = DefaultListeners.uiListeners(
                    onImmersiveModeEnabled = { immersive -> uiVisible = !immersive },
                ),
            ),
        )

        // Anchored below the SDK main toolbar (status-bar strip + the standard 56dp toolbar
        // row) — the bottom edge belongs to the thumbnail bar. Fixed clearance on purpose:
        // measuring through the fragment would read 0 before it attaches.
        AnimatedVisibility(
            visible = uiVisible && !modeActive,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 56.dp + 12.dp),
        ) {
            SourceButtons(
                showingSecond = showingSecond,
                isLoading = isLoading,
                onSwap = {
                    // Capture the leaving document's place FIRST (before the page-index guard
                    // below clobbers it), so switching back restores page, zoom, and viewport.
                    savedViewStates[showingSecond] = documentState.getViewState()
                    // Guard the page index BEFORE replacing: it carries over into the new
                    // document unguarded, and a shorter document can crash on an out-of-range
                    // index. Page 0 is always valid.
                    documentState.documentConnection.setPageIndex(0)
                    isLoading = true
                    showingSecond = !showingSecond
                    // The incoming document's own saved place, if it was visited before.
                    pendingViewStateRestore = savedViewStates[showingSecond]
                    val target = if (showingSecond) second else first
                    // Replace: asynchronous — completion arrives in onDocumentLoaded above.
                    documentState.setCustomPdfSource(DocumentSource(target))
                },
                onRefresh = {
                    // Same-document reload: capture now, restore from onDocumentLoaded — the
                    // reload alone would reset the zoom to 1.0.
                    pendingViewStateRestore = documentState.getViewState()
                    isLoading = true
                    // Refresh: null reloads the CURRENT source (same completion signal).
                    documentState.setCustomPdfSource(null)
                },
            )
        }

        if (isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
    // Back unwinds like PdfActivity: while a mode or an overlay view is active, let the SDK
    // close it the layered way (contextual toolbar first, then the overlay); only the default
    // viewer state lets the system back finish the example. Both gate signals are observable
    // flows, so the handler's enabled state never goes stale.
    val overlayShown by documentState.viewWithOverlappingToolbarShown.collectAsState()
    BackHandler(enabled = modeActive || overlayShown) {
        documentState.handleBackPress()
    }
}

@Composable
private fun SourceButtons(showingSecond: Boolean, isLoading: Boolean, onSwap: () -> Unit, onRefresh: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(enabled = !isLoading, onClick = onSwap) {
            Text(if (showingSecond) "Show first document" else "Show second document")
        }
        OutlinedButton(enabled = !isLoading, onClick = onRefresh) {
            Text("Refresh")
        }
    }
}
