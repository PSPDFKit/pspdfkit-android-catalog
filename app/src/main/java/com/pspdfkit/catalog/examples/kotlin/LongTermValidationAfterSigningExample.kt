/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.forms.SignatureFormElement
import com.pspdfkit.signatures.SignerOptions
import com.pspdfkit.signatures.SigningManager
import com.pspdfkit.signatures.TrustedKeyStore
import com.pspdfkit.signatures.loadCertificateFromStream
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.utils.PdfLog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.security.cert.X509Certificate

/**
 * An example that shows how to digitally sign a PDF document using [SigningManager].
 * This is a Simple implementation where user provides Private key in [SignerOptions].
 */
class LongTermValidationAfterSigningExample(context: Context) : SdkExample(context, R.string.digitalSignatureLtvAddedExampleTitle, R.string.digitalSignatureLtvAddedExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract("SignedWithoutLtv.pdf", title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(AddLtvAfterSigningActivity::class)
                .build()

            // Start the MeasurementToolsActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}

/**
 * An implementation that provides a button to add long term validation info to a signed document.
 */
class AddLtvAfterSigningActivity : PdfActivity() {

    private var addLtvDisposable: Disposable? = null
    private fun addLtv() {
        val handleError: (Throwable) -> Unit = {
            Toast.makeText(this, "Error adding LTV! See log.", Toast.LENGTH_SHORT).show()
            PdfLog.e("AddLtvAfterSigningActivity", it.message, "Error adding LTV.")
        }
        try {
            val document = document ?: throw IllegalStateException("Document not found.")
            val signatureForm = document.formProvider.getFormElementWithName("Signature") as? SignatureFormElement ?: throw IllegalStateException("Signature form element not found.")
            val trustedCertificates = getTrustedCertificates()
            TrustedKeyStore.clearTrustedCertificates()
            TrustedKeyStore.addTrustedCertificates(trustedCertificates)

            addLtvDisposable?.dispose()
            // We can choose not to pass certificates here and the ones in the document's signature will be fetched instead.
            addLtvDisposable = document.addLongTermValidation(signatureForm, listOf())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Toast.makeText(this, "LTV data added to signature!", Toast.LENGTH_SHORT).show()
                    }
                ) { throwable ->
                    handleError(throwable)
                }
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, addLtvId, 0, "Add LTV")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == addLtvId) {
            addLtv()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
    private fun getTrustedCertificates(): List<X509Certificate> {
        return listOf(
            loadCertificateFromStream(assets.open("digital-signatures/ltv/Root.cert")),
            loadCertificateFromStream(assets.open("digital-signatures/ltv/Issuer.cert"))
        )
    }

    companion object {
        private const val addLtvId = 1
    }
}
