/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.compose.toolbar.state.ToolbarItem
import com.pspdfkit.compose.toolbar.state.ToolbarItem.ItemEdge
import com.pspdfkit.compose.toolbar.state.ToolbarItem.OverflowBehavior
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.forms.CheckBoxFormElement
import com.pspdfkit.forms.FormType
import com.pspdfkit.forms.RadioButtonFormElement
import com.pspdfkit.forms.RadioButtonFormField
import com.pspdfkit.forms.TextFormElement
import com.pspdfkit.forms.TextInputFormat
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import kotlinx.coroutines.launch

/**
 * Showcases how to fill forms programmatically.
 */
class FormFillingExample(context: Context) : SdkExample(context, R.string.formFillingExampleTitle, R.string.formFillingExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Turn off saving, so we have the clean original document every time the example is launched.
        configuration.autosaveEnabled(false)

        // Enable form editing UI.
        configuration.formEditingEnabled(true)

        // Extract the example document from the assets.
        ExtractAssetTask.extract("Form_example.pdf", title, context) { documentFile ->
            val intent =
                PdfActivityIntentBuilder
                    .fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(FormFillingActivity::class)
                    .build()
            context.startActivity(intent)
        }
    }
}

class FormFillingActivity : PdfActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarItemsProvider { defaultItems ->
            defaultItems + ToolbarItem.Button(
                id = RESET_FORM_MENU_ITEM_ID,
                actionId = "reset_form",
                contentDescription = "Reset form",
                position = ItemEdge.End,
                overflowBehavior = OverflowBehavior.Never,
            ) + ToolbarItem.Button(
                id = FILL_BY_FIELD_NAME,
                actionId = "fill_by_field",
                contentDescription = "Fill by field name",
                position = ItemEdge.End,
                overflowBehavior = OverflowBehavior.Never,
            )
        }
        setOnToolbarItemClickListener { itemId ->
            when (itemId) {
                RESET_FORM_MENU_ITEM_ID -> {
                    resetForm()
                    true
                }

                FILL_BY_FIELD_NAME -> {
                    fillByName()
                    true
                }

                else -> false
            }
        }
    }

    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        fillAllFormFields()
    }

    /**
     * Fills form fields in the document with text "Example <field name>".
     */
    private fun fillAllFormFields() {
        val document = document ?: return
        lifecycleScope.launch {
            for (formElement in document.formProvider.getFormElements()) {
                when (formElement.type) {
                    FormType.TEXT -> {
                        val textFormElement = formElement as TextFormElement
                        when (textFormElement.inputFormat) {
                            TextInputFormat.DATE -> textFormElement.setText("03/14/1994")
                            else -> textFormElement.setText("Example " + formElement.getName())
                        }
                    }

                    FormType.CHECKBOX -> {
                        (formElement as CheckBoxFormElement).toggleSelection()
                    }

                    FormType.RADIOBUTTON -> {
                        (formElement as RadioButtonFormElement).toggleSelection()
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * Resets all form fields in the document to their default values.
     */
    private fun resetForm() {
        val document = document ?: return

        lifecycleScope.launch {
            for (formField in document.formProvider.getFormFields()) {
                formField.reset()
            }
        }
    }

    /**
     * Shows how to query form fields/elements by their name.
     */
    private fun fillByName() {
        val document = document ?: return

        lifecycleScope.launch {
            // Form fields can be queried by their fully qualified name.
            // Each form field can have multiple child form elements that are
            // the widget annotations that are visually representing actionable
            // controls inside the form field.
            // The "Department" radio button field has 4 child form elements, one per option:
            // Administration, Engineering, Management and Design.
            // Select the Engineering option.
            (document.formProvider.getFormFieldWithFullyQualifiedName("Department") as? RadioButtonFormField)
                ?.formElements?.get(3)?.select()

            // Form elements (visible portion of the form field) can be queried by their name and filled that way.
            (document.formProvider.getFormElementWithName("First Name") as? TextFormElement)?.setText("John")
            (document.formProvider.getFormElementWithName("Last Name") as? TextFormElement)?.setText("Appleseed")

            // Querying form elements by name can be slow. If you need to fill many form elements
            // at once, retrieve list of all form fields/elements first and iterate through it.
            // For the sake of example we'll fill only address fields here.
            val formFillMap =
                mapOf(
                    "Street" to "7440-7498 S Hanna St.",
                    "City" to "Fort Wayne",
                    "State" to "IN",
                    "ZIP" to "46774",
                )

            document.formProvider.getFormElements()
                .asSequence()
                .filterIsInstance(TextFormElement::class.java)
                .forEach { textElement ->
                    formFillMap[textElement.name]?.let { textElement.setText(it) }
                }
        }
    }

    companion object {
        private const val RESET_FORM_MENU_ITEM_ID = 1
        private const val FILL_BY_FIELD_NAME = 2
    }
}
