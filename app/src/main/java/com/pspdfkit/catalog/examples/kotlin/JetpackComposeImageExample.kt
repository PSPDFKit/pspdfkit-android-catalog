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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.ui.theming.CatalogTheme
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.ImageDocumentLoader
import com.pspdfkit.jetpack.compose.interactors.rememberImageDocumentState
import com.pspdfkit.jetpack.compose.views.ImageDocumentView
import com.pspdfkit.ui.PdfUiFragment
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * Opens the example image using Jetpack Compose.
 */
class JetpackComposeImageExample(context: Context) : SdkExample(
    context,
    R.string.jetpackImageExampleTitle,
    R.string.jetpackImageExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(ANDROID_IMAGE_PNG, title, context) { documentFile ->
            val intent = Intent(context, JetpackComposeImageActivity::class.java)
            intent.putExtra(JetpackComposeImageActivity.EXTRA_URI, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}

/**
 * This example shows you how to use the [PdfUiFragment] to display PDFs in your activities.
 */
class JetpackComposeImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)!!

        setContent {
            CatalogTheme {
                Surface(
                    modifier = Modifier.background(color = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    val imageDocumentConfiguration =
                        ImageDocumentLoader.getDefaultImageDocumentActivityConfiguration(this)

                    val documentState = rememberImageDocumentState(uri, imageDocumentConfiguration)
                    ImageDocumentView(
                        documentState = documentState,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Loading via uri is also supported, and the default pdfActivityConfiguration is being used
                    // ImageDocumentView(
                    //     imageUri = uri,
                    //     modifier = Modifier.fillMaxSize()
                    // )
                }
            }
        }
    }

    companion object {
        const val EXTRA_URI = "JetpackComposeImageActivity.DocumentUri"
    }
}
