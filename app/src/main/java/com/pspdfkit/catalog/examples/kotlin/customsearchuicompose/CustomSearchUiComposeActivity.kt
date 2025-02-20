/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

@file:SuppressLint("UsingMaterialAndMaterial3Libraries")

package com.pspdfkit.catalog.examples.kotlin.customsearchuicompose

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.ui.Modifier
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.jetpack.compose.interactors.DefaultListeners
import com.pspdfkit.jetpack.compose.interactors.getDefaultDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberDocumentState
import com.pspdfkit.jetpack.compose.utilities.ExperimentalPSPDFKitApi
import com.pspdfkit.jetpack.compose.views.DocumentView
import com.pspdfkit.ui.search.SearchResultHighlighter
import com.pspdfkit.utils.getSupportParcelableExtra

class CustomSearchUiComposeActivity : AppCompatActivity() {

    private val viewModel: CustomSearchUiComposeViewModel by viewModels { CustomSearchUiComposeViewModel.Factory }

    private var highlighter: SearchResultHighlighter? = null

    @OptIn(ExperimentalPSPDFKitApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!

        highlighter = SearchResultHighlighter(this)

        setContent {
            val searchQuery = viewModel.searchQuery

            Scaffold(
                topBar = {
                    TextField(
                        value = searchQuery,
                        onValueChange = viewModel::performSearch,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(text = "Search") }
                    )
                }
            ) { paddingValues ->

                val pdfActivityConfiguration = PdfActivityConfiguration
                    .Builder(this)
                    .setUserInterfaceViewMode(UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_HIDDEN)
                    .build()

                val documentState = rememberDocumentState(uri, pdfActivityConfiguration)

                Box(Modifier.padding(paddingValues)) {
                    DocumentView(
                        documentState = documentState,
                        modifier = Modifier.fillMaxSize(),
                        documentManager = getDefaultDocumentManager(
                            documentListener = DefaultListeners.documentListeners(
                                onDocumentLoaded = { document ->
                                    viewModel.onDocumentLoaded(
                                        document,
                                        pdfActivityConfiguration.configuration,
                                        documentState,
                                        highlighter
                                    )
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
