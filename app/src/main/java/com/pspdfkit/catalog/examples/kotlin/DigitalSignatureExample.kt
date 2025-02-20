/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.signatures.SignerOptions
import com.pspdfkit.signatures.SigningManager
import com.pspdfkit.signatures.getPrivateKeyEntryFromP12Stream
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.utils.PdfLog
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

/**
 * An example that shows how to digitally sign a PDF document using [SigningManager].
 * This is a Simple implementation where user provides Private key in [SignerOptions].
 */
class DigitalSignatureExample(context: Context) : SdkExample(context, R.string.digitalSignatureExampleTitle, R.string.digitalSignatureExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val assetName = "Form_example.pdf"

        val unsignedDocument = PdfDocumentLoader.openDocument(context, DocumentSource(AssetDataProvider(assetName)))
        val keyEntryWithCertificates = getPrivateKeyEntry(context)
        val signatureFormFields = unsignedDocument.documentSignatureInfo.signatureFormFields
        val outputFile = File(context.filesDir, "signedDocument.pdf")
        outputFile.delete() // make sure output is deleted from previous runs.
        val signerOptions = SignerOptions.Builder(signatureFormFields[0], Uri.fromFile(outputFile))
            .setPrivateKey(keyEntryWithCertificates)
            .setType(digitalSignatureType)
            .build()

        /** [SignerOptions] contains all the required configuration for [SigningManager]*/
        SigningManager.signDocument(
            context = context,
            signerOptions = signerOptions,
            onFailure = { e ->
                Toast.makeText(context, "Error launching example. See logcat for details.", Toast.LENGTH_SHORT).show()
                PdfLog.e("DigitalSignatureExample", e, "Error while launching example.")
            }
        ) {
            val signedDocument = Uri.fromFile(outputFile)
            // Load and show the signed document.
            val intent = PdfActivityIntentBuilder.fromUri(context, signedDocument)
                .configuration(configuration.build())
                .build()
            context.startActivity(intent)
        }
    }

    /**
     * Loads the [KeyStore.PrivateKeyEntry] that will be used by our [SigningManager].
     */
    @Throws(IOException::class, GeneralSecurityException::class)
    private fun getPrivateKeyEntry(context: Context): KeyStore.PrivateKeyEntry {
        // Inside a p12 we have both the certificate (or certificate chain to the root CA) and private key used for signing.
        val keystoreFile = context.assets.open("digital-signatures/ExampleSigner.p12")
        return getPrivateKeyEntryFromP12Stream(keystoreFile, "test")
    }
}
