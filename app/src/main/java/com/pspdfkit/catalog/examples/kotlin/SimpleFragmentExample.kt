/*
 *   Copyright © 2024-2026 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.catalog.ui.DocumentPickerActivity
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
class SimpleFragmentExample(context: Context) :
    SdkExample(context, R.string.simpleFragmentExampleTitle, R.string.simpleFragmentExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Launch the picker activity to let users choose between default or custom document.
        val intent = Intent(context, SimpleFragmentExamplePickerActivity::class.java)
        intent.putExtra(DocumentPickerActivity.EXTRA_CONFIGURATION, configuration.build())
        context.startActivity(intent)
    }
}

/**
 * Picker that launches [SimpleFragmentActivity] with the selected document.
 *
 * [SimpleFragmentActivity] is a plain [AppCompatActivity] hosting a [PdfFragment],
 * so we override [buildLaunchIntent] to build the intent ourselves instead of
 * relying on the default [com.pspdfkit.ui.PdfActivityIntentBuilder] path.
 */
class SimpleFragmentExamplePickerActivity : DocumentPickerActivity() {
    override val targetActivityClass = SimpleFragmentActivity::class.java

    override fun buildLaunchIntent(uri: Uri, configuration: PdfActivityConfiguration, isImageFile: Boolean): Intent =
        Intent(this, SimpleFragmentActivity::class.java).apply {
            putExtra(SimpleFragmentActivity.EXTRA_URI, uri)
            putExtra(SimpleFragmentActivity.EXTRA_CONFIGURATION, configuration.configuration)
        }
}

class SimpleFragmentActivity :
    AppCompatActivity(),
    DocumentListener {
    private lateinit var fragment: PdfFragment
    private lateinit var configuration: PdfConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_fragment)

        // Get the Uri provided when launching the activity.
        val documentUri =
            intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)
                ?: throw IllegalStateException("Activity Intent was missing Uri extra!")

        // Get the configuration from the provided Intent.
        configuration = intent.getSupportParcelableExtra(EXTRA_CONFIGURATION, PdfConfiguration::class.java)
            ?: throw IllegalStateException("Activity Intent was missing configuration extra!")

        fragment = PdfFragment.newInstance(documentUri, configuration)
        supportFragmentManager
            .beginTransaction()
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
