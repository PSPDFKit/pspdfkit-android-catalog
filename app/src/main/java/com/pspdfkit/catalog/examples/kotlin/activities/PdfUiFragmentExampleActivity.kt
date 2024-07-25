/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.activities

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pspdfkit.catalog.R
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.ui.PdfUiFragment
import com.pspdfkit.ui.PdfUiFragmentBuilder
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * This example shows you how to use the [PdfUiFragment] to display PDFs in your activities.
 */
class PdfUiFragmentExampleActivity : AppCompatActivity(), DocumentListener {

    private lateinit var pdfUiFragment: PdfUiFragment

    private var observer: PdfUiFragmentLifecycleObserver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdfuifragment_example)

        if (supportFragmentManager.findFragmentByTag("pdfUiFragment") == null) {
            // PdfFragment configuration is provided with the launching intent.
            var configuration: PdfActivityConfiguration? = intent.getSupportParcelableExtra(EXTRA_CONFIGURATION, PdfActivityConfiguration::class.java)
            if (configuration == null) {
                configuration = PdfActivityConfiguration.Builder(this).build()
            }

            // The actual document Uri is provided with the launching intent. You can simply change that inside the PdfUiFragmentExample class.
            val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)

            // Create our PdfUiFragment if it doesn't exist yet.
            pdfUiFragment = PdfUiFragmentBuilder.fromUri(this, uri)
                .configuration(configuration)
                .build()
            supportFragmentManager.beginTransaction().add(R.id.fragmentContainer, pdfUiFragment, "pdfUiFragment").commit()
        } else {
            pdfUiFragment = supportFragmentManager.findFragmentByTag("pdfUiFragment") as PdfUiFragment
        }

        observer = PdfUiFragmentLifecycleObserver(pdfUiFragment, this)
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        println("Loaded!")
        // Do some fun stuff with the document here.
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // If you want the UI to properly respond to backpresses you need to forward this call to the fragment.
        if (!pdfUiFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    // We need a LifecycleObserver to observe the fragment lifecycle
    class PdfUiFragmentLifecycleObserver(
        private val pdfUiFragment: PdfUiFragment,
        private val documentListener: DocumentListener
    ) : DefaultLifecycleObserver {

        init {
            // Add observer
            pdfUiFragment.lifecycle.addObserver(this)
        }

        override fun onStart(owner: LifecycleOwner) {
            // Do stuff on start (add listener etc)
            pdfUiFragment.pdfFragment?.addDocumentListener(documentListener)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            // remember to remove listeners on destroy
            pdfUiFragment.pdfFragment?.removeDocumentListener(documentListener)
            pdfUiFragment.lifecycle.removeObserver(this)
        }
    }

    companion object {
        const val EXTRA_URI = "PdfUiFragmentExampleActivity.DocumentUri"
        const val EXTRA_CONFIGURATION = "PdfUiFragmentExampleActivity.PdfConfiguration"
    }
}
