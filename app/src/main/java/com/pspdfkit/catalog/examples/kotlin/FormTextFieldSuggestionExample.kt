/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.UiThread
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * Showcases how to provide suggestions for text form fields.
 */
class FormTextFieldSuggestionExample(context: Context) : SdkExample(context, R.string.formTextFieldSuggestionsExampleTitle, R.string.formTextFieldSuggestionsExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Turn off saving, so we have the clean original document every time the example is launched.
        configuration.autosaveEnabled(false)

        // Enable form editing UI.
        configuration.formEditingEnabled(true)

        // Extract the example document from the assets.
        ExtractAssetTask.extract("Form_example.pdf", title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(FormTextFieldSuggestionsActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * An activity that demonstrates how to provide suggestions for text form fields.
 */
class FormTextFieldSuggestionsActivity : PdfActivity() {
    @SuppressLint("CheckResult")
    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        pdfFragment?.let {
            it.addOnTextFormElementSuggestionRequestListener { formElement ->
                // Provide suggestions for first and last name fields.
                when (formElement.name) {
                    "First Name" -> {
                        listOf(
                            "Alice", "Andrew", "Anna", "Anthony",
                            "Bob", "Barbara", "Brian",
                            "Catherine", "Charles",
                            "Diana", "David",
                            "Emily", "Emma",
                            "Frank",
                            "George",
                            "Helen",
                            "Jack", "Jane", "John", "Julia",
                            "Katherine",
                            "Linda",
                            "Mark", "Mary", "Michael",
                            "Nancy",
                            "Oliver",
                            "Patricia",
                            "Robert",
                            "Sarah",
                            "Thomas",
                            "Victoria",
                            "William",
                            "Agent"
                        )
                    }
                    "Last Name" -> {
                        listOf(
                            "A. Anderson",
                            "Brown",
                            "Davis",
                            "Doe",
                            "Garcia",
                            "Johnson", "Jones",
                            "Lee",
                            "Martin", "Martinez",
                            "Mayer", "Miller", "Moore",
                            "Rodriguez",
                            "Smith",
                            "Taylor", "Thomas", "Thompson",
                            "White", "Williams", "Wilson"
                        )
                    }
                    else -> emptyList()
                }
            }
        }
    }
}
