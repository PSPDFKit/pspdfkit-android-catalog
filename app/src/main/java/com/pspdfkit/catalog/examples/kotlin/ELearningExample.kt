/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.bookmarks.Bookmark
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask.extract
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.sharing.ShareFeatures
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.preferences.PSPDFKitPreferences
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import com.pspdfkit.utils.getSupportParcelableExtra
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.EnumSet

/**
 * This example shows how to swap between documents and sync view state, annotations, and bookmarks.
 */
class ELearningExample(context: Context) :
    SdkExample(context, R.string.eLearningExampleTitle, R.string.eLearningExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We'll disable redaction tool in annotation creation toolbar to prevent creation of redaction annotations.
        val annotationTools = mutableListOf(*AnnotationTool.values())
        annotationTools.remove(AnnotationTool.REDACTION)

        configuration
            // Turn off saving, so we have the clean original document every time the example is launched.
            .autosaveEnabled(false)
            // Use single page mode.
            .layoutMode(PageLayoutMode.SINGLE)
            // Disable all the menu items but annotations editing and bookmark list.
            .outlineEnabled(false)
            .documentInfoViewEnabled(false)
            .annotationListEnabled(false)
            .searchEnabled(false)
            .settingsMenuEnabled(false)
            .setEnabledShareFeatures(EnumSet.noneOf(ShareFeatures::class.java))
            .printingEnabled(false)
            .thumbnailGridEnabled(false)
            .setRedactionUiEnabled(false)
            .enabledAnnotationTools(annotationTools)

        // The annotation creator written into newly created annotations. If not set, or set to null
        // a dialog will normally be shown when creating an annotation, asking you to enter a name.
        // We are going to skip this part and set it as "John Doe" only if it was not yet set.
        if (!PSPDFKitPreferences.get(context).isAnnotationCreatorSet) {
            PSPDFKitPreferences.get(context).setAnnotationCreator("John Doe")
        }
        // Extracts the documents from the assets and loads the teacher version first.
        extract("Teacher.pdf", title, context) { teacherDocumentFile ->
            extract("Student.pdf", title, context) { studentDocumentFile ->
                val teacherDocumentUri = Uri.fromFile(teacherDocumentFile)
                val studentDocumentUri = Uri.fromFile(studentDocumentFile)
                val intent = PdfActivityIntentBuilder.fromUri(context, teacherDocumentUri)
                    .configuration(configuration.build())
                    .activityClass(ELearningActivity::class)
                    .build()
                intent.putExtra(ELearningActivity.STUDENT_URI_KEY, studentDocumentUri)
                intent.putExtra(ELearningActivity.TEACHER_URI_KEY, teacherDocumentUri)
                context.startActivity(intent)
            }
        }
    }
}

class ELearningActivity : PdfActivity() {

    /** List of annotations in instant JSON format that will be copied over the new document.  */
    private val serializedAnnotationsToTransfer: MutableList<String> = mutableListOf()

    /** List of bookmarks that will be copied over the new document.  */
    private val bookmarksToTransfer: MutableList<Bookmark> = mutableListOf()

    /** Student document URI. */
    private lateinit var studentUri: Uri

    /** Teacher document URI. */
    private lateinit var teacherUri: Uri

    /** Job in charge of switching the document. */
    private var switchDocumentJob: Job? = null

    /** Job in charge of loading the annotation to the new document. */
    private var transferDataJob: Job? = null

    /** Fragment state containing current page, zoom, and scroll. */
    private var fragmentState: Bundle? = null

    /** Flag to keep track of visibility of bookmark list view. */
    private var isBookmarkListDisplayed: Boolean = false

    /**
     * Create a button to switch between teacher and student documents.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        if (document == null || serializedAnnotationsToTransfer.isNotEmpty()) {
            return false
        }
        // Add a new switch button.
        val switchItem = menu.add(0, SWITCH_ITEM_ID, 0, "Switch")
        switchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT)

        return true
    }

    /**
     * Set the corresponding action for every button.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            SWITCH_ITEM_ID -> {
                switchDocument()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onPause() {
        switchDocumentJob?.cancel()
        transferDataJob?.cancel()
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getSupportParcelableExtra(STUDENT_URI_KEY, Uri::class.java)?.let {
            studentUri = it
        }
        intent.getSupportParcelableExtra(TEACHER_URI_KEY, Uri::class.java)?.let {
            teacherUri = it
        }
    }

    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        // Take any serialized annotations that were stored upon switching, and add them to the loaded document.
        transferDataJob = lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // Create annotations from instant JSON and add them to the document.
                for (json in serializedAnnotationsToTransfer) {
                    document.annotationProvider.createAnnotationFromInstantJson(json)
                }
                // Once all annotations are transferred, we can clear the list.
                serializedAnnotationsToTransfer.clear()

                // Take any bookmarks that were stored upon switching, and add them to the loaded document.
                bookmarksToTransfer.forEach { bookmark ->
                    document.bookmarkProvider.addBookmarkAsync(bookmark).blockingAwait()
                }
                // Once all bookmarks are transferred, we can clear the list.
                bookmarksToTransfer.clear()
            }

            // Restore page index, scroll and zoom.
            fragmentState?.let {
                requirePdfFragment().state = it
                fragmentState = null
            }

            // Make sure the outline view shows the changes from transferring bookmarks and annotations.
            pspdfKitViews.outlineView?.setDocument(document, configuration.configuration)

            // Restore bookmark list view visibility.
            if (isBookmarkListDisplayed) {
                pspdfKitViews.outlineView?.show()
            }

            invalidateOptionsMenu()
        }
    }

    /**
     * Switch the document when the button is tapped. This works in multiple steps:
     *  - Fetching all annotations asynchronously, and convert them to Instant JSON format.
     *  - Fetch all bookmarks.
     *  - Save the view state.
     */
    private fun switchDocument() {
        val document = document ?: return

        // While the document switching is in progress, we disable all document interaction to
        // prevent changes to be made.
        requirePdfFragment().isDocumentInteractionEnabled = false

        // Run the collection of annotations and bookmarks asynchronously.
        switchDocumentJob = lifecycleScope.launch {
            // Clear previous data
            serializedAnnotationsToTransfer.clear()
            bookmarksToTransfer.clear()

            withContext(Dispatchers.IO) {
                // Fetch all annotations, serialize them, and collect them in a list. We keep them around in
                // memory, and will add them to the other document once it is loaded.
                val annotations = document.annotationProvider.getAllAnnotationsOfType(
                    AnnotationType.entries.toSet()
                )

                annotations.forEach { annotation ->
                    val json = annotation.toInstantJson()
                    // For some unsupported annotation types (like popup annotations) we don't offer serialization.
                    // In these cases, `toInstantJson()` returns "null". We filter those from the serialized items.
                    if (invalidInstantJson(json)) {
                        serializedAnnotationsToTransfer.add(json)
                    }
                }

                // Fetch the bookmarks for synchronization and collect them in a list.
                val bookmarks = document.bookmarkProvider.bookmarks
                bookmarksToTransfer.addAll(bookmarks)
            }

            // Save current page index, scroll and zoom.
            fragmentState = requirePdfFragment().state

            // Save bookmark list view visibility.
            isBookmarkListDisplayed = pspdfKitViews.outlineView?.isDisplayed ?: false

            // Swap out the current document to either the teacher or student version.
            if (document.documentSource.fileUri == teacherUri) {
                documentCoordinator.setDocument(DocumentDescriptor.fromUri(studentUri))
            } else {
                documentCoordinator.setDocument(DocumentDescriptor.fromUri(teacherUri))
            }
            invalidateOptionsMenu()
        }
    }

    companion object {
        private const val SWITCH_ITEM_ID = 1234
        const val STUDENT_URI_KEY = "TeacherStudentActivity.STUDENT_URI_KEY"
        const val TEACHER_URI_KEY = "TeacherStudentActivity.TEACHER_URI_KEY"
    }
}

private fun invalidInstantJson(it: String) = it != "null"
