/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.signatures.SignatureColorOptions
import com.pspdfkit.configuration.signatures.SignatureSavingStrategy
import com.pspdfkit.signatures.storage.DatabaseSignatureStorage
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * Showcases how to initialize a signature storage database that can be used for storing electronic signatures.
 */
class SignatureStorageDatabaseExample(context: Context) :
    SdkExample(
        context,
        R.string.signatureStorageDatabaseExampleTitle,
        R.string.signatureStorageDatabaseExampleDescription
    ) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // When setting [SignatureSavingStrategy.SAVE_IF_SELECTED], the signature saving
        // dialog will show an option that allows users to store the signature into the database provided that
        // a SignatureStorage database has been set (see ¦SignatureStorageDatabaseActivity`).
        configuration.signatureSavingStrategy(SignatureSavingStrategy.SAVE_IF_SELECTED)
        // Extract the document to the Catalog's private files, so that examples can freely modify the file.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // Create an intent for showing the SignatureStorageDatabaseActivity.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                // Signature form field on page index 16.
                .configuration(
                    configuration
                        .page(16)
                        // An example of changing the default signing colors is here as well.
                        .signatureColorOptions(SignatureColorOptions.fromColorInt(Color.BLACK, Color.GRAY, Color.RED))
                        .build()
                )
                .activityClass(SignatureStorageDatabaseActivity::class)
                .build()

            // Start the SignatureStorageDatabaseActivity showing the demo document.
            context.startActivity(intent)
        }
    }
}

/**
 * Shows how to initialize a signature storage database in custom [PdfActivity].
 */
class SignatureStorageDatabaseActivity : PdfActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize a signature storage database that uses SQLite to store signatures.
        requirePdfFragment().signatureStorage = DatabaseSignatureStorage.withName(
            this,
            // You can use any name for your database signature storage. Here, the default one
            // suggested by PSPDFKit. For example, if your app supports multiple users, you can
            // have a separate database signature storage for every user.
            DatabaseSignatureStorage.SIGNATURE_DB_NAME
        )
    }
}
