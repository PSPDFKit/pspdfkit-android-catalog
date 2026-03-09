/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
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
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.examples.kotlin.instant.api.ExampleServerClient
import com.pspdfkit.catalog.examples.kotlin.instant.api.ExampleServerDocument
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
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Entry screen for connecting to Instant examples.
 *
 * This flow is designed to work with the Nutrient example server implementation:
 * https://github.com/PSPDFKit/pspdfkit-server-example-nodejs
 *
 * Flow:
 * 1) Try web preview (if the URL is a web example link that returns a descriptor/JWT).
 * 2) Fall back to the example server API (requires username via Basic auth).
 */
open class InstantExampleConnectionActivity : AppCompatActivity() {
    /** Configuration that will be passed to created [InstantExampleActivity].  */
    private lateinit var configuration: PdfActivityConfiguration

    /** Client for connecting to Nutrient web example client.  */
    private val apiClient = WebPreviewClient()

    /** Job for the web preview server connections.  */
    private var connectionJob: Job? = null

    private lateinit var scanQrLauncher: ActivityResultLauncher<Intent>

    // Store current UI state for use in activity result callbacks
    private var currentUseCompose = false
    private var currentEnableAssistant = false

    // Basic auth dialog state
    private var showBasicAuthDialog = mutableStateOf(false)
    private var basicAuthContinuation: CancellableContinuation<Unit>? = null

    // Enter document link bottom sheet state
    private var showEnterLinkBottomSheet = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Extract PdfActivity configuration from extras.
        @Suppress("DEPRECATION")
        configuration =
            checkNotNull(intent.getParcelableExtra(CONFIGURATION_ARG)) {
                "InstantExampleConnectionActivity was not initialized with proper arguments: Missing configuration extra!"
            }

        // Register activity result launchers
        scanQrLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val url = result.data?.extras?.getString(BarcodeActivity.BARCODE_ENCODED_KEY)
                    if (url != null) {
                        editDocument(url, "", currentUseCompose, currentEnableAssistant)
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
                    },
                )

                // Basic auth dialog
                BasicAuthDialog(
                    isVisible = showBasicAuthDialog.value,
                    onDismiss = {
                        showBasicAuthDialog.value = false
                        basicAuthContinuation
                            ?.takeIf { it.isActive }
                            ?.resumeWithException(Exception("User cancelled basic auth."))
                        basicAuthContinuation = null
                    },
                    onConfirm = { username, password ->
                        showBasicAuthDialog.value = false
                        apiClient.setBasicAuthCredentials(username, password)
                        basicAuthContinuation?.takeIf { it.isActive }?.resume(Unit)
                        basicAuthContinuation = null
                    },
                )

                // Enter document link bottom sheet
                EnterDocumentLinkBottomSheet(
                    isVisible = showEnterLinkBottomSheet.value,
                    onDismiss = {
                        showEnterLinkBottomSheet.value = false
                    },
                    onConfirm = { documentLink, username ->
                        showEnterLinkBottomSheet.value = false
                        editDocument(documentLink, username, currentUseCompose, currentEnableAssistant)
                    },
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        connectionJob?.cancel()
        connectionJob = null
    }

    private fun createNewDocument(useCompose: Boolean, enableAssistant: Boolean) {
        connectionJob?.cancel()
        connectionJob =
            lifecycleScope.launch {
                try {
                    val documentDescriptor = withContext(Dispatchers.IO) { apiClient.createNewDocument() }
                    showInstantDocument(documentDescriptor, useCompose, enableAssistant)
                } catch (throwable: Throwable) {
                    handleError(throwable)
                }
            }
    }

    private fun editDocument(url: String, username: String, useCompose: Boolean = false, enableAssistant: Boolean = false) {
        connectionJob?.cancel()
        connectionJob =
            lifecycleScope.launch {
                try {
                    val trimmedUrl = url.trim()
                    val webPreviewDescriptor =
                        runCatching {
                            loadDocumentDescriptor(trimmedUrl)
                        }.getOrNull()
                    if (webPreviewDescriptor != null) {
                        showInstantDocument(webPreviewDescriptor, useCompose, enableAssistant)
                        return@launch
                    }

                    // Example servers require a username so we can call /api/document/:id or /api/documents.
                    if (username.isNotBlank()) {
                        val exampleDescriptor = loadFromExampleServer(trimmedUrl, username.trim())
                        if (exampleDescriptor != null) {
                            showInstantDocument(exampleDescriptor, useCompose, enableAssistant)
                            return@launch
                        }
                    }

                    Toast
                        .makeText(
                            this@InstantExampleConnectionActivity,
                            R.string.instant_error_missing_jwt,
                            Toast.LENGTH_LONG,
                        ).show()
                } catch (throwable: Throwable) {
                    handleError(throwable)
                }
            }
    }

    private suspend fun loadDocumentDescriptor(url: String): InstantExampleDocumentDescriptor = try {
        withContext(Dispatchers.IO) { apiClient.getDocument(url) }
    } catch (exception: Throwable) {
        handleHttpException(exception, url)
    }

    private suspend fun handleHttpException(exception: Throwable, url: String): InstantExampleDocumentDescriptor {
        if (exception is HttpException) {
            if (exception.code() == 401) {
                // We need a basic auth request here.
                // Then we'll try the request again.
                performBasicAuth()
                return withContext(Dispatchers.IO) { apiClient.getDocument(url) }
            }
        }
        throw exception
    }

    private fun extractServerUrl(url: String): String {
        val uri = url.toUri()
        val scheme = uri.scheme
        val authority = uri.encodedAuthority
        return if (!scheme.isNullOrBlank() && !authority.isNullOrBlank()) {
            "$scheme://$authority"
        } else {
            url
        }
    }

    /**
     * Loads an Instant descriptor from the example server API using Basic auth.
     *
     * If the URL contains a document id, we call `/api/document/:id`. Otherwise we list
     * documents and let the user pick one.
     */
    private suspend fun loadFromExampleServer(url: String, username: String): InstantExampleDocumentDescriptor? {
        val serverUrl = extractServerUrl(url).trimEnd('/')
        val documentId = extractExampleServerDocumentId(url)
        val client = ExampleServerClient(serverUrl, username)

        return if (documentId != null) {
            val token = withContext(Dispatchers.IO) { client.getJwt(documentId) }
            val webUrl = "$serverUrl/documents/$documentId"
            InstantExampleDocumentDescriptor(
                serverUrl = mapToInstantServerUrl(serverUrl),
                documentId = documentId,
                jwt = token,
                documentCode = "",
                webUrl = webUrl,
            )
        } else {
            val documents = withContext(Dispatchers.IO) { client.getDocuments() }
            if (documents.isEmpty()) {
                Toast.makeText(this, R.string.instant_error_invalid_id, Toast.LENGTH_LONG).show()
                null
            } else {
                val selected =
                    if (documents.size == 1) {
                        documents.first()
                    } else {
                        selectDocument(documents)
                    } ?: return null
                val token = selected.tokens.firstOrNull().orEmpty()
                if (token.isBlank()) return null
                val webUrl = "$serverUrl/documents/${selected.id}"
                InstantExampleDocumentDescriptor(
                    serverUrl = mapToInstantServerUrl(serverUrl),
                    documentId = selected.id,
                    jwt = token,
                    documentCode = "",
                    webUrl = webUrl,
                )
            }
        }
    }

    private fun extractExampleServerDocumentId(url: String): String? {
        val uri = url.toUri()
        val segments = uri.pathSegments
        // Supports the short link `/d/:id` used by the Node example server.
        val shortLinkIndex = segments.indexOf("d")
        if (shortLinkIndex >= 0 && segments.size > shortLinkIndex + 1) {
            return segments[shortLinkIndex + 1]
        }
        val documentsIndex = segments.indexOf("documents")
        if (documentsIndex >= 0 && segments.size > documentsIndex + 1) {
            return segments[documentsIndex + 1]
        }
        val apiIndex = segments.indexOf("document")
        if (apiIndex >= 0 && segments.size > apiIndex + 1) {
            return segments[apiIndex + 1]
        }
        return null
    }

    /**
     * Maps example server URLs to the Document Engine port used by the example docker setup.
     */
    private fun mapToInstantServerUrl(serverUrl: String): String {
        val parsed = serverUrl.toHttpUrlOrNull() ?: return serverUrl
        val port = parsed.port
        val targetPort =
            if (port == ExampleServerClient.WEB_EXAMPLE_SERVER_PORT) {
                ExampleServerClient.INSTANT_SERVER_PORT
            } else {
                port
            }
        return parsed
            .newBuilder()
            .port(targetPort)
            .build()
            .toString()
            .trimEnd('/')
    }

    /**
     * Simple chooser for `/api/documents` results so we can re-use a single input field.
     */
    private suspend fun selectDocument(documents: List<ExampleServerDocument>): ExampleServerDocument? =
        suspendCancellableCoroutine { continuation ->
            val items = documents.map { it.title.ifBlank { it.id } }.toTypedArray()
            val dialog =
                androidx.appcompat.app.AlertDialog
                    .Builder(this)
                    .setTitle(R.string.instant_select_document)
                    .setItems(items) { _, which ->
                        continuation.resume(documents[which])
                    }.setOnCancelListener {
                        continuation.resume(null)
                    }.show()
            continuation.invokeOnCancellation { dialog.dismiss() }
        }

    /** Asks the user for basic auth credentials and sets them on the apiClient.  */
    private suspend fun performBasicAuth() = suspendCancellableCoroutine { continuation ->
        basicAuthContinuation
            ?.takeIf { it.isActive }
            ?.resumeWithException(IllegalStateException("Basic auth already in progress."))
        basicAuthContinuation = continuation
        showBasicAuthDialog.value = true
        continuation.invokeOnCancellation {
            if (basicAuthContinuation === continuation) {
                basicAuthContinuation = null
            }
        }
    }

    private fun showInstantDocument(descriptor: InstantExampleDocumentDescriptor, useCompose: Boolean, enableAssistant: Boolean) {
        // Clear the Instant client cache first.
        InstantClient
            .create(this, descriptor.serverUrl)
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
        enableAssistant: Boolean,
    ): Intent {
        val updatedConfiguration =
            if (enableAssistant) {
                configuration.copy(configuration = configuration.configuration.copy(isAiAssistantEnabled = true))
            } else {
                configuration
            }

        val intent =
            if (useCompose) {
                Intent(this, InstantComposeExampleActivity::class.java).apply {
                    putExtra(InstantComposeExampleActivity.DOCUMENT_DESCRIPTOR, descriptor)
                    putExtra(InstantComposeExampleActivity.CONFIGURATION, updatedConfiguration)
                }
            } else {
                InstantPdfActivityIntentBuilder
                    .fromInstantDocument(this, descriptor.serverUrl, descriptor.jwt)
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
        Toast
            .makeText(
                this,
                getString(R.string.instant_error_connection_failed, getString(errorText)),
                Toast.LENGTH_LONG,
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
    onSwitchStateChanged: (useCompose: Boolean, enableAssistant: Boolean) -> Unit,
) {
    var useCompose by remember { mutableStateOf(false) }
    var enableAiAssistant by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrient Instant") },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Description text
            Text(
                text = stringResource(R.string.instant_description),
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()

            // Use Compose switch
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_jetpack_compose),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.useComposeForInstantExample),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        checked = useCompose,
                        onCheckedChange = {
                            useCompose = it
                            onSwitchStateChanged(it, enableAiAssistant)
                        },
                    )
                }

                // Enable AI Assistant switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ai_assistant),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.enableAiAssistantForInstantExample),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        checked = enableAiAssistant,
                        onCheckedChange = {
                            enableAiAssistant = it
                            onSwitchStateChanged(useCompose, it)
                        },
                    )
                }

                // Info message when enableAiAssistant is enabled
                if (enableAiAssistant) {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text =
                            "Note: On Instant, this feature requires a running Nutrient AI Assistant server. " +
                                "Please ensure the server is accessible before proceeding.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.instant_create_new_document))
                }
            }

            Text(
                text = stringResource(R.string.instant_create_new_document_description),
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scan QR Code button
            Button(
                onClick = onScanQrCode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.instant_scan_qr_code))
            }

            Text(
                text = stringResource(R.string.instant_edit_existing_document_description),
                style = MaterialTheme.typography.bodySmall,
            )

            // Enter Link Manually button
            Button(
                onClick = onEnterLinkManually,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.instant_enter_link_manually))
            }
        }
    }
}
