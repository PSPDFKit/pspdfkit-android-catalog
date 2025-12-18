/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
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
import com.pspdfkit.annotations.actions.JavaScriptAction
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import java.io.File

/** Shows how to fill forms via JavaScript.  */
class JavaScriptFormFillingExample(context: Context) : SdkExample(
    context.getString(R.string.formFillingJavaScriptExampleTitle),
    context.getString(R.string.formFillingJavaScriptExampleDescription)
) {
    override fun launchExample(
        context: Context,
        configuration: PdfActivityConfiguration.Builder
    ) {
        configuration // Turn off saving, so we have the clean original document every time the example is launched.
            .autosaveEnabled(false)
            .formEditingEnabled(true)
            .build()

        // Extract the document from the assets.
        ExtractAssetTask.extract("Form_example.pdf", title, context) { documentFile: File? ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(JavaScriptFormFillingActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }
}

/** This activity shows how to fill document forms via JavaScript */
class JavaScriptFormFillingActivity : PdfActivity() {

    companion object {
        private const val RESET_FORM_MENU_ITEM_ID = 1
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, RESET_FORM_MENU_ITEM_ID, 0, "Reset form")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        RESET_FORM_MENU_ITEM_ID -> {
            resetForm()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        fillForms()
    }

    /** Shows how to fill form fields via JavaScript. */
    private fun fillForms() {
        executeJavaScript("var f = doc.getField('Sex'); f.checkThisBox(0, true);")
        executeJavaScript("var f = doc.getField('Name_Last'); f.value = 'John';")
        executeJavaScript("var f = doc.getField('Name_First'); f.value = 'Appleseed';")
    }

    /** Resets all form fields in the document to their default values. */
    private fun resetForm() {
        executeJavaScript("doc.resetForm()")
    }

    /** Executes a JavaScript script on the [com.pspdfkit.ui.PdfFragment]. */
    private fun executeJavaScript(script: String) {
        pdfFragment?.executeAction(JavaScriptAction(script))
    }
}
