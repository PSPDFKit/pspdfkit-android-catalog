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
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.bookmarks.Bookmark
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.ui.FilterPickerView
import com.pspdfkit.catalog.ui.FilterPickerView.Filter
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.listeners.OnVisibilityChangedListener
import com.pspdfkit.ui.DocumentDescriptor
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.PdfThumbnailGrid
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subscribers.DefaultSubscriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.EnumSet

/**
 * Example on how to display filters in the thumbnail grid. Shows how to combine PDF processor and
 * customizability of the thumbnail grid to create filters.
 */
class FilterableThumbnailGridExample(context: Context) :
    SdkExample(
        context,
        R.string.filterableThumbnailGridExampleTitle,
        R.string.filterableThumbnailGridExampleDescription,
    ) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent =
            PdfActivityIntentBuilder
                .fromDataProvider(context, AssetDataProvider(WELCOME_DOC))
                .configuration(configuration.build())
                .activityClass(FilterableThumbnailGridActivity::class.java)
                .build()

        context.startActivity(intent)
    }
}

/** [ViewModel] used for storing the pdf document in the [FilterableThumbnailGridExample]. */
class FilterableThumbnailGridViewModel : ViewModel() {
    var pdfDocument: PdfDocument? = null
}

/**
 * An example activity that demonstrates how to take the [PdfThumbnailGrid], add a custom view
 * to it, and connect it with the [PdfProcessor] and [com.pspdfkit.ui.DocumentCoordinator] to create an
 * example where we can filter pages. This example incorporates the filter with choices to show all
 * pages, just annotated pages, or just bookmarked pages.
 *
 * Each filtering process first retrieves the pages that need to be in the document, and then
 * uses the [PdfProcessor] to keep those pages. Finally it reloads the document.
 */
class FilterableThumbnailGridActivity :
    PdfActivity(),
    FilterPickerView.OnFilterClickedListener {
    /** A view with selectable filters that we will add to the thumbnail grid layout. */
    private var filterPickerView: FilterPickerView? = null

    /**
     * In this example, we use this [androidx.lifecycle.ViewModel] to store the original
     * (unfiltered) document across configurations changes and reloads. View Model is part of the
     * new Android architecture components and is currently the way to go when it comes to restoring
     * the state across configuration changes (among other things).
     *
     * @see [ViewModel - Developers Guide](https://developer.android.com/topic/libraries/architecture/viewmodel)
     */
    private lateinit var viewModel: FilterableThumbnailGridViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View model provider is used to instantiate the view model. If this activity is
        // recreated, it will receive the instance of the same view model.
        viewModel =
            ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
                .get(FilterableThumbnailGridViewModel::class.java)

        // Try to restore the last selected filter, otherwise use the default one.
        @Suppress("DEPRECATION")
        val restoredFilter = savedInstanceState?.getSerializable(ARG_LAST_SELECTED_FILTER) as? Filter
        val selectedFilter = restoredFilter ?: Filter.ALL

        // Get the thumbnail grid view.
        val thumbnailGridView = pspdfKitViews.thumbnailGridView
        if (thumbnailGridView != null) {
            // We don't inflate thumbnail grid view until it's displayed.
            // Check if displayed before generating view. If not, wait for it to show.
            if (thumbnailGridView.isDisplayed) {
                generateFilterView(thumbnailGridView, selectedFilter)
            } else {
                pspdfKitViews.addOnVisibilityChangedListener(
                    object : OnVisibilityChangedListener {
                        override fun onShow(view: View) {
                            if (view == thumbnailGridView) {
                                generateFilterView(thumbnailGridView, selectedFilter)
                            }
                        }

                        override fun onHide(view: View) {
                            // No action needed when hiding the thumbnail bar.
                        }
                    },
                )
            }
        }
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        // We save the original document in the view model when first loaded.
        if (viewModel.pdfDocument == null) {
            viewModel.pdfDocument = document
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save last selected filter.
        filterPickerView?.let { picker ->
            outState.putSerializable(ARG_LAST_SELECTED_FILTER, picker.selectedFilter)
        }
    }

    /**
     * Adds a filter picker to the provided thumbnail grid view.
     *
     * @param thumbnailGridView The thumbnail grid view in which to add the [FilterPickerView].
     * @param selectedFilter The default filter that will be selected.
     */
    private fun generateFilterView(thumbnailGridView: PdfThumbnailGrid, selectedFilter: Filter) {
        // Generate only once.
        if (filterPickerView?.parent != null) return

        // Initialize filter picker view.
        val picker = FilterPickerView(this@FilterableThumbnailGridActivity)
        filterPickerView = picker

        // Pick filters that will be displayed.
        picker.setFilters(listOf(Filter.ALL, Filter.ANNOTATED, Filter.BOOKMARKED))

        // Set which filter to select.
        picker.setSelectedFilter(selectedFilter)

        // Attach listener to get the filter picker clicks.
        picker.setOnFilterClickedListener(this@FilterableThumbnailGridActivity)

        // Set elevation on the picker bar.
        ViewCompat.setElevation(
            picker,
            resources.getDimension(R.dimen.pspdf__thumbnailGridFilterPickerViewElevation),
        )

        // Add view to the thumbnail grid (which is a RelativeLayout).
        thumbnailGridView.addView(
            picker,
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
        )

        // Get the recycle view and position it below the newly added view.
        val recyclerView = thumbnailGridView.findViewById<View>(com.pspdfkit.R.id.pspdf__thumbnail_grid_recycler_view)
        if (recyclerView != null) {
            val params = recyclerView.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.BELOW, R.id.filter_view)
            recyclerView.layoutParams = params
        }
    }

    override fun onFilterClicked(filter: Filter) {
        when (filter) {
            Filter.ALL -> {
                filterPickerView?.setSelectedFilter(Filter.ALL)
                loadOriginalDocument()
            }

            Filter.ANNOTATED -> {
                // Get pages that contain annotations and filter them out in the document.
                val originalDocument = viewModel.pdfDocument ?: return

                CoroutineScope(Dispatchers.Main).launch {
                    // Get all annotations and extract unique page indices.
                    val annotations =
                        originalDocument.annotationProvider.getAllAnnotationsOfType(
                            EnumSet.allOf(AnnotationType::class.java),
                        )
                    val pageIndices = annotations.map { it.pageIndex }.distinct()

                    if (pageIndices.isEmpty()) {
                        // If there are no annotations, we don't have any document to switch to.
                        Toast
                            .makeText(
                                this@FilterableThumbnailGridActivity,
                                "There are no annotated pages in the document.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        return@launch
                    }

                    // Refresh selection.
                    filterPickerView?.setSelectedFilter(Filter.ANNOTATED)

                    // Filter the original document to contain just the pages
                    // with annotations, and load it.
                    loadFilteredDocument(originalDocument, pageIndices.toHashSet(), "annotated")
                }
            }

            Filter.BOOKMARKED -> {
                // Get pages that contain bookmarks and filter them out in the document.
                val originalDocument = viewModel.pdfDocument ?: return
                // Get the provider for fetching the bookmarks.
                originalDocument
                    .bookmarkProvider
                    // Get all bookmarks.
                    .bookmarksAsync
                    // Split the list and emit elements separately.
                    .flatMapIterable { bookmarks: List<Bookmark> -> bookmarks }
                    // Filter out bookmarks without page index and map to page index.
                    .filter { bookmark: Bookmark -> bookmark.pageIndex != null }
                    // Map bookmarks to the page index they have (that's the only thing we need
                    // for filtering).
                    .map { bookmark: Bookmark -> bookmark.pageIndex as Int }
                    // Avoid duplicates.
                    .distinct()
                    // Convert to list.
                    .toList()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        object : SingleObserver<List<Int>> {
                            override fun onSubscribe(d: Disposable) {
                                // Do something once the filtering process starts. Not
                                // implemented in this example.
                            }

                            override fun onSuccess(integers: List<Int>) {
                                if (integers.isEmpty()) {
                                    // If there are no bookmarked pages, we don't have any document to switch to.
                                    Toast
                                        .makeText(
                                            this@FilterableThumbnailGridActivity,
                                            "There are no bookmarked pages in the document.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    return
                                }

                                // Refresh selection.
                                filterPickerView?.setSelectedFilter(Filter.BOOKMARKED)

                                // Filter the original document to contain just the
                                // bookmarked pages, and load it.
                                loadFilteredDocument(originalDocument, integers.toHashSet(), "bookmarked")
                            }

                            override fun onError(e: Throwable) {
                                // Handle filtering error here.
                            }
                        },
                    )
            }
        }
    }

    /** Loads the original document. */
    private fun loadOriginalDocument() {
        val originalDoc = viewModel.pdfDocument ?: return
        val coordinator = documentCoordinator
        val originalDocumentDescriptor = DocumentDescriptor.fromDocument(originalDoc)
        coordinator.addOnDocumentVisibleListener { pspdfKitViews.thumbnailGridView?.show() }
        coordinator.setDocument(originalDocumentDescriptor)
    }

    /**
     * Loads the filtered document.
     *
     * @param originalDocument Original document on which to perform filtering.
     * @param pages Pages that will be taken from the original document.
     * @param fileSuffix Suffix to add to the filename when creating the filtered document.
     */
    private fun loadFilteredDocument(originalDocument: PdfDocument, pages: Set<Int>, fileSuffix: String) {
        val pdfProcessorTask = PdfProcessorTask.fromDocument(originalDocument).keepPages(pages)
        val newDocumentFile = File(filesDir, "${originalDocument.title}-$fileSuffix")
        PdfProcessor
            .processDocumentAsync(pdfProcessorTask, newDocumentFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                object : DefaultSubscriber<PdfProcessor.ProcessorProgress>() {
                    override fun onNext(processorProgress: PdfProcessor.ProcessorProgress) {
                        // Consume processing progress here. Could be used to show the
                        // filtering progress in some UI.
                    }

                    override fun onError(t: Throwable) {
                        // Handle processing error here. Not handled in this particular
                        // example.
                    }

                    override fun onComplete() {
                        try {
                            val documentSource = DocumentSource(Uri.fromFile(newDocumentFile.canonicalFile))
                            val documentDescriptor = DocumentDescriptor.fromDocumentSource(documentSource)
                            val coordinator = documentCoordinator
                            coordinator.addOnDocumentVisibleListener { pspdfKitViews.thumbnailGridView?.show() }
                            coordinator.setDocument(documentDescriptor)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                },
            )
    }

    companion object {
        /** Argument used for storing last selected filter in the [FilterPickerView]. */
        private const val ARG_LAST_SELECTED_FILTER = "FilterableThumbnailGridActivity.LAST_SELECTED_FILTER"
    }
}
