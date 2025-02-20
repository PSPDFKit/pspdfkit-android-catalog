/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

@file:SuppressLint("UsingMaterialAndMaterial3Libraries")

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.utilities.ExperimentalPSPDFKitApi
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.utils.getSupportParcelableExtra

private const val TAG = "FormInterceptActivity"
class FormInterceptActivity : AppCompatActivity() {

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

                val context = LocalContext.current
                Box(Modifier.padding(paddingValues)) {
                    DocumentView(
                        documentState = documentState,
                        modifier = Modifier.fillMaxSize(),
                        documentManager = getDefaultDocumentManager(
                            documentListener = DefaultListeners.documentListeners(
                                onDocumentLoaded = {
                                    Log.e(TAG, "onDocumentLoaded ${it.title}")
                                    documentState.documentConnection.setPageIndex(16)
                                },
                                onPageChanged = { document, page ->
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
                            ),
                            formListener = DefaultListeners.formListeners(
                                addOnFormElementClickedListener = { formElement ->
                                    Toast.makeText(
                                        context,
                                        "Clicked form of type ${formElement.type}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    true
                                },
                                addOnFormElementViewUpdatedListener = { formElement ->
                                    println("Form click addOnFormElementViewUpdatedListener formElement ${formElement.type}")
                                    true
                                },
                                addOnFormElementValidationSuccess = { formElement ->
                                    println("Form click addOnFormElementValidationSuccess formElement ${formElement.type}")
                                    true
                                },
                                addOnFormElementValidationFailed = { formElement, error ->
                                    println("Form click addOnFormElementValidationFailed formElement ${formElement.type} error $error")
                                    true
                                },
                                addOnEnterFormElementEditingMode = {
                                    println("Form click addOnEnterFormElementEditingMode")
                                    true
                                },
                                addOnChangeFormElementEditingMode = {
                                    println("Form click addOnChangeFormElementEditingMode")
                                    true
                                },
                                addOnExitFormElementEditingMode = {
                                    println("Form click addOnExitFormElementEditingMode")
                                    true
                                },
                                addOnFormElementUpdatedListener = { formElement ->
                                    println("Form click addOnFormElementUpdatedListener formElement ${formElement.type}")
                                    true
                                },
                                addOnFormElementSelectedListener = { formElement ->
                                    println("Form click addOnFormElementSelectedListener formElement ${formElement.type}")
                                    true
                                },
                                addOnFormElementDeselectedListener = { formElement, _ ->
                                    println("Form click addOnFormElementDeselectedListener formElement ${formElement.type}")
                                    true
                                }
                            )
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_URI = "JetpackComposeActivity.DocumentUri"
    }
}
