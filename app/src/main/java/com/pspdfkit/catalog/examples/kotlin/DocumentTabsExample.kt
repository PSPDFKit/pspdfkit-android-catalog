/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import com.pspdfkit.PSPDFKit
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.TabBarHidingMode
import com.pspdfkit.document.ImageDocumentUtils
import com.pspdfkit.document.download.DownloadJob
import com.pspdfkit.document.download.DownloadProgressFragment
import com.pspdfkit.document.download.DownloadRequest
import com.pspdfkit.ui.DocumentCoordinator
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import java.io.File

/**
 * This example shows how to add document tabs in the default [PdfActivity].
 */
class DocumentTabsExample(context: Context) : SdkExample(context, R.string.documentTabsExampleTitle, R.string.documentTabsExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Make the tab bar always visible.
        configuration.setTabBarHidingMode(TabBarHidingMode.SHOW)

        // First, extract the initial document from the app's assets and place it in the device's internal storage.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // Launch the custom example activity using the document and configuration.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(DocumentTabsActivity::class)
                .build()

            // Start the activity for the extracted document.
            context.startActivity(intent)
        }
    }
}

// We're temporarily suppressing the warning for startActivityForResult and OnActivityResult being deprecated.
// Issue: https://github.com/PSPDFKit/PSPDFKit/issues/31881
@Suppress("DEPRECATION")
class DocumentTabsActivity : PdfActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PdfActivity can handle multiple documents out of the box using its internal document
        // coordinator object. The activity also hosts a tab bar, which observes documents added to
        // the document coordinator, and will automatically display tabs for them. It also delegates
        // user interaction to the activity (e.g. closing tabs, changing the active document, etc.).

        val documentCoordinator = documentCoordinator
        if (document == null && savedInstanceState == null) {
            // For this example, we're loading multiple PDF documents from the app's assets, for
            // displaying them in the activity's tab bar. We use the `DocumentCoordinator` object
            // which is provided by the activity, to add each document as soon as it is loaded. This
            // is everything that is required to make `PdfActivity` show the documents inside the tab bar.
            for (assetName in assetFiles) {
                // Extract document from the app's assets and place it in the device's internal storage.
                ExtractAssetTask.extract(assetName, assetName, this) { documentFile ->
                    documentCoordinator.addDocument(DocumentDescriptor.fromUri(Uri.fromFile(documentFile)))
                }
            }

            // Image documents are also supported.
            ExtractAssetTask.extract("images/android.png", "images/android.png", this) { documentFile ->
                val documentDescriptor = DocumentDescriptor.imageDocumentFromUri(Uri.fromFile(documentFile))
                documentDescriptor.setTitle("Android Image Document")
                documentCoordinator.addDocument(documentDescriptor)
            }
        }

        // You can also customize the tab bar layout. For example, to insert an "Add tab" button at
        // the start of the tab bar, simply retrieve the `PdfTabBar` from the activity, and add a
        // view to it.
        pspdfKitViews.tabBar?.apply {
            val addTabButton = layoutInflater.inflate(R.layout.item_add_button, this, false) as ImageView
            addTabButton.setOnClickListener { addNewTab() }
            addView(addTabButton, 0)
        }
    }

    private fun addNewTab() {
        // Show system document picker to allow user to pick document that will be displayed in the added tab.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))
        startActivityForResult(intent, REQUEST_OPEN_DOCUMENT)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OPEN_DOCUMENT && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data ?: return
            val isImageFile = ImageDocumentUtils.isImageUri(this, uri)

            // Some URIs can be opened directly, including local filesystem, app assets, and content provider URIs.
            if (PSPDFKit.isOpenableUri(this, uri)) {
                showDocumentInNewTab(uri, isImageFile)
            } else {
                // Find the DownloadProgressFragment for showing download progress, or create a new one.
                val downloadFragment = supportFragmentManager.findFragmentByTag(
                    DOWNLOAD_PROGRESS_FRAGMENT
                ) as DownloadProgressFragment? ?: run {
                    val downloadRequest = DownloadRequest.Builder(this).uri(uri).build()
                    val job = DownloadJob.startDownload(downloadRequest)

                    val downloadFragment = DownloadProgressFragment()
                    downloadFragment.show(supportFragmentManager, DOWNLOAD_PROGRESS_FRAGMENT)
                    downloadFragment.job = job
                    downloadFragment
                }

                // Once the download is complete we show the downloaded document in a new tab.
                downloadFragment.job.setProgressListener(object : DownloadJob.ProgressListenerAdapter() {
                    override fun onComplete(output: File) {
                        showDocumentInNewTab(Uri.fromFile(output), isImageFile)
                    }
                })
            }
        }
    }

    /**
     * Adds document from Uri to the [DocumentCoordinator] and makes it visible immediately.
     */
    private fun showDocumentInNewTab(uri: Uri, isImageDocument: Boolean) {
        val documentDescriptor = if (isImageDocument) {
            DocumentDescriptor.imageDocumentFromUri(uri)
        } else {
            DocumentDescriptor.fromUri(uri)
        }
        documentCoordinator.addDocument(documentDescriptor)
        documentCoordinator.setVisibleDocument(documentDescriptor)
    }

    companion object {
        private val assetFiles = arrayOf("Classbook.pdf", "Aviation.pdf", "Annotations.pdf")

        private const val REQUEST_OPEN_DOCUMENT = 1
        private const val REQUEST_ASK_FOR_PERMISSION = 2
        private const val DOWNLOAD_PROGRESS_FRAGMENT = "DownloadProgressFragment"
    }
}
