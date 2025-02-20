/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import static com.pspdfkit.catalog.ui.FilterPickerView.Filter.ALL;
import static com.pspdfkit.catalog.ui.FilterPickerView.Filter.ANNOTATED;
import static com.pspdfkit.catalog.ui.FilterPickerView.Filter.BOOKMARKED;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.bookmarks.Bookmark;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.examples.java.activities.viewmodels.FilterableThumbnailGridViewModel;
import com.pspdfkit.catalog.ui.FilterPickerView;
import com.pspdfkit.catalog.ui.FilterPickerView.Filter;
import com.pspdfkit.document.DocumentSource;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.document.processor.PdfProcessor;
import com.pspdfkit.document.processor.PdfProcessorTask;
import com.pspdfkit.listeners.OnVisibilityChangedListener;
import com.pspdfkit.ui.DocumentCoordinator;
import com.pspdfkit.ui.DocumentDescriptor;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfThumbnailGrid;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An example activity that demonstrates how to take the {@link PdfThumbnailGrid}, add a custom view
 * to it, and connect it with the {@link PdfProcessor} and {@link DocumentCoordinator} to create an
 * example where we can filter pages. This example incorporates the filter with choices to show all
 * pages, just annotated pages, or just bookmarked pages.
 *
 * <p>Each filtering process first retrieves the pages that need to be in the document, and then
 * uses the {@link PdfProcessor} to keep those pages. Finally it reloads the document.
 */
public class FilterableThumbnailGridActivity extends PdfActivity implements FilterPickerView.OnFilterClickedListener {

    /** Argument used for storing last selected filter in the {@link FilterPickerView}. */
    private static final String ARG_LAST_SELECTED_FILTER = "FilterableThumbnailGridActivity.LAST_SELECTED_FILTER";

    /** A view with selectable filters that we will add to the thumbnail grid layout. */
    @Nullable
    private FilterPickerView filterPickerView = null;

    /**
     * In this example, we use this {@link androidx.lifecycle.ViewModel} to store the original
     * (unfiltered) document across configurations changes and reloads. View Model is part of the
     * new Android architecture components and is currently the way to go when it comes to restoring
     * the state across configuration changes (among other things).
     *
     * @see <a href="https://developer.android.com/topic/libraries/architecture/viewmodel">ViewModel
     *     - Developers Guide</a>
     */
    private FilterableThumbnailGridViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View model provider is used to instantiate the view model. If this activity is
        // recreated, it will receive the instance of the same view model.
        viewModel = new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory())
                .get(FilterableThumbnailGridViewModel.class);

        // Try to restore the last selected filter, otherwise use the default one.
        Filter restoredFilter = null;
        if (savedInstanceState != null) {
            restoredFilter = (Filter) savedInstanceState.getSerializable(ARG_LAST_SELECTED_FILTER);
        }
        final Filter selectedFilter = (restoredFilter != null) ? restoredFilter : ALL;

        // Get the thumbnail grid view.
        PdfThumbnailGrid thumbnailGridView = getPSPDFKitViews().getThumbnailGridView();
        if (thumbnailGridView != null) {

            // We don't inflate thumbnail grid view until it's displayed.
            // Check if displayed before generating view. If not, wait for it to show.
            if (thumbnailGridView.isDisplayed()) {
                generateFilterView(thumbnailGridView, selectedFilter);
            } else {
                getPSPDFKitViews().addOnVisibilityChangedListener(new OnVisibilityChangedListener() {
                    @Override
                    public void onShow(@NonNull View view) {
                        if (view == thumbnailGridView) {
                            generateFilterView(thumbnailGridView, selectedFilter);
                        }
                    }

                    @Override
                    public void onHide(@NonNull View view) {
                        // No action needed when hiding the thumbnail bar.
                    }
                });
            }
        }
    }

    @Override
    public void onDocumentLoaded(@NonNull PdfDocument document) {
        super.onDocumentLoaded(document);

        // We save the original document in the view model when first loaded.
        if (viewModel.pdfDocument == null) {
            viewModel.pdfDocument = document;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save last selected filter.
        if (filterPickerView != null) {
            outState.putSerializable(ARG_LAST_SELECTED_FILTER, filterPickerView.getSelectedFilter());
        }
    }

    /**
     * Adds a filter picker to the provided thumbnail grid view.
     *
     * @param thumbnailGridView The thumbnail grid view in which to add the {@link
     *     FilterPickerView}.
     * @param selectedFilter The default filter that will be selected.
     */
    private void generateFilterView(@NonNull PdfThumbnailGrid thumbnailGridView, @NonNull Filter selectedFilter) {
        // Generate only once.
        if (filterPickerView != null && filterPickerView.getParent() != null) return;

        // Initialize filter picker view.
        filterPickerView = new FilterPickerView(FilterableThumbnailGridActivity.this);

        // Pick filters that will be displayed.
        filterPickerView.setFilters(Arrays.asList(ALL, ANNOTATED, BOOKMARKED));

        // Set which filter to select.
        filterPickerView.setSelectedFilter(selectedFilter);

        // Attach listener to get the filter picker clicks.
        filterPickerView.setOnFilterClickedListener(FilterableThumbnailGridActivity.this);

        // Set elevation on the picker bar.
        ViewCompat.setElevation(
                filterPickerView, getResources().getDimension(R.dimen.pspdf__thumbnailGridFilterPickerViewElevation));

        // Add view to the thumbnail grid (which is a RelativeLayout).
        thumbnailGridView.addView(
                filterPickerView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        // Get the recycle view and position it below the newly added view.
        final View recyclerView = thumbnailGridView.findViewById(com.pspdfkit.R.id.pspdf__thumbnail_grid_recycler_view);
        if (recyclerView != null) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recyclerView.getLayoutParams();
            params.addRule(RelativeLayout.BELOW, R.id.filter_view);
            recyclerView.setLayoutParams(params);
        }
    }

    @Override
    public void onFilterClicked(@NonNull Filter filter) {
        if (filter == ALL) {
            if (filterPickerView != null) {
                filterPickerView.setSelectedFilter(ALL);
            }
            loadOriginalDocument();
        } else if (filter == ANNOTATED) {
            // Get pages that contain annotations and filter them out in the document.
            final PdfDocument originalDocument = viewModel.pdfDocument;
            if (originalDocument != null) {
                // Get the provider for fetching the annotations.
                originalDocument
                        .getAnnotationProvider()
                        // Get all annotations.
                        .getAllAnnotationsOfTypeAsync(EnumSet.allOf(AnnotationType.class))
                        // Map annotations to the page index they have (that's the only thing we
                        // need for filtering).
                        .map(Annotation::getPageIndex)
                        // Avoid duplicates.
                        .distinct()
                        // Convert to list.
                        .toList()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<List<Integer>>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                // Do something once the filtering process starts. Not
                                // implemented in this example.
                            }

                            @Override
                            public void onSuccess(List<Integer> integers) {
                                if (integers.isEmpty()) { // If there are no annotations, we
                                    // don't have any document to switch
                                    // to.
                                    Toast.makeText(
                                                    FilterableThumbnailGridActivity.this,
                                                    "There are no annotated pages in the document.",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                    return;
                                }

                                // Refresh selection.
                                if (filterPickerView != null) {
                                    filterPickerView.setSelectedFilter(ANNOTATED);
                                }

                                // Filter the original document to contain just the pages
                                // with annotations, and load it.
                                loadFilteredDocument(originalDocument, new HashSet<>(integers), "annotated");
                            }

                            @Override
                            public void onError(Throwable e) {
                                // Handle filtering error here.
                            }
                        });
            }
        } else if (filter == BOOKMARKED) {
            // Get pages that contain bookmarks and filter them out in the document.
            final PdfDocument originalDocument = viewModel.pdfDocument;
            if (originalDocument != null) {
                // Get the provider for fetching the bookmarks.
                originalDocument
                        .getBookmarkProvider()
                        // Get all bookmarks.
                        .getBookmarksAsync()
                        // Split the list and emit elements separately.
                        .flatMapIterable((Function<List<Bookmark>, Iterable<Bookmark>>) bookmarks -> bookmarks)
                        // Map bookmarks to the page index they have (that's the only thing we need
                        // for filtering).
                        .map(Bookmark::getPageIndex)
                        // Avoid duplicates.
                        .distinct()
                        // Convert to list.
                        .toList()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<List<Integer>>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                // Do something once the filtering process starts. Not
                                // implemented in this example.
                            }

                            @Override
                            public void onSuccess(List<Integer> integers) {
                                if (integers.isEmpty()) { // If there are no bookmarked pages,
                                    // we don't have any document to
                                    // switch to.
                                    Toast.makeText(
                                                    FilterableThumbnailGridActivity.this,
                                                    "There are no bookmarked pages in the document.",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                    return;
                                }

                                // Refresh selection.
                                if (filterPickerView != null) {
                                    filterPickerView.setSelectedFilter(BOOKMARKED);
                                }

                                // Filter the original document to contain just the
                                // bookmarked pages, and load it.
                                loadFilteredDocument(originalDocument, new HashSet<>(integers), "bookmarked");
                            }

                            @Override
                            public void onError(Throwable e) {
                                // Handle filtering error here.
                            }
                        });
            }
        }
    }

    /** Loads the original document. */
    private void loadOriginalDocument() {
        if (viewModel != null && viewModel.pdfDocument != null) {
            final DocumentCoordinator documentCoordinator = getDocumentCoordinator();
            final DocumentDescriptor originalDocumentDescriptor =
                    DocumentDescriptor.fromDocument(viewModel.pdfDocument);
            documentCoordinator.addOnDocumentVisibleListener(documentDescriptor1 ->
                    getPSPDFKitViews().getThumbnailGridView().show());
            documentCoordinator.setDocument(originalDocumentDescriptor);
        }
    }

    /**
     * Loads the filtered document.
     *
     * @param originalDocument Original document on which to perform filtering.
     * @param pages Pages that will be taken from the original document.
     * @param fileSuffix Suffix to add to the filename when creating the filtered document.
     */
    private void loadFilteredDocument(
            @NonNull PdfDocument originalDocument, @NonNull Set<Integer> pages, @NonNull String fileSuffix) {
        final PdfProcessorTask pdfProcessorTask =
                PdfProcessorTask.fromDocument(originalDocument).keepPages(pages);
        final File newDocumentFile = new File(getFilesDir(), originalDocument.getTitle() + "-" + fileSuffix);
        PdfProcessor.processDocumentAsync(pdfProcessorTask, newDocumentFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultSubscriber<PdfProcessor.ProcessorProgress>() {
                    @Override
                    public void onNext(PdfProcessor.ProcessorProgress processorProgress) {
                        // Consume processing progress here. Could be used to show the
                        // filtering progress in some UI.
                    }

                    @Override
                    public void onError(Throwable t) {
                        // Handle processing error here. Not handled in this particular
                        // example.
                    }

                    @Override
                    public void onComplete() {
                        try {
                            DocumentSource documentSource =
                                    new DocumentSource(Uri.fromFile(newDocumentFile.getCanonicalFile()));
                            DocumentDescriptor documentDescriptor =
                                    DocumentDescriptor.fromDocumentSource(documentSource);
                            DocumentCoordinator documentCoordinator = getDocumentCoordinator();
                            documentCoordinator.addOnDocumentVisibleListener(documentDescriptor1 ->
                                    getPSPDFKitViews().getThumbnailGridView().show());
                            documentCoordinator.setDocument(documentDescriptor);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
