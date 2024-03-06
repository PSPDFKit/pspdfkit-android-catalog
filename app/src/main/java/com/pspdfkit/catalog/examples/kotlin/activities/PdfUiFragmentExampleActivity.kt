/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.activities

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pspdfkit.catalog.R
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfUiFragment
import com.pspdfkit.ui.PdfUiFragmentBuilder
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * This example shows you how to use the [PdfUiFragment] to display PDFs in your activities.
 */
// You can disable warnings about usage of experimental APIs by adding a supress annotation to your class.
@SuppressLint("pspdfkit-experimental")
class PdfUiFragmentExampleActivity : AppCompatActivity() {

    lateinit var pdfUiFragment: PdfUiFragment

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
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // If you want the UI to properly respond to backpresses you need to forward this call to the fragment.
        if (!pdfUiFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_URI = "PdfUiFragmentExampleActivity.DocumentUri"
        const val EXTRA_CONFIGURATION = "PdfUiFragmentExampleActivity.PdfConfiguration"
    }
}
