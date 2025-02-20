/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask.extract
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.forms.FormType
import com.pspdfkit.forms.SignatureFormElement
import com.pspdfkit.signatures.DigitalSignatureMetadata
import com.pspdfkit.signatures.Signature
import com.pspdfkit.signatures.SignatureAppearance
import com.pspdfkit.signatures.SignatureGraphic
import com.pspdfkit.signatures.SignerOptions
import com.pspdfkit.signatures.SigningManager
import com.pspdfkit.signatures.getPrivateKeyEntryFromP12Stream
import com.pspdfkit.signatures.listeners.OnSignaturePickedListener
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.signatures.ElectronicSignatureFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore

/**
 * Showcases how to combine a created signature annotation using the [ElectronicSignatureFragment] with digitally signing
 * the document using a certificate.
 *
 * See guide: https://nutrient.io/guides/android/signatures/using-electronic-signatures-and-digital-signatures-together/
 */
class CombineElectronicSignaturesWithDigitalSigningExample(context: Context) :
    SdkExample(
        context,
        R.string.CombineElectronicSignaturesWithDigitalSigningExampleTitle,
        R.string.CombineElectronicSignaturesWithDigitalSigningExampleDescription
    ) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // The form field for signing is on page with index 16.
        configuration.page(16)

        // Extract the document from the assets.
        extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(CombineElectronicSignaturesWithDigitalSigningActivity::class)
                .build()

            // Start the CombineElectronicSignaturesWithDigitalSigningActivity showing the demo document.
            context.startActivity(intent)
        }
    }
}

/**
 * Shows how to implement a signature picked listener and document signing listener in order to create a signature and sign with the
 * created signature render.
 */
class CombineElectronicSignaturesWithDigitalSigningActivity :
    PdfActivity(),
    OnSignaturePickedListener {

    /** Name of the previously clicked signature form field (if any). Used to access it after a configuration change.  */
    private var signatureFormFieldName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // In case this activity is being recreated (e.g. during a configuration change), reattach
        // the activity as listener to the existing dialog. Calling restore() is safe, and won't do
        // anything in case the dialog isn't currently shown.
        ElectronicSignatureFragment.restore(supportFragmentManager, this)

        // Also restore information about any previously clicked signature for element. We'll use
        // this when adding an ink annotation later.
        if (savedInstanceState != null) {
            signatureFormFieldName = savedInstanceState.getString(STATE_FORM_FIELD_NAME, null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Make sure to persist information of any clicked signature form element (so it outlives configuration changes).
        outState.putString(STATE_FORM_FIELD_NAME, signatureFormFieldName)
    }

    override fun onStart() {
        super.onStart()

        // We need to keep track of the form field name so we can retrieve it when digitally signing after the signature has been created
        // in the Electronic Signature Dialog. The dialog is opened by Nutrient automatically when the signature form element is clicked.
        // This behaviour can also be overridden here if required.
        requirePdfFragment().addOnFormElementClickedListener { formElement ->
            when (formElement.type) {
                FormType.SIGNATURE -> {
                    onSignatureFormElementClicked(formElement as SignatureFormElement)
                    // By returning true, you intercept the event and prevent Nutrient from showing the signature picker itself.
                    true
                }
                // This click event is not interesting for us. Return false to let Nutrient handle this event.
                else -> false
            }
        }
    }

    /**
     * This callback handles showing the signature picker whenever a signature form element is clicked by the user.
     */
    private fun onSignatureFormElementClicked(formElement: SignatureFormElement) {
        // Keep reference of the stored signature form element so we can later on access it.
        signatureFormFieldName = formElement.formField.name
        // We need to control the showing of the signature dialog in order to attach this as a listener.
        ElectronicSignatureFragment.show(supportFragmentManager, this)
    }

    /**
     * We are the signature creation listener, so this method is called by the signature picker,
     * whenever the user selects/creates a signature.
     */
    override fun onSignaturePicked(signature: Signature) {
        // You can add you custom signature handling logic here.
        // ...
        digitallySignWithSignatureAnnotation(signature)
    }

    /**
     * This method is called by the signature picker, if the user dismissed the picker without selecting a signature.
     */
    override fun onDismiss() {
        signatureFormFieldName = null
    }

    /**
     * This is an example that shows how to use the created signature annotation as the digital signature stamp appearance.
     * Note we're only ignoring the return of getFormFieldWithFullyQualifiedNameAsync for the sake of the example.
     */
    @SuppressLint("CheckResult")
    private fun digitallySignWithSignatureAnnotation(signature: Signature) {
        val document = document ?: return
        val signatureFormFieldName = this.signatureFormFieldName ?: return

        // Retrieve the previously clicked signature form element. We do this asynchronously to not block the UI thread.
        document.formProvider.getFormFieldWithFullyQualifiedNameAsync(signatureFormFieldName)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { formField ->
                val clickedSignatureFormElement = formField.formElement as SignatureFormElement

                // We want to place the ink annotation on top of the signature field. We retrieve the widget annotation to access its position.
                val formFieldAnnotation = clickedSignatureFormElement.annotation

                // The signature object provides convenient conversion to ink or stamp annotation.
                val signatureAnnotation = signature.toAnnotation(document, formFieldAnnotation.pageIndex, formFieldAnnotation.boundingBox)

                // Add the annotation to the document. This step is required so we can render the signature
                // in order to pass the render to the `SignatureAppearance` when digitally signing in the
                // next step. We remove it once we have the render.
                // Here, we use the synchronous `AnnotationProvider` method as the next steps need to wait for this to complete.
                document.annotationProvider.addAnnotationToPage(signatureAnnotation)

                val w = kotlin.math.abs(signatureAnnotation.boundingBox.width().toInt())
                val h = kotlin.math.abs(signatureAnnotation.boundingBox.height().toInt())
                val signatureBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                signatureAnnotation.renderToBitmap(signatureBitmap)

                // Now we've made the bitmap, we can remove the annotation from the document, as it will appear on the digital
                // signature when we digitally sign the document.
                document.annotationProvider.removeAnnotationFromPage(signatureAnnotation)

                // Now sign the document using the certificate added in the example class in `addCertificateAndSigner`.
                signDocumentWithSignatureBitmap(clickedSignatureFormElement, signatureBitmap)
            }
    }

    /**
     * Takes the created signature bitmap and digitally signs the document using the bitmap and the signing certificate initialised in
     * [CombineElectronicSignaturesWithDigitalSigningExample].
     */
    private fun signDocumentWithSignatureBitmap(formElement: SignatureFormElement, signatureBitmap: Bitmap) {
        val outputFile = File(filesDir, "signedDocument.pdf")

        val signatureAppearance = SignatureAppearance(
            showWatermark = false,
            signatureGraphic = SignatureGraphic.fromBitmap(getImageUri(signatureBitmap))
        )

        val key = getPrivateKeyEntry(this)
        val signedDocumentUri = Uri.fromFile(outputFile)
        val signerOptions = SignerOptions.Builder(formElement.formField, signedDocumentUri).setSignatureMetadata(
            DigitalSignatureMetadata(signatureAppearance = signatureAppearance)
        ).setPrivateKey(key).build()

        // handles the signing process
        SigningManager.signDocument(
            context = this,
            signerOptions = signerOptions,
            onFailure = { t ->
                Toast.makeText(this, t.localizedMessage, Toast.LENGTH_LONG).show()
            }
        ) {
            setDocumentFromUri(signedDocumentUri, null)
            // Signature page.
            pageIndex = 16
        }
    }

    /**
     * Stores the signature image to the FS in order to obtain a URI to pass to the [SignatureAppearance].
     */
    private fun getImageUri(bitmap: Bitmap): Uri {
        val imageFile = File(cacheDir, "signature.png")
        val fileStream = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileStream)
        fileStream.close()
        return Uri.fromFile(imageFile)
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    private fun getPrivateKeyEntry(context: Context): KeyStore.PrivateKeyEntry {
        // Inside a p12 we have both the certificate (or certificate chain to the root CA) and private key used for signing.
        val keystoreFile = context.assets.open("digital-signatures/ExampleSigner.p12")
        return getPrivateKeyEntryFromP12Stream(keystoreFile, "test")
    }
}

private const val STATE_FORM_FIELD_NAME = "Example.FORM_FIELD_NAME"
