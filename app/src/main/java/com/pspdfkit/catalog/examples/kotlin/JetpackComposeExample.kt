/*
 *   Copyright © 2021-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.ui.PdfUiFragment
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * Opens the example document using Jetpack Compose.
 */
class JetpackComposeExample(context: Context) : SdkExample(context, R.string.jetpackExampleTitle, R.string.jetpackExampleDescription) {
    /** Configuration is handled inside [JetpackComposeActivity] */
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, JetpackComposeActivity::class.java)
            intent.putExtra(JetpackComposeActivity.EXTRA_URI, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}

/**
 * This example shows you how to use the [PdfUiFragment] to display PDFs in your activities.
 */
private const val TAG = "JetpackComposeActivity"
class JetpackComposeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!

        setContent {
            CatalogTheme {
                val pdfActivityConfiguration = PdfActivityConfiguration
                    .Builder(this)
                    .setUserInterfaceViewMode(UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_HIDDEN)
                    .build()

                val documentState = rememberDocumentState(uri, pdfActivityConfiguration)

                var currentPage = pdfActivityConfiguration.page

                Scaffold(
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.onPrimary)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                documentState.documentConnection.save()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "save")
                        }
                    },
                    bottomBar = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Button(
                                onClick = {
                                    documentState.documentConnection.setPageIndex(
                                        (currentPage - 1).coerceAtLeast(
                                            0
                                        )
                                    )
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("Previous page")
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    documentState.documentConnection.setPageIndex(
                                        (currentPage + 1).coerceAtMost(
                                            17
                                        )
                                    )
                                },
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text("Next page")
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(Modifier.padding(paddingValues)) {
                        DocumentView(
                            documentState = documentState,
                            modifier = Modifier.fillMaxSize(),
                            documentManager = getDefaultDocumentManager(
                                documentListener = DefaultListeners.documentListeners(
                                    onDocumentLoaded = {
                                        Log.e(TAG, "onDocumentLoaded ${it.title}")
                                    },
                                    onPageChanged = { document, page ->
                                        currentPage = page
                                        Log.e(TAG, "onPageChanged: ${document.title} - $page")
                                    },
                                    onDocumentSave = { document, _ ->
                                        Log.e(TAG, "onDocumentSave ${document.title}")
                                        true
                                    },
                                    onDocumentSaved = {
                                        Log.e(TAG, "onDocumentSaved ${it.title}")
                                    },
                                    onDocumentZoomed = { document, pageIndex, scaleFactor ->
                                        Log.e(
                                            TAG,
                                            "onDocumentZoomed: ${document.title}  $pageIndex $scaleFactor"
                                        )
                                    }
                                ),
                                annotationListener = DefaultListeners.annotationListeners(
                                    onAnnotationSelected = { annotation, created ->
                                        Log.e(
                                            TAG,
                                            "onAnnotationSelected: ${annotation.type.name} $created"
                                        )
                                    },
                                    onAnnotationDeselected = { annotation, created ->
                                        Log.e(
                                            TAG,
                                            "onAnnotationDeselected: ${annotation.type.name} $created"
                                        )
                                    }
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_URI = "JetpackComposeActivity.DocumentUri"
    }
}
