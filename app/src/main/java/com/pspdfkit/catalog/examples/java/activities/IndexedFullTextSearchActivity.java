/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.drawable.DrawableCompat;
import com.google.android.material.snackbar.Snackbar;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.utils.Utils;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.datastructures.Range;
import com.pspdfkit.document.DocumentSource;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.document.PdfDocumentLoader;
import com.pspdfkit.document.library.PdfLibrary;
import com.pspdfkit.document.library.QueryOptions;
import com.pspdfkit.document.library.QueryPreviewResult;
import com.pspdfkit.document.library.QueryResultListener;
import com.pspdfkit.document.providers.AssetDataProvider;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import io.reactivex.rxjava3.core.Observable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This activity showcases {@link PdfLibrary} by indexing all PDFs of the catalog app, making them
 * searchable.
 */
public class IndexedFullTextSearchActivity extends AppCompatActivity {

    private static final String TAG = "IndexedFullTextSearch";

    /** Name of the Full-text search library file. */
    private static final String FTS_SEARCH_LIBRARY_NAME = "fts-library.db";

    /** UI-thread handler for updating the UI from a background thread. */
    private final Handler handler = new Handler();
    /** List view adapter for displaying search results. */
    @NonNull
    private final SearchResultAdapter adapter = new SearchResultAdapter();
    /** Contains document paths (relative to the assets) keyed by the document UID. */
    private Map<String, String> indexedDocumentPaths = new HashMap<>();
    /** FTS indexing library. */
    private PdfLibrary library;
    /** This {@link Snackbar} is used to show a ongoing indexing process. */
    @Nullable
    private Snackbar progressIndicator;
    /** Search option set by the user, to ignore search results inside the document body. */
    private boolean ignoreDocumentText = false;

    /** Search option set by the user, to ignore search results inside annotations. */
    private boolean ignoreAnnotations = false;

    /** Create the Full-text search indexing library and prepare the search result list view. */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fts_indexing);

        // Open the PdfLibrary on a desired location. This example stores the FTS index library
        // inside the app's private database directory.
        try {
            final File databaseFile = getDatabasePath(FTS_SEARCH_LIBRARY_NAME);
            if (!databaseFile.mkdirs() && !databaseFile.getParentFile().exists()) {
                Log.w(TAG, "Could not create the FTS indexing database directory.");
            }

            library = PdfLibrary.get(databaseFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error while creating the FTS library database.", e);
            Toast.makeText(
                            this,
                            "Could not create FTS library - see logcat for error. Exiting example.",
                            Toast.LENGTH_LONG)
                    .show();
            finish();
        }

        // Prepare the list view and adapter for displaying search results.
        final ListView searchResultsList = findViewById(android.R.id.list);
        searchResultsList.setAdapter(adapter);

        // Create a search result click listener that launches the PdfActivity showing the results.
        searchResultsList.setOnItemClickListener((parent, view, position, id) -> {
            final QueryPreviewResult clickedSearchResult = adapter.getItem(position);
            final String clickedDocumentPath = indexedDocumentPaths.get(clickedSearchResult.getUid());
            final AssetDataProvider dataProvider = new AssetDataProvider(clickedDocumentPath);

            // Open the touched search result on the correct page.
            final IndexedFullTextSearchActivity context = IndexedFullTextSearchActivity.this;
            final PdfActivityConfiguration configuration = new PdfActivityConfiguration.Builder(context)
                    .page(clickedSearchResult.getPageIndex())
                    .build();

            final Intent intent = PdfActivityIntentBuilder.fromDataProvider(context, dataProvider)
                    .configuration(configuration)
                    .build();
            startActivity(intent);
        });

        searchResultsList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Hide the keyboard when scrolling the list.
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL || scrollState == SCROLL_STATE_FLING) {
                    Utils.hideKeyboard(view);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });

        // Initially trigger document indexing.
        performIndexing();
    }

    /** Set up search inside the action bar. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_fts_indexing, menu);

        final MenuItem item = menu.findItem(R.id.action_search);
        final Drawable searchIcon = DrawableCompat.wrap(item.getIcon());
        DrawableCompat.setTint(searchIcon, Color.BLACK);
        item.setIcon(searchIcon);
        item.expandActionView();

        final SearchView searchView = (SearchView) item.getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.requestFocus();
        searchView.setQueryHint("Search PDF documents...");

        // Search is started as soon as the user starts writing.
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return performSearch(newText);
            }
        });

        final MenuItem checkboxIgnoreDocumentText = menu.findItem(R.id.checkboxIgnoreDocumentText);
        checkboxIgnoreDocumentText.setOnMenuItemClickListener(menuItem -> {
            menuItem.setChecked(!menuItem.isChecked());
            ignoreDocumentText = menuItem.isChecked();
            performSearch(searchView.getQuery().toString());
            return true;
        });

        final MenuItem checkboxIgnoreAnnotations = menu.findItem(R.id.checkboxIgnoreAnnotations);
        checkboxIgnoreAnnotations.setOnMenuItemClickListener(menuItem -> {
            menuItem.setChecked(!menuItem.isChecked());
            ignoreAnnotations = menuItem.isChecked();
            performSearch(searchView.getQuery().toString());
            return true;
        });

        return true;
    }

    /** Runs a search query on all indexed documents. */
    private boolean performSearch(String query) {
        if (query.length() > 2) {
            library.stopSearch();
            final QueryOptions options = new QueryOptions.Builder()
                    .ignoreDocumentText(ignoreDocumentText)
                    .ignoreAnnotations(ignoreAnnotations)
                    .generateTextPreviews(true)
                    .build();
            library.search(query, options, new QueryResultListener() {
                @Override
                public void onSearchCompleted(
                        @NonNull String searchString, @NonNull Map<String, Set<Integer>> results) {
                    Log.d(
                            TAG,
                            "onSearchCompleted() called with: "
                                    + "searchString = ["
                                    + searchString
                                    + "], results = ["
                                    + results
                                    + "]");
                }

                @Override
                public void onSearchPreviewsGenerated(
                        @NonNull String searchString, @NonNull final Map<String, Set<QueryPreviewResult>> results) {
                    Log.d(
                            TAG,
                            "onSearchPreviewsGenerated() called with: "
                                    + "searchString = ["
                                    + searchString
                                    + "], results = ["
                                    + results
                                    + "]");

                    // Search results are returned on a background thread. Post the results
                    // to the adapter, on the main thread.
                    runOnUiThread(() -> adapter.setSearchResults(results));
                }
            });

            return true;
        } else {
            // Clear the search results if the query is too short.
            adapter.setSearchResults(null);
        }
        return false;
    }

    /** Performs indexing of all available documents in the app's assets. */
    private void performIndexing() {
        final List<PdfDocument> indexingQueue = new ArrayList<>();

        // This example always clears the FTS index, just for demo purposes. In a real-world app
        // this is not necessary.
        library.clearIndex();

        try {
            // List all top-level assets of the app and filter them for PDF files.
            final List<String> assets = Observable.fromArray(
                            Objects.requireNonNull(getAssets().list("")))
                    .filter(s -> s.endsWith(".pdf"))
                    .toList()
                    .blockingGet();

            // Open and collect all documents that should be indexed.
            for (String asset : assets) {
                try {
                    // Try to open the document and if successful, enqueue it for indexing.
                    final PdfDocument document = PdfDocumentLoader.openDocumentAsync(
                                    this, new DocumentSource(new AssetDataProvider(asset)))
                            .blockingGet();
                    indexingQueue.add(document);

                    // Store the asset's path and name using its UID. This allows us to retrieve the
                    // document later when a search is performed.
                    indexedDocumentPaths.put(document.getUid(), asset);
                } catch (Exception ex) {
                    // This example catches any action that happens while opening the document (e.g.
                    // if a password would be needed).
                    // If an exception is thrown, the document will not be indexed.
                    Log.w(
                            TAG,
                            String.format("Could not open document '%s' from assets. See exception for reason.", asset),
                            ex);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error while trying to index all catalog app assets.", e);
            Toast.makeText(
                            this,
                            "Error listing asset files to index - see logcat for detailed error message.",
                            Toast.LENGTH_LONG)
                    .show();
        }

        if (indexingQueue.size() > 0) {
            // Start the actual document indexing.
            library.enqueueDocuments(indexingQueue);

            // Show to the user that indexing is in progress.
            progressIndicator =
                    Snackbar.make(findViewById(android.R.id.content), "Indexing...", Snackbar.LENGTH_INDEFINITE);
            progressIndicator.show();
            scheduleProgressIndicatorUpdate();
        }
    }

    /** Hides progress snackbar as soon as {@link PdfLibrary} has finished document indexing. */
    private void scheduleProgressIndicatorUpdate() {
        handler.postDelayed(
                () -> {
                    if (progressIndicator == null) return;

                    if (library.isIndexing()) {
                        scheduleProgressIndicatorUpdate();
                    } else {
                        progressIndicator.dismiss();
                        progressIndicator = null;
                    }
                },
                1000);
    }

    private static class ViewHolder {
        @NonNull
        public static ViewHolder get(View view, ViewGroup parent) {
            ViewHolder holder;

            if (view != null) {
                holder = (ViewHolder) view.getTag();
            } else {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fts_result, parent, false);
                holder = new ViewHolder(view);
                view.setTag(holder);
            }

            return holder;
        }

        final View view;
        final TextView documentTitleTextView;
        final TextView pageNumberTextView;
        final TextView previewTextView;

        private ViewHolder(View view) {
            this.view = view;
            documentTitleTextView = view.findViewById(R.id.documentTitleTextView);
            pageNumberTextView = view.findViewById(R.id.pageNumberTextView);
            previewTextView = view.findViewById(R.id.previewTextView);
        }
    }

    /** List view adapter for presenting search results. */
    private class SearchResultAdapter extends BaseAdapter {

        private List<QueryPreviewResult> listItems = new ArrayList<>();

        void setSearchResults(Map<String, Set<QueryPreviewResult>> searchResults) {
            listItems.clear();

            if (searchResults != null) {
                for (String documentUID : searchResults.keySet()) {
                    listItems.addAll(searchResults.get(documentUID));
                }
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return listItems.size();
        }

        @Override
        public QueryPreviewResult getItem(int position) {
            return listItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder = ViewHolder.get(convertView, parent);
            final QueryPreviewResult item = getItem(position);
            holder.documentTitleTextView.setText(indexedDocumentPaths.get(item.getUid()));
            holder.pageNumberTextView.setText(String.format(Locale.getDefault(), "Page %d", item.getPageIndex() + 1));

            // Highlight the actual search results phrase.
            final Range highlightedRange = item.getRangeInPreviewText();
            final SpannableString previewText = new SpannableString(item.getPreviewText());
            previewText.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    highlightedRange.getStartPosition(),
                    highlightedRange.getEndPosition(),
                    0);
            previewText.setSpan(
                    new BackgroundColorSpan(Color.YELLOW),
                    highlightedRange.getStartPosition(),
                    highlightedRange.getEndPosition(),
                    0);
            holder.previewTextView.setText(previewText);

            return holder.view;
        }
    }
}
