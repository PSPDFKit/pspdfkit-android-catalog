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
import com.pspdfkit.signatures.getPrivateKeyFromPemFile
import com.pspdfkit.signatures.loadCertificateFromStream
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.utils.PdfLog
import java.io.File
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * An example that shows how to digitally sign a PDF document using [SigningManager].
 * This is a Simple implementation where user provides Private key in [SignerOptions].
 */
class LongTermValidationExample(context: Context) : SdkExample(context, R.string.digitalSignatureLtvExampleTitle, R.string.digitalSignatureLtvExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val assetName = "Form_example.pdf"

        val unsignedDocument = PdfDocumentLoader.openDocument(context, DocumentSource(AssetDataProvider(assetName)))
        val privateKey = getPrivateKey(context)
        val certificates = getCertificates(context)
        val signatureFormFields = unsignedDocument.documentSignatureInfo.signatureFormFields
        val outputFile = File(context.filesDir, "signedDocument.pdf")
        outputFile.delete() // make sure output is deleted from previous runs.
        val signerOptions = SignerOptions.Builder(signatureFormFields[0], Uri.fromFile(outputFile))
            .setPrivateKey(privateKey)
            .setCertificates(certificates)
            // LTV is enabled by default, but you can disable it here if you don't need or experience issues with it.
            .setEnableLtv(true)
            .setType(digitalSignatureType)
            .build()

        /** [SignerOptions] contains all the required configuration for [SigningManager]*/
        SigningManager.signDocument(
            context = context,
            signerOptions = signerOptions,
            onFailure = { e ->
                Toast.makeText(context, "Error launching example. See logcat for details.", Toast.LENGTH_SHORT).show()
                PdfLog.e("AdvancedLtvExample", e, "Error while launching example.")
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
     * Loads the full chain of [X509Certificate]s used for signing; from the signer's certificate, to the issuing authority certificate
     * that will be used by our [SigningManager].
     * In this example, we are using 3 certificates (signer, issuer, and root).
     */
    private fun getCertificates(context: Context): List<X509Certificate> {
        return listOf(
            loadCertificateFromStream(context.assets.open("digital-signatures/ltv/Signer.cert")),
            loadCertificateFromStream(context.assets.open("digital-signatures/ltv/Issuer.cert")),
            loadCertificateFromStream(context.assets.open("digital-signatures/ltv/Root.cert"))
        )
    }

    /**
     * Loads the [PrivateKey] that will be used by our [SigningManager].
     */
    private fun getPrivateKey(context: Context): PrivateKey {
        return getPrivateKeyFromPemFile(context.assets.open("digital-signatures/ltv/Signer.key"))
    }
}
