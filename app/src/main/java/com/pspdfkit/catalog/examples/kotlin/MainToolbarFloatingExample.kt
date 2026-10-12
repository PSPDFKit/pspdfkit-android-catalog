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
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.enableConfigurationEdgeToEdge
import com.pspdfkit.compose.theme.NutrientTheme
import com.pspdfkit.compose.toolbar.composables.FloatingToolbarEdge
import com.pspdfkit.compose.toolbar.composables.NutrientFloatingToolbar
import com.pspdfkit.compose.toolbar.composables.rememberFloatingToolbarState
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.search.SearchType
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * Renders the main-toolbar model as a **Material 3 Expressive floating toolbar** — a draggable
 * pill built with [NutrientFloatingToolbar] on top of the document's
 * [com.pspdfkit.compose.toolbar.adapter.ToolbarCoordinator]:
 *
 *  - Drag the pill by its handle and release: it springs to the nearest window edge, flipping
 *    between horizontal (top/bottom) and vertical (start/end) layouts.
 *  - All motion (show/hide, orientation flip, drag release, press feedback) runs on the Material 3
 *    expressive motion scheme's spring tokens, built into [NutrientFloatingToolbar].
 *  - Items and clicks flow through the same coordinator APIs as the regular main toolbar.
 *  - The **Collapsible** toggle — a labeled Material 3 [Switch] — switches the pill into
 *    its collapsible variant: tapping any item in the expanded menu runs it and collapses the pill
 *    down to just that item — the *active* item stays visible as a single floating button, and
 *    tapping it springs the menu back open (nothing is dispatched). The pill starts docked at the
 *    trailing edge, showing off the vertical layout right away. The toggle rides the top edge
 *    whenever that edge is free — with the pill docked at the bottom or vertically at either
 *    side — and drops to the bottom, clear of the floating thumbnail bar, once the pill or the
 *    inline-search pill takes the top edge. It animates away entirely while a contextual toolbar
 *    (annotation creation, content editing, …) is up.
 */
class MainToolbarFloatingExample(context: Context) :
    SdkExample(context, R.string.mainToolbarFloatingExampleTitle, R.string.mainToolbarFloatingExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, MainToolbarFloatingActivity::class.java)
            intent.putExtra(MainToolbarFloatingActivity.EXTRA_URI, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}

class MainToolbarFloatingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!
        val configuration = PdfActivityConfiguration.Builder(this)
            // The floating toolbar replaces the SDK's edge-to-edge top toolbar entirely.
            .defaultToolbarEnabled(false)
            .documentTitleOverlayEnabled(false)
            // Inline search renders as an expressive search pill at the top edge while active.
            .setSearchType(SearchType.INLINE)
            .build()

        enableConfigurationEdgeToEdge(configuration)
        setContent {
            // The floating toolbar reads its container/button colors from the ambient Material 3
            // color scheme; its expressive spring motion is built in.
            MaterialTheme(colorScheme = expressiveColorScheme()) {
                FloatingToolbarDocument(uri, configuration)
            }
        }
    }

    companion object {
        const val EXTRA_URI = "MainToolbarFloatingActivity.DocumentUri"
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

/**
 * Vertical room the toggle leaves above the bottom window edge so it clears the SDK's floating
 * thumbnail bar: the bar's own height (`pspdf__thumbnail_height`, 28dp, inside
 * `pspdf__thumbnail_bar_content_padding` of 6dp top and bottom), plus the 36dp
 * `pspdf__floating_thumbnail_bar_margin_bottom` it keeps from the window edge, plus an 8dp gap so
 * the two pieces of chrome don't touch.
 */
private val THUMBNAIL_BAR_CLEARANCE = 28.dp + (6.dp * 2) + 36.dp + 8.dp

@Composable
private fun FloatingToolbarDocument(uri: Uri, configuration: PdfActivityConfiguration) {
    val documentState = rememberDocumentState(uri, configuration)

    // The DocumentView publishes its fragment-scoped toolbar coordinator here once the UI is ready.
    val coordinator by documentState.toolbarCoordinator.collectAsState()
    var collapsible by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        DocumentView(
            documentState = documentState,
            modifier = Modifier.fillMaxSize(),
        )

        coordinator?.let { c ->
            NutrientTheme {
                // Hoisted out of the `key` below on purpose: the pill's docked edge survives the
                // variant flip (a drag to the bottom edge stays put when the user toggles), and
                // the toggle's own placement reads `edge` to stay out from under the pill. It
                // starts docked at the trailing edge, showing the vertical layout right away.
                val toolbarState = rememberFloatingToolbarState(initialEdge = FloatingToolbarEdge.End)
                // Re-seed the pill's *internal* state (expanded/collapsed, active item) when the
                // variant flips, so each variant starts from a clean pill.
                key(collapsible) {
                    NutrientFloatingToolbar(
                        coordinator = c,
                        state = toolbarState,
                        vibrant = true,
                        draggable = true,
                        collapsible = collapsible,
                    )
                }

                // Deliberately outside the `key`: the toggle drives `collapsible`, so re-keying it
                // on its own tap would rebuild the Switch mid-gesture and snap the thumb instead
                // of animating it.
                //
                // Keep the toggle out from under the toolbar. The pill docks centered on an
                // edge, so the toggle rides the top edge for every dock except the top one:
                // docked at the bottom or vertically at either side, the top edge is free.
                // The top edge is taken while the pill docks there, and also while inline
                // search is active — the pill hides and a full-width search pill takes its
                // place — so the toggle drops to the bottom, lifted clear of the floating
                // thumbnail bar that lives there.
                val searchActive = c.searchSession.collectAsState().value?.isActive?.collectAsState()?.value == true
                val topEdgeTaken = searchActive || toolbarState.edge == FloatingToolbarEdge.Top

                // A contextual toolbar (annotation creation, content editing, …) owns the
                // screen while it is up, so the toggle springs away until it is dismissed.
                val contextualToolbar by c.contextualToolbar.collectAsState()
                // The pill itself honors `frame.isVisible`, so the toggle that drives it has to
                // as well: tapping the document into immersive mode takes the pill away, and a
                // switch for a toolbar that is not on screen would be left floating alone.
                val frame by c.frame.collectAsState()
                AnimatedVisibility(
                    visible = contextualToolbar == null && frame.isVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(if (topEdgeTaken) Alignment.BottomCenter else Alignment.TopCenter)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(
                            top = 8.dp,
                            bottom = if (topEdgeTaken) THUMBNAIL_BAR_CLEARANCE else 8.dp,
                        ),
                ) {
                    Surface(
                        onClick = { collapsible = !collapsible },
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.mainToolbarFloatingCollapsibleToggle),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Switch(checked = collapsible, onCheckedChange = { collapsible = it })
                        }
                    }
                }
            }
        }
    }
}
