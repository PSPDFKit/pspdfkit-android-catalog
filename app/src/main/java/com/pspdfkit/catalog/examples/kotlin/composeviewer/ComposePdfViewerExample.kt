/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.composeviewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.utils.getSupportParcelableExtra
import java.io.File

/**
 * A complete Jetpack Compose PDF viewer app in one catalog example — the in-catalog successor
 * of the standalone `simple-compose` app:
 *
 * - **Library**: recent documents as a grid of live page-1 thumbnails (rendered with
 *   `PdfDocumentLoader`), with system file import (SAF).
 * - **Viewer** ([ViewerScreen]): `DocumentView` with a host-rendered `NutrientMainToolbar`,
 *   the content/viewport inset bookkeeping a custom main toolbar requires, custom toolbar
 *   items, `DefaultListeners` wiring, and a live demonstration of
 *   [com.pspdfkit.jetpack.compose.interactors.DocumentState.setCustomPdfSource]'s async
 *   completion and view-state restore.
 * - **Settings**: light/dark/system theme and Material You dynamic color, persisted in a
 *   DataStore; the choice drives both the app chrome and the document theme.
 *
 * State lives in [ComposeViewerViewModel] — plain `ViewModel` + DataStore, no DI framework or
 * database, so the whole example is copy-paste-able.
 */
class ComposePdfViewerExample(context: Context) :
    SdkExample(context, R.string.composePdfViewerExampleTitle, R.string.composePdfViewerExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Extract the bundled demo document so a fresh install starts with a library entry.
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, ComposePdfViewerActivity::class.java)
                .putExtra(ComposePdfViewerActivity.EXTRA_DEMO_DOCUMENT, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}

class ComposePdfViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val demoDocument = intent.getSupportParcelableExtra(EXTRA_DEMO_DOCUMENT, Uri::class.java)

        setContent {
            val viewModel: ComposeViewerViewModel = viewModel()
            // Seed the library with the bundled demo document on every launch — recordOpened
            // upserts, so an existing entry only refreshes its timestamp. Label it with the asset
            // name: ExtractAssetTask writes the file as "<asset>_<sha1-of-title>", which is not a
            // name to show a reader.
            LaunchedEffect(viewModel) {
                demoDocument?.path?.let { viewModel.recordOpened(File(it), name = SdkExample.WELCOME_DOC) }
            }

            val darkMode by viewModel.darkMode.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            // The catalog's own theme (brand palette + typography), driven by this example's
            // persisted settings rather than CatalogTheme's system defaults — so the Settings
            // screen's theme and dynamic-color controls still work while the viewer looks like
            // the rest of the catalog.
            CatalogTheme(darkTheme = viewerDarkMode(darkMode), dynamicColor = dynamicColor) {
                ViewerNavHost(viewModel)
            }
        }
    }

    companion object {
        const val EXTRA_DEMO_DOCUMENT = "ComposePdfViewerActivity.DemoDocument"
    }
}

/** The app's three destinations; the viewer takes the document's file path as its argument. */
@Composable
private fun ViewerNavHost(viewModel: ComposeViewerViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onOpenDocument = { document ->
                    viewModel.recordOpened(File(document.path))
                    navController.navigate("viewer/${Uri.encode(document.path)}")
                },
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("viewer/{path}") { backStackEntry ->
            val path = Uri.decode(backStackEntry.arguments?.getString("path").orEmpty())
            ViewerScreen(
                path = path,
                isDark = viewerInDarkTheme(viewModel),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/** Resolves the effective dark flag from the tri-state setting. */
@Composable
internal fun viewerInDarkTheme(viewModel: ComposeViewerViewModel): Boolean {
    val darkMode by viewModel.darkMode.collectAsState()
    return viewerDarkMode(darkMode)
}

/** Resolves the tri-state [ViewerDarkMode] setting into the effective dark flag. */
@Composable
private fun viewerDarkMode(darkMode: Int): Boolean = when (darkMode) {
    ViewerDarkMode.DARK -> true
    ViewerDarkMode.LIGHT -> false
    else -> isSystemInDarkTheme()
}
