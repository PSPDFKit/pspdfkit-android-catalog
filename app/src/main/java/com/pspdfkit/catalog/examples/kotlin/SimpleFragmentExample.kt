/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * A very simple [PdfFragment] example with minimum configuration.
 * Adds a document listener to give access to the document.
 */
class SimpleFragmentExample(context: Context) : SdkExample(context, R.string.simpleFragmentExampleTitle, R.string.simpleFragmentExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = Intent(context, SimpleFragmentActivity::class.java)

            // We pass the Uri for the PDF file that should be opened in `PdfFragment` via Intent extra.
            intent.putExtra(SimpleFragmentActivity.EXTRA_URI, Uri.fromFile(documentFile))

            // We pass the `PdfFragment` configuration via another extra.
            intent.putExtra(
                SimpleFragmentActivity.EXTRA_CONFIGURATION,
                configuration.build().configuration
            )

            context.startActivity(intent)
        }
    }
}

class SimpleFragmentActivity : AppCompatActivity(), DocumentListener {
    private lateinit var fragment: PdfFragment
    private lateinit var configuration: PdfConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_fragment)

        // Get the Uri provided when launching the activity.
        val documentUri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)
            ?: throw IllegalStateException("Activity Intent was missing Uri extra!")

        // Get the configuration from the provided Intent.
        configuration = intent.getSupportParcelableExtra(EXTRA_CONFIGURATION, PdfConfiguration::class.java)
            ?: throw IllegalStateException("Activity Intent was missing configuration extra!")

        fragment = PdfFragment.newInstance(documentUri, configuration)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        fragment.addDocumentListener(this)
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        println("Document loaded!")
        // Do your magic here...
    }

    companion object {
        const val EXTRA_CONFIGURATION = "SimpleFragmentActivity.EXTRA_CONFIGURATION"
        const val EXTRA_URI = "SimpleFragmentActivity.EXTRA_URI"
    }
}
