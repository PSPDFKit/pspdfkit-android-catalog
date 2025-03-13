/*
 *   Copyright © 2020-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.util.Log
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.signatures.DigitalSignatureType
import com.pspdfkit.signatures.SignerOptions
import com.pspdfkit.signatures.SigningManager
import com.pspdfkit.signatures.getPrivateKeyEntryFromP12Stream
import com.pspdfkit.ui.PdfActivityIntentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

/**
 * An example showing the ability to sign a document using a third-party service.
 * */
class ThirdPartySigningExample(context: Context) : SdkExample(context, R.string.thirdPartySigningExampleTitle, R.string.thirdPartySigningExampleDescription) {

    private val TAG = "SigningManager"
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val assetName = "Form_example.pdf"

        val unsignedDocument = PdfDocumentLoader.openDocument(context, DocumentSource(AssetDataProvider(assetName)))
        val keyEntryWithCertificates = getPrivateKeyEntry(context)
        val signatureFormFields = unsignedDocument.documentSignatureInfo.signatureFormFields
        val outputFile = File(context.filesDir, "signedDocument.pdf")
        outputFile.delete() // make sure output is deleted from previous runs.

        /** [SignerOptions] contains all the required configuration for [SigningManager]*/
        val signerOptions = SignerOptions.Builder(signatureFormFields[0], Uri.fromFile(outputFile))
            .setType(digitalSignatureType)
        CoroutineScope(Dispatchers.Main).launch {
            SigningManager.getDataToSign(context, signerOptions.build()).onSuccess {
                // ---  Start  --- //
                // This code can be replaced by a third party signing service that signs the data in PKCS@7 format.
                val newSignerOptions = signerOptions.setPrivateKey(keyEntryWithCertificates).build()
                val signing = if (digitalSignatureType == DigitalSignatureType.BASIC) {
                    SigningManager.signWithBasicSignature(context, newSignerOptions, it.first, it.second)
                } else {
                    SigningManager.signWithCAdESSignature(context, newSignerOptions, it.first, it.second)
                }
                // ---  End  --- //

                signing.onSuccess {
                    SigningManager.embedPKCS7Signature(context, signerOptions.build(), it).onSuccessEmpty {
                        val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(outputFile))
                            .configuration(configuration.build())
                            .build()
                        context.startActivity(intent)
                    }.onError { Log.e(TAG, "embedPKCS7Signature: ${it.localizedMessage}") }
                }.onError { Log.e(TAG, "signWithBasicSignature: ${it.localizedMessage}") }
            }.onError { Log.e(TAG, "getDataToSign: ${it.localizedMessage}") }
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
