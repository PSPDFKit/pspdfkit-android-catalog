/*
 *   Copyright © 2020-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.compose.theme.MainToolbarColors
import com.pspdfkit.compose.theme.ToolbarPopupColors
import com.pspdfkit.compose.theme.getUiColors
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.jetpack.compose.components.MainToolbar
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.DocumentState
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

/**
 * List of PDF files to be displayed in the example
 */
private val FILES = listOf(
    WELCOME_DOC.removeSuffix(".pdf"),
    "Scientific-paper",
    "The-Cosmic-Context-for-Life"
)

/**
 * Example entry point showing how to display multiple PDF documents with individual configurations
 */
class DocumentPagerExample(context: Context) :
    SdkExample(context, R.string.documentPagerExampleTitle, R.string.documentPagerExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Extract the documents from assets before launching the activity
        FileUtils.copyFilesFromAssetsToLocalStorage(context, FILES) {
            val intent = Intent(context, DocumentPagerExampleActivity::class.java)
            context.startActivity(intent)
        }
    }
}

/**
 * Activity hosting the multiple PDF viewer Composable
 */
class DocumentPagerExampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CatalogTheme {
                PdfViewerPager(
                    files = FILES,
                    getFileUri = { filename ->
                        File(filesDir, "$filename.pdf").toUri()
                    }
                )
            }
        }
    }
}

/**
 * Manages DocumentState instances for multiple PDFs using WeakReferences to prevent memory leaks
 */
class DocumentStateManager(
    private val files: List<String>,
    private val getFileUri: (String) -> Uri
) {
    private val documentStates = mutableMapOf<Int, WeakReference<DocumentState>>()

    /**
     * Returns an existing DocumentState or creates a new one if needed
     *
     * @param index The index of the PDF in the files list
     * @return A DocumentState for the PDF at the given index
     */
    @Composable
    fun getOrCreateDocumentState(index: Int): DocumentState {
        val weakRef = documentStates[index]
        val existingState = weakRef?.get()

        // Return existing state if available
        if (existingState != null) {
            return existingState
        }

        // Create a new state with different configuration based on document index
        val context = LocalContext.current
        val commonConfig = PdfActivityConfiguration.Builder(context)
            .defaultToolbarEnabled(false).fitMode(PageFitMode.FIT_TO_WIDTH)
        val uri = getFileUri(files[index])
        val configuration = createConfigurationForIndex(index, commonConfig)
        val newState = rememberDocumentState(uri, configuration)

        // Store weak reference and return the new state
        documentStates[index] = WeakReference(newState)
        return newState
    }

    /**
     * Creates a specific configuration based on the document index
     */
    private fun createConfigurationForIndex(index: Int, commonConfig: PdfActivityConfiguration.Builder) = when (index) {
        0 -> commonConfig.scrollMode(PageScrollMode.PER_PAGE).build()
        1 -> commonConfig.scrollDirection(PageScrollDirection.VERTICAL).build()
        else -> commonConfig.layoutMode(PageLayoutMode.DOUBLE).build()
    }
}

/**
 * Main Composable that displays multiple PDFs with tabs for navigation
 *
 * @param files List of PDF filenames to display
 * @param getFileUri Function to convert a filename to a URI
 */
@Composable
fun PdfViewerPager(
    files: List<String>,
    getFileUri: (String) -> Uri
) {
    val localDensity = LocalDensity.current
    val documentStateManager = remember(files) {
        DocumentStateManager(files, getFileUri)
    }

    val pagerState = rememberPagerState(pageCount = { files.size })
    val coroutineScope = rememberCoroutineScope()
    var hideTopBar by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = hideTopBar,
                    enter = slideInVertically { with(localDensity) { -40.dp.roundToPx() } } +
                        expandVertically(expandFrom = Alignment.Top) +
                        fadeIn(initialAlpha = 0.3f),
                    exit = slideOutVertically() + shrinkVertically() + fadeOut()
                ) {
                    // Tab row for document selection
                    PdfTabRow(
                        files = files,
                        currentPage = pagerState.currentPage,
                        onTabSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            // Horizontal pager for PDF documents
            HorizontalPager(
                userScrollEnabled = false,
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                key(page) {
                    PdfDocumentPage(
                        page = page,
                        documentStateManager = documentStateManager,
                        paddingValues = paddingValues,
                        localDensity = localDensity
                    ) {
                        hideTopBar = it
                    }
                }
            }
        }
    }
}

/**
 * Composable for the tab row to switch between documents
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfTabRow(
    files: List<String>,
    currentPage: Int,
    onTabSelected: (Int) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = currentPage,
        modifier = Modifier
            .fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        files.forEachIndexed { index, title ->
            Tab(
                selected = currentPage == index,
                onClick = { onTabSelected(index) },
                text = {
                    Column(
                        Modifier
                            .padding(4.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            )
        }
    }
}

/**
 * Composable for an individual PDF document page
 */
@Composable
private fun PdfDocumentPage(
    page: Int,
    documentStateManager: DocumentStateManager,
    paddingValues: PaddingValues,
    localDensity: Density,
    updateTopBarVisibility: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val documentState = documentStateManager.getOrCreateDocumentState(page)

    // State for toolbar visibility and height
    var toolbarVisibility by remember { mutableStateOf(true) }
    var toolbarHeight by remember { mutableStateOf(0.dp) }

    // Calculate spacer height based on view overlap state
    val enableViewSpacer by documentState.viewWithOverlappingToolbarShown.collectAsState()
    val viewSpacerHeight by remember {
        derivedStateOf { if (enableViewSpacer && toolbarVisibility) toolbarHeight else 0.dp }
    }

    Box(
        contentAlignment = Alignment.TopCenter
    ) {
        Column {
            // Spacer to prevent content from being covered by toolbar
            Box(modifier = Modifier.height(viewSpacerHeight).padding(top = paddingValues.calculateTopPadding()))

            // PDF Document View
            DocumentView(
                documentState = documentState,
                modifier = Modifier.weight(1f),
                documentManager = createDocumentManager(context) {
                    toolbarVisibility = !it
                    updateTopBarVisibility.invoke(!it)
                }
            )
        }

        // Animated toolbar
        AnimatedVisibility(
            visible = toolbarVisibility,
            enter = slideInVertically { with(localDensity) { -40.dp.roundToPx() } } +
                expandVertically(expandFrom = Alignment.Top) +
                fadeIn(initialAlpha = 0.3f),
            exit = slideOutVertically() + shrinkVertically() + fadeOut()
        ) {
            // Common toolbar for all pages
            MainToolbar(
                modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
                documentState = documentState,
                windowInsets = WindowInsets.captionBar,
                colorScheme = createCustomUiColors(),
                onHeightChanged = { height ->
                    toolbarHeight = with(localDensity) { height.toDp() }
                }
            )
        }
    }
}

/**
 * Creates a document manager with listeners for document events
 */
@Composable
private fun createDocumentManager(
    context: Context,
    onImmersiveModeChanged: (Boolean) -> Unit
) = getDefaultDocumentManager(
    documentListener = DefaultListeners.documentListeners(
        onDocumentLoaded = {
            Toast.makeText(context, "Document loaded", Toast.LENGTH_SHORT).show()
        }
    ),
    annotationListener = DefaultListeners.annotationListeners(
        onAnnotationSelected = { annotation, _ ->
            Toast.makeText(
                context,
                "${annotation.type} selected",
                Toast.LENGTH_SHORT
            ).show()
        },
        onAnnotationDeselected = { annotation, _ ->
            Toast.makeText(
                context,
                "${annotation.type} deselected",
                Toast.LENGTH_SHORT
            ).show()
        }
    ),
    uiListener = DefaultListeners.uiListeners(
        onImmersiveModeEnabled = onImmersiveModeChanged
    )
)

/**
 * Creates custom UI colors for the toolbar
 */
@Composable
private fun createCustomUiColors() = getUiColors().copy(
    mainToolbar = MainToolbarColors(
        backgroundColor = MaterialTheme.colorScheme.primary,
        textColor = MaterialTheme.colorScheme.onPrimary,
        popup = ToolbarPopupColors(backgroundColor = MaterialTheme.colorScheme.primary),
        titleTextColor = MaterialTheme.colorScheme.onPrimary
    )
)

/**
 * Utility object for file operations
 */
object FileUtils {
    /**
     * Copies files from assets to local storage
     *
     * @param context Android context
     * @param fileNames List of filenames to copy
     * @param onComplete Callback to run after copying completes
     */
    fun copyFilesFromAssetsToLocalStorage(
        context: Context,
        fileNames: List<String>,
        onComplete: () -> Unit = {}
    ) {
        fileNames.forEach { fileName ->
            val destinationFile = File(context.filesDir, "$fileName.pdf")

            if (!destinationFile.exists()) {
                try {
                    context.assets.open("$fileName.pdf").use { inputStream ->
                        destinationFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream, BUFFER_SIZE)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        onComplete()
    }

    private const val BUFFER_SIZE = 8192
}
