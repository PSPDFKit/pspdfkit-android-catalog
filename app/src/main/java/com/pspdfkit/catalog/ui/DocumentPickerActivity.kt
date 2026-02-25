/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pspdfkit.Nutrient
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.utils.Utils
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.ImageDocumentLoader
import com.pspdfkit.document.ImageDocumentUtils
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.utils.getSupportParcelableExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base activity that shows a dialog letting users choose between
 * using a default document or picking their own file.
 *
 * This is a Compose-based implementation of [DocumentPickerActivity].
 */
abstract class DocumentPickerActivity : AppCompatActivity() {

    /** Subclasses must specify which PdfActivity to launch */
    abstract val targetActivityClass: Class<out PdfActivity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configuration = intent.getSupportParcelableExtra(
            EXTRA_CONFIGURATION,
            PdfActivityConfiguration::class.java
        ) ?: throw IllegalStateException("Activity was started without a PdfActivityConfiguration.")

        setContent {
            DocumentPickerScreen(
                configuration = configuration,
                targetActivityClass = targetActivityClass,
                onFinish = { finish() }
            )
        }
    }

    companion object {
        const val EXTRA_CONFIGURATION = "DocumentPickerActivity.configuration"
    }
}

@Composable
private fun DocumentPickerScreen(
    configuration: PdfActivityConfiguration,
    targetActivityClass: Class<out PdfActivity>,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // For Android 8-10 runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        showDialog = true
    }

    // For Android 11+ settings intent
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        showDialog = true
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            onFinish()
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            val isOpenable = withContext(Dispatchers.IO) {
                Nutrient.isOpenableUri(context, uri)
            }
            if (isOpenable) {
                val isImageFile = withContext(Dispatchers.IO) {
                    ImageDocumentUtils.isImageUri(context, uri)
                }
                launchTargetActivity(context, uri, configuration, targetActivityClass, isImageFile)
            }
            onFinish()
        }
    }

    // Check permissions on first composition
    LaunchedEffect(Unit) {
        if (Utils.hasExternalStorageRwPermission(context)) {
            showDialog = true
        } else {
            showPermissionDialog = true
        }
    }

    // Permission explanation dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Storage Permission") },
            text = { Text(stringResource(R.string.externalDocumentExamplePermissionExplanation)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    Utils.requestExternalStorageRwPermission(permissionLauncher, settingsLauncher, context)
                }) {
                    Text(stringResource(R.string.grantAccess))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    showDialog = true
                }) {
                    Text(stringResource(R.string.continueWithout))
                }
            }
        )
    }

    // Document source dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onFinish,
            title = { Text("Select Document Source") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    openDefaultDocument(context, configuration, targetActivityClass, onFinish)
                }) {
                    Text("Use Default Document")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    documentPicker.launch(arrayOf("application/pdf", "image/*"))
                }) {
                    Text("Pick from Device")
                }
            }
        )
    }
}

private fun openDefaultDocument(
    context: Context,
    configuration: PdfActivityConfiguration,
    targetActivityClass: Class<out PdfActivity>,
    onFinish: () -> Unit
) {
    ExtractAssetTask.extract(WELCOME_DOC, "Document", context) { documentFile ->
        launchTargetActivity(context, Uri.fromFile(documentFile), configuration, targetActivityClass)
        onFinish()
    }
}

private fun launchTargetActivity(
    context: Context,
    uri: Uri,
    configuration: PdfActivityConfiguration,
    targetActivityClass: Class<out PdfActivity>
) {
    val isImageFile = ImageDocumentUtils.isImageUri(context, uri)
    launchTargetActivity(context, uri, configuration, targetActivityClass, isImageFile)
}

private fun launchTargetActivity(
    context: Context,
    uri: Uri,
    configuration: PdfActivityConfiguration,
    targetActivityClass: Class<out PdfActivity>,
    isImageFile: Boolean
) {
    val (intentBuilder, finalConfiguration) = if (isImageFile) {
        PdfActivityIntentBuilder.fromImageUri(context, uri) to
            ImageDocumentLoader.getDefaultImageDocumentActivityConfiguration(configuration)
    } else {
        PdfActivityIntentBuilder.fromUri(context, uri) to configuration
    }

    val intent = intentBuilder
        .configuration(finalConfiguration)
        .activityClass(targetActivityClass)
        .build()
    context.startActivity(intent)
}
