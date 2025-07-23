/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.kotlin.instant.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.pspdfkit.catalog.examples.kotlin.instant.api.InstantExampleDocumentDescriptor
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.jetpack.compose.interactors.getDefaultInstantDocumentManager
import com.pspdfkit.jetpack.compose.interactors.rememberInstantDocumentState
import com.pspdfkit.jetpack.compose.views.InstantDocumentView

/**
 * An example activity that demonstrates how to use the InstantDocumentView composable to display an Instant document.
 */

class InstantComposeExampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract the PSPDFKit bundle holding all internal extras.
        val extra = intent.extras ?: throw IllegalArgumentException("Intent extras are required")
        val instantDocumentSource: InstantExampleDocumentDescriptor = extra.getParcelable(DOCUMENT_DESCRIPTOR)
            ?: throw IllegalArgumentException("InstantExampleDocumentDescriptor is required")
        val configuration: PdfActivityConfiguration = extra.getParcelable(CONFIGURATION) ?: PdfActivityConfiguration.Builder(this).build()

        setContent {
            CatalogTheme {
                val documentState = rememberInstantDocumentState(
                    serverUrl = instantDocumentSource.serverUrl,
                    jwt = instantDocumentSource.jwt,
                    configuration = configuration
                )
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
                    }
                ) { paddingValues ->
                    Box(Modifier.padding(paddingValues)) {
                        InstantDocumentView(
                            documentState = documentState,
                            modifier = Modifier.fillMaxSize(),
                            instantDocumentManager = getDefaultInstantDocumentManager()
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val DOCUMENT_DESCRIPTOR = "InstantComposeExampleActivity.DOCUMENT_DESCRIPTOR"
        const val CONFIGURATION = "InstantComposeExampleActivity.CONFIGURATION"
    }
}
