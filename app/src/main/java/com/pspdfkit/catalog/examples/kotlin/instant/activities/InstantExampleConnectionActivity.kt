/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

@file:Suppress("DEPRECATION")

package com.pspdfkit.catalog.examples.kotlin.instant.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.examples.kotlin.instant.api.InstantExampleDocumentDescriptor
import com.pspdfkit.catalog.examples.kotlin.instant.api.WebPreviewClient
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.instant.client.InstantClient
import com.pspdfkit.instant.ui.InstantPdfActivity
import com.pspdfkit.instant.ui.InstantPdfActivityIntentBuilder
import com.pspdfkit.utils.PdfLog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import retrofit2.HttpException
import java.net.UnknownHostException

/**
 * Allows to connect to the example Instant Server (Nutrient Document Engine).
 */
class InstantExampleConnectionActivity : AppCompatActivity() {
    /** Configuration that will be passed to created [InstantExampleActivity].  */
    private var configuration: PdfActivityConfiguration? = null

    /** Client for connecting to PSPDFKit web example client.  */
    private val apiClient = WebPreviewClient()

    /** Disposable for the web preview server connections.  */
    private var connectionDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_try_instant_connect)

        // Extract PdfActivity configuration from extras.
        configuration = intent.getParcelableExtra(CONFIGURATION_ARG)
        checkNotNull(configuration) { "InstantExampleConnectionActivity was not initialized with proper arguments: Missing configuration extra!" }

        // Configure toolbar.
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configure new document button.
        findViewById<View>(R.id.button_new_document).setOnClickListener { createNewDocument() }

        // Configure scan document link button.
        val scanQrCodeButton = findViewById<Button>(R.id.scan_qr_code)
        scanQrCodeButton.setOnClickListener {
            startActivityForResult(
                Intent(this, BarcodeActivity::class.java),
                BarcodeActivity.BARCODE_RESULT_REQUEST_CODE
            )
        }

        // Configure enter document link button.
        val enterLinkButton = findViewById<Button>(R.id.enter_link_manually)
        enterLinkButton.setOnClickListener {
            startActivityForResult(
                Intent(this, EnterDocumentLinkActivity::class.java),
                EnterDocumentLinkActivity.DOCUMENT_LINK_RESULT_REQUEST_CODE
            )
        }
    }

    override fun onStop() {
        super.onStop()
        connectionDisposable?.dispose()
        connectionDisposable = null
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == BarcodeActivity.BARCODE_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val url = data?.extras?.getString(BarcodeActivity.BARCODE_ENCODED_KEY)
            if (url != null) {
                editDocument(url)
            }
        } else if (requestCode == EnterDocumentLinkActivity.DOCUMENT_LINK_RESULT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val url = data?.extras?.getString(EnterDocumentLinkActivity.DOCUMENT_LINK_ENCODED_KEY)
            if (url != null) {
                editDocument(url)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun createNewDocument() {
        val progressDialog = ProgressDialog.show(this, null, getString(R.string.instant_creating), true, false)
        connectionDisposable?.dispose()
        connectionDisposable = apiClient.createNewDocument()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { progressDialog.dismiss() }
            .subscribe({ documentDescriptor ->
                showInstantDocument(documentDescriptor)
            }, { throwable: Throwable? ->
                if (throwable != null) {
                    handleError(throwable)
                }
            })
    }

    private fun editDocument(url: String) {
        val progressDialog = ProgressDialog.show(this, null, getString(R.string.instant_connecting), true, false)
        connectionDisposable?.dispose()
        connectionDisposable = apiClient.getDocument(url)
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorResumeNext { throwable -> handleHttpException(throwable, url) }
            .doOnError { progressDialog.dismiss() }
            .subscribe({ documentDescriptor: InstantExampleDocumentDescriptor ->
                progressDialog.dismiss()
                showInstantDocument(documentDescriptor)
            }, { throwable: Throwable? ->
                if (throwable != null) {
                    handleError(throwable)
                }
            })
    }

    private fun handleHttpException(exception: Throwable, url: String): Single<InstantExampleDocumentDescriptor> {
        if (exception is HttpException) {
            if (exception.code() == 401) {
                // We need a basic auth request here.
                // Then we'll try the request again.
                return performBasicAuth()
                    .andThen(apiClient.getDocument(url))
                    .observeOn(AndroidSchedulers.mainThread())
            }
        }
        return Single.error(exception)
    }

    /** Asks the user for basic auth credentials and sets them on the apiClient.  */
    @SuppressLint("InflateParams")
    private fun performBasicAuth(): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            val basicAuthView = LayoutInflater.from(this).inflate(R.layout.dialog_basic_auth, null, false)
            val username = basicAuthView.findViewById<EditText>(R.id.username)
            val password = basicAuthView.findViewById<EditText>(R.id.password)
            AlertDialog.Builder(this)
                .setTitle(R.string.instant_authentication_required)
                .setView(basicAuthView)
                .setPositiveButton(R.string.instant_login) { _, _ ->
                    apiClient.setBasicAuthCredentials(username.text.toString(), password.text.toString())
                    emitter.onComplete()
                }
                .setOnCancelListener { emitter.onError(Exception("User cancelled basic auth.")) }
                .show()
        }
    }

    private fun showInstantDocument(descriptor: InstantExampleDocumentDescriptor) {
        // Clear the Instant client cache first.
        InstantClient.create(this@InstantExampleConnectionActivity, descriptor.serverUrl).removeLocalStorage()

        // Build the activity intent.
        val builder = InstantPdfActivityIntentBuilder.fromInstantDocument(
            this@InstantExampleConnectionActivity,
            descriptor.serverUrl,
            descriptor.jwt
        )
        val intent = builder
            .configuration(configuration)
            .activityClass(InstantExampleActivity::class.java)
            .build()

        // Put the Instant document descriptor to extras.
        intent.putExtra(InstantExampleActivity.DOCUMENT_DESCRIPTOR, descriptor)

        // Finally start the InstantExampleActivity and finish the current activity.
        startActivity(intent)
        finish()
    }

    private fun handleError(throwable: Throwable) {
        var errorText = R.string.instant_error_something_went_wrong
        if (throwable is HttpException) {
            val httpCode: Int = throwable.code()
            if (httpCode in 400..499) {
                errorText = R.string.instant_error_invalid_id
            } else if (httpCode >= 500) {
                errorText = R.string.instant_error_server_error
            }
        } else if (throwable is UnknownHostException) {
            errorText = R.string.instant_error_no_connection
        }
        Toast.makeText(
            this@InstantExampleConnectionActivity,
            getString(R.string.instant_error_connection_failed, getString(errorText)),
            Toast.LENGTH_LONG
        ).show()
        PdfLog.e("InstantExample", "Error loading document with Instant: $throwable")
    }

    companion object {
        /** Name of the extra in activity intent holding [PdfActivityConfiguration] that should be passed to created [InstantPdfActivity].  */
        const val CONFIGURATION_ARG = "InstantExampleConnectionActivity.PSPDFKitConfiguration"
    }
}
