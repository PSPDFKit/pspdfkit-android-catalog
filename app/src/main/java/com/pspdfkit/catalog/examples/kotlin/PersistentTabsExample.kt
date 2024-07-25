/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.kotlin.activities.PersistentTabsActivity
import com.pspdfkit.catalog.tasks.ExtractAssetTask.extractAsync
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.TabBarHidingMode
import com.pspdfkit.document.ImageDocumentUtils
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import org.json.JSONArray
import org.json.JSONObject

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
            val pdfFiles = listOf(QUICK_START_GUIDE, "Aviation.pdf", "Annotations.pdf")
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
        const val PREFERENCES_NAME = "PSPDFKit.PersistentTabsExample"

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
        preferences.edit().putString(PREF_DOCUMENT_DESCRIPTORS_JSON, descriptorsArray.toString()).apply()
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

            val fileUri = Uri.parse(uri)
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
