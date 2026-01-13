/*
 *   Copyright © 2020-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.instant.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.examples.kotlin.instant.api.InstantExampleDocumentDescriptor
import com.pspdfkit.catalog.examples.kotlin.instant.api.WebPreviewClient
import com.pspdfkit.catalog.ui.BasicAuthDialog
import com.pspdfkit.catalog.ui.EnterDocumentLinkBottomSheet
import com.pspdfkit.catalog.ui.theming.CatalogTheme
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
open class InstantExampleConnectionActivity : AppCompatActivity() {
    /** Configuration that will be passed to created [InstantExampleActivity].  */
    private lateinit var configuration: PdfActivityConfiguration

    /** Client for connecting to Nutrient web example client.  */
    private val apiClient = WebPreviewClient()

    /** Disposable for the web preview server connections.  */
    private var connectionDisposable: Disposable? = null

    private lateinit var scanQrLauncher: ActivityResultLauncher<Intent>

    // Store current UI state for use in activity result callbacks
    private var currentUseCompose = false
    private var currentEnableAssistant = false

    // Basic auth dialog state
    private var showBasicAuthDialog = mutableStateOf(false)
    private var basicAuthEmitter: CompletableEmitter? = null

    // Enter document link bottom sheet state
    private var showEnterLinkBottomSheet = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Extract PdfActivity configuration from extras.
        @Suppress("DEPRECATION")
        configuration = checkNotNull(intent.getParcelableExtra(CONFIGURATION_ARG)) {
            "InstantExampleConnectionActivity was not initialized with proper arguments: Missing configuration extra!"
        }

        // Register activity result launchers
        scanQrLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val url = result.data?.extras?.getString(BarcodeActivity.BARCODE_ENCODED_KEY)
                if (url != null) {
                    editDocument(url, currentUseCompose, currentEnableAssistant)
                }
            }
        }

        setContent {
            CatalogTheme {
                InstantConnectionScreen(
                    onCreateNewDocument = { useCompose, enableAssistant ->
                        createNewDocument(useCompose, enableAssistant)
                    },
                    onScanQrCode = {
                        scanQrLauncher.launch(Intent(this, BarcodeActivity::class.java))
                    },
                    onEnterLinkManually = {
                        showEnterLinkBottomSheet.value = true
                    },
                    onSwitchStateChanged = { useCompose, enableAssistant ->
                        currentUseCompose = useCompose
                        currentEnableAssistant = enableAssistant
                    }
                )

                // Basic auth dialog
                BasicAuthDialog(
                    isVisible = showBasicAuthDialog.value,
                    onDismiss = {
                        showBasicAuthDialog.value = false
                        basicAuthEmitter?.onError(Exception("User cancelled basic auth."))
                        basicAuthEmitter = null
                    },
                    onConfirm = { username, password ->
                        showBasicAuthDialog.value = false
                        apiClient.setBasicAuthCredentials(username, password)
                        basicAuthEmitter?.onComplete()
                        basicAuthEmitter = null
                    }
                )

                // Enter document link bottom sheet
                EnterDocumentLinkBottomSheet(
                    isVisible = showEnterLinkBottomSheet.value,
                    onDismiss = {
                        showEnterLinkBottomSheet.value = false
                    },
                    onConfirm = { documentLink ->
                        showEnterLinkBottomSheet.value = false
                        editDocument(documentLink, currentUseCompose, currentEnableAssistant)
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        connectionDisposable?.dispose()
        connectionDisposable = null
    }

    private fun createNewDocument(useCompose: Boolean, enableAssistant: Boolean) {
        connectionDisposable?.dispose()
        connectionDisposable = apiClient.createNewDocument()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ documentDescriptor ->
                showInstantDocument(documentDescriptor, useCompose, enableAssistant)
            }, { throwable: Throwable? ->
                if (throwable != null) {
                    handleError(throwable)
                }
            })
    }

    private fun editDocument(url: String, useCompose: Boolean = false, enableAssistant: Boolean = false) {
        connectionDisposable?.dispose()
        connectionDisposable = apiClient.getDocument(url)
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorResumeNext { throwable -> handleHttpException(throwable, url) }
            .subscribe({ documentDescriptor: InstantExampleDocumentDescriptor ->
                showInstantDocument(documentDescriptor, useCompose, enableAssistant)
            }, { throwable: Throwable? ->
                if (throwable != null) {
                    handleError(throwable)
                }
            })
    }

    private fun handleHttpException(
        exception: Throwable,
        url: String
    ): Single<InstantExampleDocumentDescriptor> {
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
    private fun performBasicAuth(): Completable {
        return Completable.create { emitter: CompletableEmitter ->
            basicAuthEmitter = emitter
            showBasicAuthDialog.value = true
        }
    }

    private fun showInstantDocument(descriptor: InstantExampleDocumentDescriptor, useCompose: Boolean, enableAssistant: Boolean) {
        // Clear the Instant client cache first.
        InstantClient.create(this, descriptor.serverUrl)
            .removeLocalStorage()

        val intent = createIntentForInstantDocumentActivity(descriptor, useCompose, enableAssistant)

        // Put the Instant document descriptor to extras.
        intent.putExtra(InstantExampleExtras.DOCUMENT_DESCRIPTOR, descriptor)

        // Finally start the InstantExampleActivity and finish the current activity.
        startActivity(intent)

        finish()
    }

    protected open fun createIntentForInstantDocumentActivity(
        descriptor: InstantExampleDocumentDescriptor,
        useCompose: Boolean,
        enableAssistant: Boolean
    ): Intent {
        val updatedConfiguration = if (enableAssistant) {
            configuration.copy(configuration = configuration.configuration.copy(isAiAssistantEnabled = true))
        } else {
            configuration
        }

        val intent = if (useCompose) {
            Intent(this, InstantComposeExampleActivity::class.java).apply {
                putExtra(InstantComposeExampleActivity.DOCUMENT_DESCRIPTOR, descriptor)
                putExtra(InstantComposeExampleActivity.CONFIGURATION, updatedConfiguration)
            }
        } else {
            InstantPdfActivityIntentBuilder.fromInstantDocument(this, descriptor.serverUrl, descriptor.jwt)
                .configuration(updatedConfiguration)
                .activityClass(InstantExampleActivity::class.java)
                .build()
        }

        return intent
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
            this,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstantConnectionScreen(
    onCreateNewDocument: (useCompose: Boolean, enableAssistant: Boolean) -> Unit,
    onScanQrCode: () -> Unit,
    onEnterLinkManually: () -> Unit,
    onSwitchStateChanged: (useCompose: Boolean, enableAssistant: Boolean) -> Unit
) {
    var useCompose by remember { mutableStateOf(false) }
    var enableAiAssistant by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrient Instant") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Description text
            Text(
                text = stringResource(R.string.instant_description),
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            // Use Compose switch
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_jetpack_compose),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.useComposeForInstantExample),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = useCompose,
                        onCheckedChange = {
                            useCompose = it
                            onSwitchStateChanged(it, enableAiAssistant)
                        }
                    )
                }

                // Enable AI Assistant switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ai_assistant),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.enableAiAssistantForInstantExample),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = enableAiAssistant,
                        onCheckedChange = {
                            enableAiAssistant = it
                            onSwitchStateChanged(useCompose, it)
                        }
                    )
                }

                // Info message when enableAiAssistant is enabled
                if (enableAiAssistant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Note: On Instant, this feature requires a running Nutrient AI Assistant server. Please ensure the server is accessible before proceeding.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // Create New Document button
            Button(
                onClick = {
                    isLoading = true
                    onCreateNewDocument(useCompose, enableAiAssistant)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.instant_create_new_document))
                }
            }

            Text(
                text = stringResource(R.string.instant_create_new_document_description),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scan QR Code button
            Button(
                onClick = onScanQrCode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.instant_scan_qr_code))
            }

            Text(
                text = stringResource(R.string.instant_edit_existing_document_description),
                style = MaterialTheme.typography.bodySmall
            )

            // Enter Link Manually button
            Button(
                onClick = onEnterLinkManually,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.instant_enter_link_manually))
            }
        }
    }
}
