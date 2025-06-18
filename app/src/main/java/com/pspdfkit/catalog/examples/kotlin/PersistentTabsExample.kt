/*
 *   Copyright © 2019-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.core.content.edit
import androidx.core.net.toUri
import com.pspdfkit.Nutrient
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask.extractAsync
import com.pspdfkit.catalog.utils.Utils
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * This example shows how to persist list of opened documents/tabs between activity instances.
 */
class PersistentTabsExample(context: Context) : SdkExample(context.getString(R.string.persistentTabsExampleTitle), context.getString(R.string.persistentTabsExampleDescription)) {

    @SuppressLint("CheckResult")
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        configuration
            // Make the tab bar always visible.
            .setTabBarHidingMode(TabBarHidingMode.SHOW)

        // We use shared preferences for storing tabs state in this example.
        // Preferences access is handled with TabsPreferences class. We don't
        val tabsPreferences = TabsPreferences.get(context)
        // Then retrieve document descriptors saved in the user's shared preferences.
        val restoredDocumentDescriptors = tabsPreferences.getDocumentDescriptors(context)

        // If there are no document descriptors this this means that this example is running for
        // the first time. In this case, we'll extract a few documents from the app assets to be shown as tabs.
        if (restoredDocumentDescriptors == null) {
            val pdfFiles = listOf(WELCOME_DOC, "Aviation.pdf", "Annotations.pdf")
            val imageFiles = listOf("images/android.png")

            val extractAssetsObservable = Observable.concat(
                Observable.fromIterable(pdfFiles)
                    .flatMapSingle { assetName -> extractAsync(assetName, assetName, context, false, null) }
                    // PdfActivity uses document descriptors to encapsulate all information required for opening a single document.
                    // Create document descriptors for extracted files right away so we can pass them directly to PdfActivityIntentBuilder.
                    .map { file -> DocumentDescriptor.fromUri(Uri.fromFile(file)) },
                Observable.fromIterable(imageFiles)
                    .flatMapSingle { assetName -> extractAsync(assetName, assetName, context, false, "png") }
                    .map {
                        val descriptor = DocumentDescriptor.imageDocumentFromUri(Uri.fromFile(it))
                        // File name of the image document is used as document title. Override this with a custom title.
                        descriptor.setTitle("Android Image Document")
                        descriptor
                    }
            )

            extractAssetsObservable
                // Collect the document descriptors into a single list.
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { documentDescriptors ->
                    // Launch the example activity with extracted documents in tabs.
                    launchExampleActivity(context, documentDescriptors, configuration)
                }
        } else {
            // Launch the example activity with restored document descriptors and restore visible document.
            launchExampleActivity(context, restoredDocumentDescriptors, configuration, tabsPreferences.getVisibleDocumentIndex())
        }
    }

    private fun launchExampleActivity(context: Context, documentDescriptors: List<DocumentDescriptor>, configuration: PdfActivityConfiguration.Builder, visibleDocumentIndex: Int = 0) {
        val intentBuilder = if (documentDescriptors.isEmpty()) {
            PdfActivityIntentBuilder.emptyActivity(context)
        } else {
            PdfActivityIntentBuilder.fromDocumentDescriptor(context, *documentDescriptors.toTypedArray())
        }
        intentBuilder.visibleDocument(visibleDocumentIndex)
            .configuration(configuration.build())
            .activityClass(PersistentTabsActivity::class.java)

        context.startActivity(intentBuilder.build())
    }
}

/**
 * We use [SharedPreferences] for storing tabs state in this example.
 * This class encapsulates the required serialization/deserialization of [DocumentDescriptor] data to JSON.
 */
class TabsPreferences(private val preferences: SharedPreferences) {

    companion object {
        // We use separate preferences
        const val PREFERENCES_NAME = "Nutrient.PersistentTabsExample"

        const val JSON_DESCRIPTOR_URI = "uri"
        const val JSON_DESCRIPTOR_TITLE = "title"

        const val PREF_DOCUMENT_DESCRIPTORS_JSON = "document_descriptors"
        const val PREF_VISIBLE_DOCUMENT_INDEX = "visible_document_index"

        private var instance: TabsPreferences? = null

        /**
         * Returns global singleton preferences instance.
         */
        @Synchronized
        fun get(context: Context): TabsPreferences {
            if (instance == null) {
                val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                instance = TabsPreferences(preferences)
            }
            return instance!!
        }
    }

    /**
     * Stores list of document descriptors in preferences.
     */
    fun setDocumentDescriptors(descriptors: List<DocumentDescriptor>) {
        // A simple JSON based format is used for serializing document descriptors.
        val descriptorsArray = JSONArray()
        for (descriptor in descriptors) {
            val descriptorJson = JSONObject()
            // The only data we need to restore tabs state is file Uri (we expect all document sources
            // to be Uri based for the sake of simplicity) and possible custom document title.
            descriptorJson.put(JSON_DESCRIPTOR_URI, descriptor.documentSource.fileUri)
            descriptorJson.put(JSON_DESCRIPTOR_TITLE, descriptor.customTitle)
            descriptorsArray.put(descriptorJson)
        }
        preferences.edit { putString(PREF_DOCUMENT_DESCRIPTORS_JSON, descriptorsArray.toString()) }
    }

    /**
     * Returns the list of document descriptors stored in the shared preferences.
     *
     * @return List of document descriptors to display in tabs, empty list for cases where an empty
     * activity should be displayed and `null` if no previous state was stored in the preferences.
     */
    fun getDocumentDescriptors(context: Context): List<DocumentDescriptor>? {
        val descriptorsJson = preferences.getString(PREF_DOCUMENT_DESCRIPTORS_JSON, null)
            ?: return null
        val descriptorsArray = JSONArray(descriptorsJson)

        val documentDescriptors = mutableListOf<DocumentDescriptor>()
        for (i in 0 until descriptorsArray.length()) {
            val descriptorJson = descriptorsArray[i] as JSONObject
            val uri = descriptorJson.getString(JSON_DESCRIPTOR_URI) ?: continue
            val title = if (descriptorJson.has(JSON_DESCRIPTOR_TITLE)) descriptorJson.getString(JSON_DESCRIPTOR_TITLE) else null

            val fileUri = uri.toUri()
            val documentDescriptor = if (ImageDocumentUtils.isImageUri(context, fileUri)) {
                DocumentDescriptor.imageDocumentFromUri(fileUri)
            } else {
                DocumentDescriptor.fromUri(fileUri)
            }
            if (title != null) {
                documentDescriptor.setTitle(title)
            }
            documentDescriptors.add(documentDescriptor)
        }
        return documentDescriptors
    }

    /**
     * Sets index of currently visible document in the list of stored document descriptors.
     */
    fun setVisibleDocumentIndex(visibleDocumentIndex: Int) {
        preferences.edit().putInt(PREF_VISIBLE_DOCUMENT_INDEX, visibleDocumentIndex).apply()
    }

    /**
     * Returns index of currently visible document in the list of stored document descriptors.
     */
    fun getVisibleDocumentIndex(): Int {
        return preferences.getInt(PREF_VISIBLE_DOCUMENT_INDEX, 0)
    }
}

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
        // On Android 6.0+ we ask for SD card access permission. This isn't strictly necessary, but Nutrient
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
            // Nutrient can open documents without the permissions when SAF is used, however the access
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
                if (Nutrient.isOpenableUri(this, uri)) {
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
