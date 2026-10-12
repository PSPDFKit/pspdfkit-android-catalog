/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The Nutrient sample applications are licensed with a modified BSD license.
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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.catalog.ui.theming.enableConfigurationEdgeToEdge
import com.pspdfkit.catalog.ui.theming.isNightTheme
import com.pspdfkit.compose.tabs.composables.NutrientTabBar
import com.pspdfkit.compose.tabs.composables.NutrientTabBarDefaults
import com.pspdfkit.compose.theme.MainToolbarColors
import com.pspdfkit.compose.theme.NutrientTheme
import com.pspdfkit.compose.theme.ToolbarPopupColors
import com.pspdfkit.compose.theme.UiColorScheme
import com.pspdfkit.compose.theme.getUiColors
import com.pspdfkit.compose.toolbar.composables.NutrientMainToolbar
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.TabBarHidingMode
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.ui.DocumentCoordinator
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.tabs.PdfTabBarCloseMode
import com.pspdfkit.utils.getSupportParcelableExtra
import io.nutrient.ui.theme.getTabBarColors

/**
 * Shows several documents in one [DocumentView] under app-owned chrome that floats over the document: a
 * [NutrientMainToolbar] rendered as a pill, and below it a [NutrientTabBar] with rounded, spaced tabs in the app's
 * Material colors plus an add button that opens another document in a new tab. Because the chrome overlays the
 * document rather than sitting above it, hiding it in immersive mode leaves the page where it is.
 *
 * Both bars bind to coordinators the [com.pspdfkit.jetpack.compose.interactors.DocumentState] publishes once the
 * document UI is ready: the toolbar coordinator and the [DocumentCoordinator]. The SDK's own main toolbar and tab
 * bar are disabled through the configuration.
 */
class DocumentTabsComposeExample(context: Context) :
    SdkExample(context, R.string.documentTabsComposeExampleTitle, R.string.documentTabsComposeExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, DocumentTabsComposeActivity::class.java)
            intent.putExtra(DocumentTabsComposeActivity.EXTRA_URI, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}

class DocumentTabsComposeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!
        val configuration = PdfActivityConfiguration.Builder(this)
            // The activity renders its own main toolbar and tab bar above the document, so the SDK's stay hidden.
            .defaultToolbarEnabled(false)
            .setTabBarHidingMode(TabBarHidingMode.HIDE)
            // Keep the pill to a single row: no document title overlay strip below the toolbar items.
            .documentTitleOverlayEnabled(false)
            .build()

        enableConfigurationEdgeToEdge(configuration)
        setContent {
            CatalogTheme(darkTheme = configuration.isNightTheme) {
                TabbedDocuments(uri, configuration)
            }
        }
    }

    companion object {
        const val EXTRA_URI = "DocumentTabsComposeActivity.DocumentUri"
    }
}

@Composable
private fun TabbedDocuments(uri: Uri, configuration: PdfActivityConfiguration) {
    val context = LocalContext.current
    val documentState = rememberDocumentState(uri, configuration)

    // The DocumentView publishes both coordinators once its UI is ready. They are null until then, so the bars are
    // rendered only once they are non-null.
    val toolbarCoordinator by documentState.toolbarCoordinator.collectAsState()
    val documentCoordinator by documentState.documentCoordinator.collectAsState()

    // Let the SDK handle back the layered way: close an overlay view (outline, page grid, ...) first.
    val overlayViewShown by documentState.viewWithOverlappingToolbarShown.collectAsState()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = overlayViewShown && !imeVisible) {
        documentState.handleBackPress()
    }

    // Cycles through the extra documents the add button opens.
    var nextDocument by rememberSaveable { mutableIntStateOf(0) }

    // The toolbar frame's visibility flips with immersive mode (tap-to-hide chrome). The SDK's own toolbar animates
    // on it; here the whole chrome, pill and tab bar, follows it so that both leave together.
    val chromeVisible = toolbarCoordinator?.let { it.frame.collectAsState().value.isVisible } ?: true

    // Overlay views (outline, page grid, ...) are laid out by the SDK inside the DocumentView, which the chrome
    // covers. Push their content below the chrome while one is shown; the document itself stays put.
    var chromeHeightPx by remember { mutableStateOf(0) }
    LaunchedEffect(overlayViewShown, chromeHeightPx) {
        documentState.setContentViewTopPadding(if (overlayViewShown) chromeHeightPx else 0)
    }

    val chromeColor = MaterialTheme.colorScheme.surfaceContainer
    Box(Modifier.fillMaxSize()) {
        // The document fills the screen and sits behind the chrome, so showing or hiding the chrome never moves it.
        DocumentView(
            documentState = documentState,
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(
            visible = chromeVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).background(Color.Transparent),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .statusBarsPadding()
                    .onSizeChanged { chromeHeightPx = it.height },
            ) {
                toolbarCoordinator?.let { coordinator ->
                    // The floating, rounded, elevated main toolbar. NutrientTheme provides the pill's color scheme
                    // to the toolbar items, the title and the overflow popup.
                    NutrientTheme(colorScheme = pillToolbarColors()) {
                        Surface(
                            shape = RoundedCornerShape(PILL_CORNER_RADIUS),
                            shadowElevation = 8.dp,
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(PILL_CORNER_RADIUS)),
                        ) {
                            NutrientMainToolbar(
                                coordinator = coordinator,
                                // The chrome already offsets the status bar, so the toolbar must not add its own inset.
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                // The chrome animates as a whole, so the toolbar must not animate on its own too.
                                enter = EnterTransition.None,
                                exit = ExitTransition.None,
                            )
                        }
                    }
                }
                documentCoordinator?.let { coordinator ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NutrientTabBar(
                            documentCoordinator = coordinator,
                            modifier = Modifier.weight(1f).padding(top = 5.dp),
                            closeMode = PdfTabBarCloseMode.CLOSE_ONLY_SELECTED_TAB,
                            colors = getTabBarColors(
                                backgroundColor = Color.Transparent,
                                tabColor = MaterialTheme.colorScheme.surface,
                                selectedTabColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            dimensions = NutrientTabBarDefaults.dimensions(height = 52.dp, tabMinWidth = 96.dp),
                            tabShape = RoundedCornerShape(PILL_CORNER_RADIUS),
                            tabSpacing = 8.dp,
                            contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 8.dp),
                        )
                        SmallFloatingActionButton({
                            val assetName = MORE_DOCUMENTS[nextDocument % MORE_DOCUMENTS.size]
                            nextDocument++
                            ExtractAssetTask.extract(assetName, assetName, context) { documentFile ->
                                openInNewTab(coordinator, Uri.fromFile(documentFile))
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.documentTabsComposeExampleAddTab))
                        }
                    }
                }
            }
        }
    }
}

/** The pill's color scheme: the app's primary color with white content, for items, title and the overflow popup. */
@Composable
private fun pillToolbarColors(): UiColorScheme {
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

/** Adds the document at [uri] to [coordinator] and switches to it, which selects its tab. */
private fun openInNewTab(coordinator: DocumentCoordinator, uri: Uri) {
    val descriptor = DocumentDescriptor.fromUri(uri)
    coordinator.addDocument(descriptor)
    coordinator.setVisibleDocument(descriptor)
}

private val MORE_DOCUMENTS = listOf("Classbook.pdf", "Aviation.pdf", "Annotations.pdf")
private val PILL_CORNER_RADIUS = 28.dp
