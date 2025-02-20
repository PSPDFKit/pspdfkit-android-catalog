/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.forms.TextFormConfiguration
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.utilities.ExperimentalPSPDFKitApi
import com.pspdfkit.jetpack.compose.views.DocumentView

private const val URL_PARAM = "url_param"

/**
 * This activity shows how DocumentView can be used with Navigation Component.
 */
class NavHostExample(context: Context) : SdkExample(context, R.string.navHostExampleTitle, R.string.navHostExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Extract the document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { url ->
            context.startActivity(
                Intent(context, NavHostActivity::class.java).apply {
                    putExtra(URL_PARAM, url.toUri().toString())
                }
            )
        }
    }
}

/**
 * This activity will create a simple navigation flow between two screens, each containing a DocumentView.
 */
class NavHostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(URL_PARAM) ?: ""
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "ScreenOne",
                        modifier = Modifier.fillMaxSize().padding(it)
                    ) {
                        composable("ScreenOne") { ScreenOne(path) { navController.navigate("ScreenTwo") } }
                        composable("ScreenTwo") { ScreenTwo(path) { navController.popBackStack() } }
                    }
                }
            }
        }
    }
}

/**
 * ScreenOne contains a DocumentView
 */
@OptIn(ExperimentalPSPDFKitApi::class)
@Composable
fun ScreenOne(path: String, navigate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val pdfActivityConfiguration = remember {
            PdfActivityConfiguration
                .Builder(context)
                .layoutMode(PageLayoutMode.AUTO)
                .scrollMode(PageScrollMode.CONTINUOUS)
                .fitMode(PageFitMode.FIT_TO_WIDTH)
                .pagePadding(8)
                .scrollDirection(PageScrollDirection.VERTICAL)
                .setUserInterfaceViewMode(UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_HIDDEN)
                .autosaveEnabled(false)
                .formEditingEnabled(false)
                .build()
        }

        val documentState = rememberDocumentState(documentUri = Uri.parse(path), configuration = pdfActivityConfiguration)
        DocumentView(
            documentState = documentState,
            documentManager = getDefaultDocumentManager(
                documentListener = DefaultListeners.documentListeners(
                    onDocumentLoaded = { i ->
                        i.formProvider
                            .addFormElementToPage<TextFormConfiguration>(
                                "text-field-1",
                                TextFormConfiguration.Builder(0, RectF(30f, 750f, 200f, 720f))
                                    .setText("Hello new Text")
                                    .build()
                            )
                        Toast.makeText(context, "Document loaded!", Toast.LENGTH_SHORT).show()
                    }
                )
            )
        )
        OutlinedButton(navigate, modifier = Modifier.padding(24.dp)) {
            Text("Navigate by NavController")
        }
    }
}

/**
 * ScreenTwo contains a DocumentView with a slightly different configuration.
 */
@OptIn(ExperimentalPSPDFKitApi::class)
@Composable
fun ScreenTwo(path: String, navigate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val pdfActivityConfiguration = remember {
            PdfActivityConfiguration
                .Builder(context)
                .layoutMode(PageLayoutMode.AUTO)
                .scrollMode(PageScrollMode.PER_PAGE)
                .fitMode(PageFitMode.FIT_TO_WIDTH)
                .pagePadding(8)
                .scrollDirection(PageScrollDirection.VERTICAL)
                .setUserInterfaceViewMode(UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_HIDDEN)
                .autosaveEnabled(false)
                .formEditingEnabled(true)
                .build()
        }

        val documentState = rememberDocumentState(documentUri = Uri.parse(path), configuration = pdfActivityConfiguration)
        DocumentView(
            documentState = documentState,
            documentManager = getDefaultDocumentManager(
                documentListener = DefaultListeners.documentListeners(
                    onDocumentLoaded = { i ->
                        i.formProvider
                            .addFormElementToPage<TextFormConfiguration>(
                                "text-field-editable",
                                TextFormConfiguration.Builder(0, RectF(30f, 750f, 200f, 720f))
                                    .setText("Hello new Text")
                                    .build()
                            )
                        Toast.makeText(context, "Document loaded!", Toast.LENGTH_SHORT).show()
                    }
                )
            )
        )
        Box(modifier = Modifier.background(MaterialTheme.colors.primarySurface)) {
            TopAppBar(
                title = { Text("${documentState.getTitle()}") },
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(navigate) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.statusBarsPadding()
            )
        }
    }
}
