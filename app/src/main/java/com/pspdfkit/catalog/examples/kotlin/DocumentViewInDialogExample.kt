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
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * Shows how to host a [DocumentView] inside a Navigation Compose `dialog(...)` destination, so the document is
 * previewed in a dialog window on top of the rest of the UI. The document keeps rendering across configuration
 * changes and when the app is minimized and restored.
 */
class DocumentViewInDialogExample(context: Context) :
    SdkExample(context, R.string.documentViewInDialogExampleTitle, R.string.documentViewInDialogExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, DocumentViewInDialogActivity::class.java)
            intent.putExtra(DocumentViewInDialogActivity.EXTRA_URI, Uri.fromFile(documentFile))
            intent.putExtra(DocumentViewInDialogActivity.EXTRA_CONFIG, configuration.build())
            context.startActivity(intent)
        }
    }
}

class DocumentViewInDialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!
        val pdfActivityConfiguration = intent.getSupportParcelableExtra(EXTRA_CONFIG, PdfActivityConfiguration::class.java)
            ?: PdfActivityConfiguration
                .Builder(this)
                .setUserInterfaceViewMode(UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_AUTOMATIC)
                .fitMode(PageFitMode.FIT_TO_WIDTH)
                .build()

        setContent {
            CatalogTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home",
                ) {
                    composable("home") {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Button(onClick = { navController.navigate("pdf") }) {
                                Text("Open PDF Preview")
                            }
                        }
                    }
                    dialog(route = "pdf") {
                        val documentState = rememberDocumentState(uri, pdfActivityConfiguration)

                        Column(
                            modifier = Modifier
                                .background(Color.White)
                                .systemBarsPadding()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "PDF Preview",
                                fontSize = 18.sp,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )

                            DocumentView(
                                modifier = Modifier.fillMaxWidth(),
                                documentState = documentState,
                                documentManager = getDefaultDocumentManager(),
                            )

                            TextButton(
                                onClick = { navController.navigateUp() },
                                modifier = Modifier.padding(vertical = 8.dp),
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_URI = "DocumentViewInDialogActivity.DocumentUri"
        const val EXTRA_CONFIG = "DocumentViewInDialogActivity.Configuration"
    }
}
