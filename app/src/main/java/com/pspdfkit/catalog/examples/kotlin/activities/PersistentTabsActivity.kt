/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import com.pspdfkit.PSPDFKit
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.examples.kotlin.TabsPreferences
import com.pspdfkit.catalog.utils.Utils
import com.pspdfkit.document.ImageDocumentUtils
import com.pspdfkit.document.download.DownloadJob
import com.pspdfkit.document.download.DownloadProgressFragment
import com.pspdfkit.document.download.DownloadRequest
import com.pspdfkit.ui.DocumentCoordinator
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import java.io.File

/**
 * This example activity shows multiple documents in tabs and persists
 * currently opened documents to preferences once left by the user.
 */
class PersistentTabsActivity : PdfActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Insert add tab button at the start of the tab bar.
        val tabsBar = pspdfKitViews.tabBar
        if (tabsBar != null) {
            val addTabButton = layoutInflater.inflate(R.layout.item_add_button, tabsBar, false) as ImageView
            addTabButton.setOnClickListener { addNewTab() }
            tabsBar.addView(addTabButton, 0)
        }
    }

    override fun onStop() {
        // Save opened document descriptors and currently visible document index to preferences.
        val tabsPreferences = TabsPreferences.get(this)

        val documents = documentCoordinator.documents
        tabsPreferences.setDocumentDescriptors(documents)

        val visibleDocumentIndex = documents.indexOf(documentCoordinator.visibleDocument)
        tabsPreferences.setVisibleDocumentIndex(if (visibleDocumentIndex >= 0) visibleDocumentIndex else 0)

        // Proceed with stopping the activity.
        super.onStop()
    }

    private fun addNewTab() {
        // On Android 6.0+ we ask for SD card access permission. This isn't strictly necessary, but PSPDFKit
        // being able to access file directly will significantly improve performance.
        // Since documents can be annotated we ask for write permission as well.
        if (Utils.requestExternalStorageRwPermission(this, REQUEST_ASK_FOR_PERMISSION)) {
            showOpenFileDialog()
        }
    }

    // We're temporarily suppressing the warning for startActivityForResult being deprecated.
    // Issue: https://github.com/PSPDFKit/PSPDFKit/issues/31881
    @Suppress("DEPRECATION")
    private fun showOpenFileDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        // We accept PDF files and images (for image documents).
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))

        // Set of the intent for result, so we can retrieve the Uri of the selected document.
        startActivityForResult(intent, REQUEST_OPEN_DOCUMENT)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_ASK_FOR_PERMISSION) {
            // We attempt to open document after permissions have been requested.
            // PSPDFKit can open documents without the permissions when SAF is used, however the access
            // without permissions will be significantly slower.
            showOpenFileDialog()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_OPEN_DOCUMENT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val uri = data.data ?: return
                val isImageFile = ImageDocumentUtils.isImageUri(this, uri)

                // Some URIs can be opened directly, including local filesystem, app assets, and content provider URIs.
                if (PSPDFKit.isOpenableUri(this, uri)) {
                    showDocumentInNewTab(uri, isImageFile)
                } else {
                    // The Uri cannot be directly opened. Download the PDF document from the uri, for local access.

                    // Find the DownloadProgressFragment for showing download progress, or create a new one.
                    val downloadFragment = supportFragmentManager.findFragmentByTag(
                        DOWNLOAD_PROGRESS_FRAGMENT
                    ) as? DownloadProgressFragment ?: run {
                        val job = DownloadJob.startDownload(DownloadRequest.Builder(this).uri(uri).build())
                        val downloadFragment1 = DownloadProgressFragment()
                        downloadFragment1.show(supportFragmentManager, DOWNLOAD_PROGRESS_FRAGMENT)
                        downloadFragment1.job = job
                        downloadFragment1
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
        private val REQUEST_OPEN_DOCUMENT = 1
        private val REQUEST_ASK_FOR_PERMISSION = 2

        private val DOWNLOAD_PROGRESS_FRAGMENT = "DownloadProgressFragment"
    }
}
