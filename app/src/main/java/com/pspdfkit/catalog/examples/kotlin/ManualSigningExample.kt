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
import com.pspdfkit.signatures.DigitalSignatureMetadata
import com.pspdfkit.signatures.SignatureAppearance
import com.pspdfkit.signatures.SignatureAppearance.SignatureAppearanceMode
import com.pspdfkit.signatures.SignerOptions
import com.pspdfkit.signatures.SigningManager
import com.pspdfkit.signatures.getPrivateKeyEntryFromP12Stream
import com.pspdfkit.signatures.getX509Certificates
import com.pspdfkit.signatures.timestamp.TimestampData
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.utils.PdfLog
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.Signature

/**
 * An example showing how to leverage customSigning functionality in [SigningManager] to digitally sign document manually.
 * */
class ManualSigningExample(context: Context) : SdkExample(context, R.string.manualSigningExampleTitle, R.string.manualSigningExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val assetName = "Form_example.pdf"

        val unsignedDocument = PdfDocumentLoader.openDocument(context, DocumentSource(AssetDataProvider(assetName)))
        val keyEntryWithCertificates = getPrivateKeyEntry(context)
        val privateKey = keyEntryWithCertificates.privateKey
        val certificates = keyEntryWithCertificates.getX509Certificates()
        val signatureFormFields = unsignedDocument.documentSignatureInfo.signatureFormFields
        val outputFile = File(context.filesDir, "signedDocument.pdf")
        outputFile.delete() // make sure output is deleted from previous runs.

        /** Configure the appearance of the signature using [SignatureAppearance] **/
        val appearance = SignatureAppearance(
            signatureAppearanceMode = SignatureAppearanceMode.SIGNATURE_ONLY
        )
        val metadata = DigitalSignatureMetadata(
            signatureAppearance = appearance,
            timestampData = TimestampData("https://freetsa.org/tsr")
        )

        /** [SignerOptions] contains all the required configuration for [SigningManager]*/
        val signerOptions = SignerOptions.Builder(signatureFormFields[0], Uri.fromFile(outputFile))
            .setCertificates(certificates)
            .setSignatureMetadata(metadata)
            .setType(digitalSignatureType)
            .build()

        /** [SignerOptions] contains all the required configuration for [SigningManager]*/
        SigningManager.signDocument(
            context = context,
            signerOptions = signerOptions,
            customSigning = { data, hashAlgorithm ->
                /** Here we are manually signing ByteArray with provided hashAlgorithm and private key, this is a mandatory step if
                 * customer doesn't provide private key in [SignerOptions] */
                data.signData(privateKey, hashAlgorithm)
            },
            onFailure = { e ->
                Toast.makeText(context, "Error launching example. See logcat for details.", Toast.LENGTH_SHORT).show()
                PdfLog.e("ManualSigningExample", e, "Error while launching example.")
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
