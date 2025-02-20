/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.UiThread
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
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
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

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
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(FormFillingActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

class FormFillingActivity : PdfActivity() {

    /** Holds disposables for all async operations done in this example. We dispose this when destroying the activity. */
    private val disposables = CompositeDisposable()

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
        document.formProvider.formElementsAsync.subscribe { formElements ->
            for (formElement in formElements) {
                when (formElement.type) {
                    FormType.TEXT -> {
                        val textFormElement = formElement as TextFormElement
                        when (textFormElement.inputFormat) {
                            TextInputFormat.DATE -> textFormElement.setText("03/14/1994")
                            else -> textFormElement.setText("Example " + formElement.getName())
                        }
                    }
                    FormType.CHECKBOX -> (formElement as CheckBoxFormElement).toggleSelection()
                    FormType.RADIOBUTTON -> (formElement as RadioButtonFormElement).toggleSelection()
                    else -> Unit
                }
            }
        }.addToDisposables()
    }

    /**
     * Resets all form fields in the document to their default values.
     */
    private fun resetForm() {
        val document = document ?: return

        document.formProvider.formFieldsAsync.subscribe { formFields ->
            for (formField in formFields) {
                formField.reset()
            }
        }.addToDisposables()
    }

    /**
     * Shows how to query form fields/elements by their name.
     */
    private fun fillByName() {
        val document = document ?: return

        // Form fields can be queried by their fully qualified name.
        // Each form field can have multiple child form elements that are
        // the widget annotations that are visually representing actionable
        // controls inside the form field.
        document.formProvider.getFormFieldWithFullyQualifiedNameAsync("Sex").subscribe { formField ->
            val formElement = formField as RadioButtonFormField
            // Sex radio button field has 2 child form elements. These represent 2 radio buttons in the radio group.
            // First radio element has the name "Sex.0" and represents the MALE option.
            //      formElement.getFormElements().get(0)
            // Second radio element has name "Sex.1" and represents the FEMALE option.
            //      formElement.getFormElements().get(1)
            // Select the MALE radio option.
            formElement.formElements[0].select()
        }.addToDisposables()

        // Form elements (visible portion of the form field) can be queried by their name and filled that way.
        document.formProvider.getFormElementWithNameAsync("First Name").subscribe { formElement ->
            (formElement as TextFormElement).setText("John")
        }.addToDisposables()
        document.formProvider.getFormElementWithNameAsync("Last Name").subscribe { formElement ->
            (formElement as TextFormElement).setText("Appleseed")
        }.addToDisposables()

        // Querying form elements by name can be slow. If you need to fill many form elements
        // at once, retrieve list of all form fields/elements first and iterate through it.
        document.formProvider.formElementsAsync.subscribe { formElements ->
            // For the sake of example we'll fill only address fields here.
            val formFillMap = mapOf(
                "Address_1" to "7440-7498 S Hanna St.",
                "Address_2" to "",
                "City" to "Fort Wayne",
                "STATE" to "IN",
                "ZIP" to "46774"
            )

            formElements.asSequence()
                .filterIsInstance(TextFormElement::class.java)
                .forEach { textElement ->
                    formFillMap[textElement.name]?.let { textElement.setText(it) }
                }
        }.addToDisposables()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, RESET_FORM_MENU_ITEM_ID, 0, "Reset form")
        menu.add(0, FILL_BY_FIELD_NAME, 0, "Fill by field name")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            RESET_FORM_MENU_ITEM_ID -> {
                resetForm()
                true
            }
            FILL_BY_FIELD_NAME -> {
                fillByName()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private fun Disposable.addToDisposables() = disposables.add(this)

    companion object {
        private const val RESET_FORM_MENU_ITEM_ID = 1
        private const val FILL_BY_FIELD_NAME = 2
    }
}
