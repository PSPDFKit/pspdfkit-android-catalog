/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.examples.kotlin.instant.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.pspdfkit.catalog.R

/** Allows connecting to the Instant Examples server when running the catalog in the emulator. */
class EnterDocumentLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_instant_document_link)
        val openDocument = findViewById<Button>(R.id.open_document_button)
        openDocument.setOnClickListener { openDocument() }
    }

    private fun openDocument() {
        val textField = findViewById<EditText>(R.id.link_text_field)
        val documentLink = textField.text.toString()
        val returnIntent = Intent()
        returnIntent.putExtra(DOCUMENT_LINK_ENCODED_KEY, documentLink)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    companion object {
        const val DOCUMENT_LINK_RESULT_REQUEST_CODE = 3
        const val DOCUMENT_LINK_ENCODED_KEY = "DOCUMENT_LINK_ENCODED_KEY"
    }
}
