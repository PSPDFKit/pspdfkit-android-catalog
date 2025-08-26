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
import com.pspdfkit.catalog.SdkExample.Companion.TAG
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.signatures.SignerOptions
import com.pspdfkit.signatures.SigningManager
import com.pspdfkit.signatures.getPrivateKeyEntryFromP12Stream
import com.pspdfkit.signatures.getX509Certificates
import com.pspdfkit.ui.PdfActivityIntentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.Signature

/**
 * An example showing the ability to sign a document using a two-step signing process.
 * */
class TwoStepSigningExample(context: Context) : SdkExample(context, R.string.twoStepSigningExampleTitle, R.string.twoStepSigningExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val assetName = "Form_example.pdf"

        val unsignedDocument = PdfDocumentLoader.openDocument(context, DocumentSource(AssetDataProvider(assetName)))
        val keyEntryWithCertificates = getPrivateKeyEntry(context)
        val privateKey = keyEntryWithCertificates.privateKey
        val signatureFormFields = unsignedDocument.documentSignatureInfo.signatureFormFields
        val outputFile = File(context.filesDir, "signedDocument.pdf")
        outputFile.delete() // make sure output is deleted from previous runs.

        /** [SignerOptions] contains all the required configuration for [SigningManager]*/
        val signerOptions = SignerOptions.Builder(signatureFormFields[0], Uri.fromFile(outputFile))
            .setPrivateKey(privateKey)
            .setCertificates(keyEntryWithCertificates.getX509Certificates())
            .setType(digitalSignatureType)
        CoroutineScope(Dispatchers.Main).launch {
            SigningManager.getDataToSign(context, signerOptions.build()).onSuccess { unsignedData ->
                val signedData = unsignedData.first.signData(privateKey, unsignedData.second.name)
                SigningManager.embedSignature(context, signerOptions.build(), signedData, unsignedData.first, unsignedData.second).onSuccessEmpty {
                    val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(outputFile))
                        .configuration(configuration.build())
                        .build()
                    context.startActivity(intent)
                }.onError { Log.e(TAG, "embedSignature: ${it.localizedMessage}") }
            }.onError { Log.e(TAG, "getDataToSign: ${it.localizedMessage}") }
        }
    }

    private fun ByteArray.signData(privateKey: PrivateKey, hashAlgorithm: String?): ByteArray = try {
        /** We are using the [Signature] class from java.security package, to sign the byte arrays.*/

        /** The signature algorithm can be, among others, the NIST standard DSA, using DSA and SHA-256.
         *  The DSA algorithm using the SHA-256 message digest algorithm can be specified as SHA256withDSA.
         *  In the case of RSA the signing algorithm could be specified as, for example, SHA256withRSA.
         *  The algorithm name must be specified, as there is no default.
         *
         *  Here 'hashAlgorithm' is SHA256 and 'key.algorithm' is RSA
         *  providing algorithm as SHA256withRSA
         *  for more details visit https://docs.oracle.com/javase/8/docs/api/java/security/Signature.html
         **/
        val algorithm = "${hashAlgorithm}with${privateKey.algorithm}"

        Signature.getInstance(algorithm).run {
            initSign(privateKey) // Initialize this object for signing.
            update(this@signData) // Updates the data to be signed or verified, using the specified array of bytes
            sign() // Returns the signature bytes of all the data updated
        }
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException(e)
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
