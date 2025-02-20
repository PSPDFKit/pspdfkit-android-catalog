/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.activities

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.UiThread
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.bookmarks.Bookmark
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.utils.getSupportParcelableExtra
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import java.util.EnumSet

class ELearningActivity : PdfActivity() {

    /** List of annotations in instant JSON format that will be copied over the new document.  */
    private val serializedAnnotationsToTransfer: MutableList<String> = mutableListOf()

    /** List of bookmarks that will be copied over the new document.  */
    private val bookmarksToTransfer: MutableList<Bookmark> = mutableListOf()

    /** Student document URI. */
    private lateinit var studentUri: Uri

    /** Teacher document URI. */
    private lateinit var teacherUri: Uri

    /** Disposable in charge of switching the document. */
    private var switchDocumentDisposable: Disposable? = null

    /** Disposable in charge of loading the annotation to the new document. */
    private var transferDataDisposable: Disposable? = null

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
        switchDocumentDisposable.safelyDispose()
        transferDataDisposable.safelyDispose()
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
    @SuppressLint("CheckResult")
    override fun onDocumentLoaded(document: PdfDocument) {
        // Take any serialized annotations that were stored upon switching, and add them to the loaded document.
        val addAnnotations = Observable.fromIterable(serializedAnnotationsToTransfer)
            .toFlowable(BackpressureStrategy.BUFFER)
            // Create an annotation from the instant JSON and add it to the document.
            .flatMapSingle(document.annotationProvider::createAnnotationFromInstantJsonAsync)
            // Once all annotations are transferred, we can clear the list.
            .doOnComplete(serializedAnnotationsToTransfer::clear)
            .ignoreElements()

        // Take any bookmarks that were stored upon switching, and add them to the loaded document.
        val addBookmarks = Observable.fromIterable(bookmarksToTransfer)
            .flatMapCompletable(document.bookmarkProvider::addBookmarkAsync)
            // Once all bookmarks are transferred, we can clear the list.
            .doOnComplete(bookmarksToTransfer::clear)

        // Iterate through the annotation list.
        transferDataDisposable = Completable
            .mergeArray(
                addAnnotations,
                addBookmarks
            )
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
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
            .subscribe()
    }

    /**
     * Switch the document when the button is tapped. This works in multiple steps:
     *  - Fetching all annotations asynchronously, and convert them to Instant JSON format.
     *  - Fetch all bookmarks.
     *  - Save the view state.
     */
    private fun switchDocument() {
        val document = document ?: return

        // Fetch all annotations, serialize them, and collect them in a list. We keep them around in
        // memory, and will add them to the other document once it is loaded.
        val serializeAnnotations = document.annotationProvider
            .getAllAnnotationsOfTypeAsync(EnumSet.allOf(AnnotationType::class.java))
            .doOnSubscribe { serializedAnnotationsToTransfer.clear() }
            .map(Annotation::toInstantJson)
            // For some unsupported annotation types (like popup annotations) we don't offer serialization.
            // In these cases, `toInstantJson()` returns "null". We filter those from the serialized items.
            // those annotations that are not supported.
            .filter(::invalidInstantJson)
            // Collect all JSON annotations.
            .map(serializedAnnotationsToTransfer::add)
            .ignoreElements()

        // Fetch the bookmarks for synchronization and collect them in a list.
        val fetchBookmarks = document.bookmarkProvider.bookmarksAsync
            .doOnSubscribe { bookmarksToTransfer.clear() }
            .map(bookmarksToTransfer::addAll)
            .ignoreElements()

        // Run the collection of annotations and bookmarks asynchronously.
        switchDocumentDisposable = Completable
            .mergeArray(
                serializeAnnotations,
                fetchBookmarks
            )
            // While the document switching is in progress, we disable all document interaction to
            // prevent changes to be made.
            .doOnSubscribe {
                requirePdfFragment().isDocumentInteractionEnabled = false
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                // Save current page index, scroll and zoom.
                fragmentState = requirePdfFragment().state

                // Save bookmark list view visibility.
                isBookmarkListDisplayed = pspdfKitViews.outlineView?.isDisplayed ?: false

                // Swap out the current document to eather the teacher or student version.
                if (document.documentSource.fileUri == teacherUri) {
                    documentCoordinator.setDocument(DocumentDescriptor.fromUri(studentUri))
                } else {
                    documentCoordinator.setDocument(DocumentDescriptor.fromUri(teacherUri))
                }
                invalidateOptionsMenu()
            }
            .subscribe()
    }

    /**
     * If `disposable` is non-null, it will be disposed upon calling this method. If `disposable` is null, this method is a no-op.
     * This method will always return `null` which allows to null out any disposable reference in a single statement. If
     * the disposable is disposed `onDispose()` will be called.
     */
    private fun Disposable?.safelyDispose(onDispose: Action? = null): Disposable? {
        if (this != null && !isDisposed) {
            dispose()
            onDispose?.run()
        }
        return null
    }

    companion object {
        private const val SWITCH_ITEM_ID = 1234
        const val STUDENT_URI_KEY = "TeacherStudentActivity.STUDENT_URI_KEY"
        const val TEACHER_URI_KEY = "TeacherStudentActivity.TEACHER_URI_KEY"
    }
}

private fun invalidInstantJson(it: String) = it != "null"
