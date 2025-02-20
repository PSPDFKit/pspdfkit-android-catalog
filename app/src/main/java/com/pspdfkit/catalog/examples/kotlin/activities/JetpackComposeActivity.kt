/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
@file:SuppressLint("UsingMaterialAndMaterial3Libraries")

package com.pspdfkit.catalog.examples.kotlin.activities

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.utilities.ExperimentalPSPDFKitApi
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.ui.PdfUiFragment
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * This example shows you how to use the [PdfUiFragment] to display PDFs in your activities.
 */
private const val TAG = "JetpackComposeActivity"
class JetpackComposeActivity : AppCompatActivity() {

    @OptIn(ExperimentalPSPDFKitApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!

        setContent {
            Scaffold { paddingValues ->
                val pdfActivityConfiguration = PdfActivityConfiguration
                    .Builder(this)
                    .setUserInterfaceViewMode(UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_HIDDEN)
                    .build()

                val documentState = rememberDocumentState(uri, pdfActivityConfiguration)
                var currentPage = pdfActivityConfiguration.page()

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
                                onDocumentSaved = {
                                    Log.e(TAG, "onDocumentSaved ${it.title}")
                                },
                                onDocumentZoomed = { document, pageIndex, scaleFactor ->
                                    Log.e(TAG, "onDocumentZoomed: ${document.title}  $pageIndex $scaleFactor")
                                }
                            ),
                            annotationListener = DefaultListeners.annotationListeners(
                                onAnnotationSelected = { annotation, created ->
                                    Log.e(TAG, "onAnnotationSelected: ${annotation.type.name} $created")
                                },
                                onAnnotationDeselected = { annotation, created ->
                                    Log.e(TAG, "onAnnotationDeselected: ${annotation.type.name} $created")
                                }
                            )
                        )
                    )

                    // Loading via uri is also supported, and the default pdfActivityConfiguration is being used
                    // DocumentView(
                    //     documentUri = uri,
                    //     modifier = Modifier.fillMaxSize()
                    // )

                    // As well as loading images, using image URI
                    // ImageDocumentView(
                    //     imageUri = uri,
                    //     modifier = Modifier.fillMaxSize()
                    // )

                    // Or using documentState
                    // ImageDocumentView(
                    //     documentState = documentState,
                    //     modifier = Modifier.fillMaxSize()
                    // )

                    Button(
                        onClick = { documentState.documentConnection.setPageIndex((currentPage + 1).coerceAtMost(17)) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    ) {
                        Text("Go to next page")
                    }

                    Button(
                        onClick = { documentState.documentConnection.setPageIndex((currentPage - 1).coerceAtLeast(0)) },
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                    ) {
                        Text("Go to Previous page")
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_URI = "JetpackComposeActivity.DocumentUri"
    }
}
