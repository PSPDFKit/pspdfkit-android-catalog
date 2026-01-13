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
import android.view.Menu
import android.view.MenuItem
import com.pspdfkit.annotations.actions.JavaScriptAction
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask.extract
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/** This example shows how to create and execute JavaScript actions programmatically. */
class JavaScriptActionsExample(context: Context) : SdkExample(context, R.string.javaScriptActionExampleTitle, R.string.javaScriptActionExampleDescription) {

    @SuppressLint("SetJavaScriptEnabled")
    override fun launchExample(
        context: Context,
        configuration: PdfActivityConfiguration.Builder
    ) {
        configuration
            // JavaScript is enabled by default. It can be disabled in configuration.
            .setJavaScriptEnabled(true)
            // Turn off saving, so we have the clean original document every time the example is launched.
            .autosaveEnabled(false)

        extract(WELCOME_DOC, title, context) { documentFile ->
            // Launch the custom example activity using the document and configuration.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(JavaScriptActionsActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * This activity shows how to create and execute JavaScript actions programmatically.
 *
 * @see JavaScriptActionsExample
 */
class JavaScriptActionsActivity : PdfActivity() {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, SHOW_ALERT, 0, "Show Alert")
        menu.add(0, GO_TO_NEXT_PAGE, 0, "Go To Next Page")
        menu.add(0, SHOW_FORM_FIELDS, 0, "Show Form Fields")
        menu.add(0, MAIL_TO, 0, "Mail to")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            SHOW_ALERT -> {
                executeShowAlertAction()
                true
            }
            GO_TO_NEXT_PAGE -> {
                executeGoToNextPageAction()
                true
            }
            SHOW_FORM_FIELDS -> {
                executeShowFormFieldsAction()
                true
            }
            MAIL_TO -> {
                executeMailDocumentAction()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    /** Shows simple alert dialog through JavaScript. */
    private fun executeShowAlertAction() {
        executeJavaScript("app.alert('This is a JavaScript alert!')")
    }

    /** Changes to next page via JavaScript. */
    private fun executeGoToNextPageAction() {
        executeJavaScript("doc.pageNum = doc.pageNum + 1")
    }

    /** Prints list of form field names via JavaScript. */
    private fun executeShowFormFieldsAction() {
        executeJavaScript(
            """
            var message = 'There are ' + this.numFields + ' form fields in this document.\n';
            for (var i = 0; i < this.numFields; i++) {
               message += 'Field[' + i + '] = ' + this.getNthFieldName(i) + '\n';
            };
            app.alert(message);
            """.trimIndent()
        )
    }

    /** Opens default mail client with document as attachment via JavaScript. */
    private fun executeMailDocumentAction() {
        executeJavaScript(
            """
            doc.mailDoc(true,
                "john@nutrient.io",
                "cc@nutrient.io",
                "bcc@nutrient.io",
                "subject",
                "This is a message");
            """.trimIndent()
        )
    }

    /**
     * Shows how to execute JavaScript scripts on the [com.pspdfkit.ui.PdfFragment] without attaching
     * them to annotations.
     */
    private fun executeJavaScript(script: String) {
        val javaScriptAction = JavaScriptAction(script)
        pdfFragment?.executeAction(javaScriptAction)
    }

    private companion object {
        private const val SHOW_ALERT = 1
        private const val GO_TO_NEXT_PAGE = 2
        private const val SHOW_FORM_FIELDS = 3
        private const val MAIL_TO = 4
    }
}
