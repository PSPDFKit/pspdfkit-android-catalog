/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.utils.OnScrollListenerAdapter;
import com.pspdfkit.catalog.utils.Utils;
import com.pspdfkit.configuration.PdfConfiguration;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.document.search.SearchOptions;
import com.pspdfkit.document.search.SearchResult;
import com.pspdfkit.document.search.TextSearch;
import com.pspdfkit.listeners.SimpleDocumentListener;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.search.SearchResultHighlighter;
import com.pspdfkit.utils.Size;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This activity is a completely custom activity, using the {@link PdfFragment} for displaying a
 * document. Furthermore, it uses the support {@link SearchView} to ask the user for a search term,
 * and the {@link TextSearch} to search the loaded document for results. Finally, the {@link
 * SearchResultHighlighter} is used to draw the results on top of the pages.
 */
public class CustomSearchUiActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    public static final String EXTRA_URI = "CustomSearchUiExample.DocumentUri";
    private static final String TAG = "CustomSearchUiExample";
    private static final PdfConfiguration config = new PdfConfiguration.Builder().build();

    private PdfFragment fragment;
    private PdfDocument document;

    @Nullable
    private TextSearch textSearch;

    @Nullable
    private Disposable currentSearch;

    private SearchResultHighlighter highlighter;

    private SearchResultAdapter adapter;
    private List<SearchResult> currentSearchResults;
    private int selectedSearchResult;

    private View listViewContainer;
    private View searchResultNavigationContainer;
    private TextView currentSearchResultTextView;
    private Button nextResultButton;
    private Button previousResultButton;

    private MenuItem searchAction;
    private SearchOptions searchOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_search_ui);

        // The actual document Uri is provided with the launching intent. You can simply change that
        // inside the CustomSearchUiExample class.
        // This is a check that the example is not accidentally launched without a document Uri.
        final Uri uri = getIntent().getParcelableExtra(EXTRA_URI);
        if (uri == null) {
            showCouldNotStartExample("No document Uri was provided with the launching intent.");
            return;
        }

        // Extract all views from the root layout.
        final Toolbar toolbar = findViewById(R.id.toolbar);
        listViewContainer = findViewById(R.id.searchResultsListContainer);
        final ListView listView = findViewById(R.id.searchResultsListView);
        searchResultNavigationContainer = findViewById(R.id.searchResultNavigationContainer);
        currentSearchResultTextView = findViewById(R.id.currentSearchResultTextView);
        nextResultButton = findViewById(R.id.nextSearchResultButton);
        previousResultButton = findViewById(R.id.previousSearchResultButton);

        if (toolbar == null || listView == null || nextResultButton == null || previousResultButton == null) {
            showCouldNotStartExample("Some of the required views were not inflated.");
            return;
        }

        setSupportActionBar(toolbar);

        nextResultButton.setOnClickListener(v -> selectSearchResultAtIndex(selectedSearchResult + 1));
        previousResultButton.setOnClickListener(v -> selectSearchResultAtIndex(selectedSearchResult - 1));

        // Extract the existing fragment from the layout, or create a new fragment instance if none
        // has been attached yet.
        fragment = (PdfFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (fragment == null) {
            fragment = PdfFragment.newInstance(uri, config);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .commit();
        }

        // Register a listener to retrieve the document as soon as it was loaded, or show an dialog
        // if an error occurred.
        fragment.addDocumentListener(new SimpleDocumentListener() {
            @UiThread
            @Override
            public void onDocumentLoaded(@NonNull PdfDocument loadedDocument) {
                document = loadedDocument;
                textSearch = new TextSearch(loadedDocument, config);
                prepareSearchSplash(findViewById(R.id.empty));
            }

            @Override
            public void onDocumentLoadFailed(@NonNull Throwable exception) {
                Log.e(TAG, "Error while loading the document.", exception);
                showCouldNotStartExample("An exception was thrown while loading the document. See logcat for details.");
            }
        });

        // To show search results on top of the document, a highlighter is used. This class is a
        // PdfDrawableProvider
        // and is registered as such on the fragment. We will set search results on the highlighter,
        // once available.
        highlighter = new SearchResultHighlighter(this);
        fragment.addDrawableProvider(highlighter);

        // Prepare the ListView and SearchResultAdapter which is used to display search results. To
        // make better use of
        // the available screen estate, collapse the soft-keyboard as soon as the user scrolls the
        // list.
        adapter = new SearchResultAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(new OnScrollListenerAdapter() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState != SCROLL_STATE_IDLE) {
                    hideSoftKeyboard();
                }
            }
        });

        searchOptions = new SearchOptions.Builder().snippetLength(40).build();
    }

    private void showCouldNotStartExample(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Could not start example.")
                .setMessage(message)
                .setNegativeButton("Leave example", (dialog, which) -> dialog.dismiss())
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Inflate the action bar menu that will add a search action.
        getMenuInflater().inflate(R.menu.activity_custom_search_view, menu);
        searchAction = menu.findItem(R.id.search);

        final Drawable searchIcon = DrawableCompat.wrap(searchAction.getIcon());
        DrawableCompat.setTint(searchIcon, Color.WHITE);
        searchAction.setIcon(searchIcon);

        final SearchView searchView = (SearchView) searchAction.getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.requestFocus();
        searchView.setQueryHint("Search PDF document...");

        searchAction.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                clearCurrentSearchResults();
                hideSearchResultsList();
                hideSoftKeyboard();
                return true;
            }
        });

        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showSearchResultList();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 2 && textSearch != null) {
                    if (currentSearch != null) {
                        currentSearch.dispose();
                    }

                    currentSearch = textSearch
                            .performSearchAsync(newText, searchOptions)
                            .onErrorResumeNext(throwable -> Flowable.empty())
                            .toList()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(searchResults -> {
                                adapter.setSearchResults(searchResults);

                                final View emptyView = findViewById(R.id.empty);
                                if (emptyView != null && emptyView.getVisibility() != View.INVISIBLE) {
                                    emptyView.setAlpha(1);
                                    emptyView
                                            .animate()
                                            .alpha(0)
                                            .setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationEnd(Animator animation) {
                                                    emptyView.animate().setListener(null);
                                                    emptyView.setVisibility(View.INVISIBLE);
                                                }
                                            })
                                            .start();
                                }
                            });
                } else {
                    final View emptyView = findViewById(R.id.empty);
                    if (emptyView != null && emptyView.getVisibility() != View.VISIBLE) {
                        emptyView.setAlpha(0);
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView
                                .animate()
                                .alpha(1)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        emptyView.animate().setListener(null);
                                        adapter.setSearchResults(null);
                                    }
                                })
                                .start();
                    }
                }

                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = false;

        if (item.getItemId() == R.id.search) {
            item.expandActionView();
            showSearchResultList();
            handled = true;
        }

        return handled || super.onOptionsItemSelected(item);
    }

    /** Called when the user selects a page from the search results list. */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        hideSoftKeyboard();

        //noinspection unchecked
        currentSearchResults = (List<SearchResult>) parent.getAdapter().getItem(position);
        selectedSearchResult = 0;
        updateSearchResultNavigationBar();

        highlighter.setSearchResults(currentSearchResults);
        highlighter.setSelectedSearchResult(currentSearchResults.get(0));
        fragment.setPageIndex(currentSearchResults.get(0).pageIndex, false);

        hideSearchResultsList();
        searchResultNavigationContainer.setVisibility(View.VISIBLE);

        final View contentView = findViewById(android.R.id.content);
        assert contentView != null;
        contentView.requestFocus();
    }

    private void hideSoftKeyboard() {
        final View focusedView = getCurrentFocus();
        if (focusedView != null) {
            Utils.hideKeyboard(focusedView);
        }
    }

    /** Create some fancy looking document statistics as part of the search list's empty view. */
    private void prepareSearchSplash(@Nullable final TextView splashTextView) {
        if (splashTextView == null) return;

        Observable.defer((Supplier<ObservableSource<String>>) () -> {
                    final int pageCount = document.getPageCount();
                    int wordCount = 0;

                    for (int i = 0; i < pageCount; i++) {
                        final String pageText = document.getPageText(i);
                        wordCount += pageText.split("\\w").length;
                    }

                    return Observable.just(getString(R.string.custom_search_ui_splash, pageCount, wordCount));
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    Spanned result = Html.fromHtml(s, 0);
                    splashTextView.setText(result);
                });
    }

    /** Marks the search result at {@code searchResultIndex} as selected. */
    private void selectSearchResultAtIndex(@IntRange(from = 0) final int searchResultIndex) {
        if (searchResultIndex < 0 || searchResultIndex >= currentSearchResults.size()) {
            return;
        }

        if (selectedSearchResult != searchResultIndex) {
            selectedSearchResult = searchResultIndex;
            updateSearchResultNavigationBar();
            highlighter.setSelectedSearchResult(currentSearchResults.get(selectedSearchResult));
        }

        final SearchResult result = currentSearchResults.get(selectedSearchResult);
        if (fragment.getPageIndex() != result.pageIndex) {
            fragment.setPageIndex(result.pageIndex);
        }
    }

    /** Update buttons and text of the on-page search result navigation. */
    private void updateSearchResultNavigationBar() {
        currentSearchResultTextView.setText(
                getString(R.string.currently_selected_result, selectedSearchResult + 1, currentSearchResults.size()));
        previousResultButton.setEnabled(selectedSearchResult > 0);
        nextResultButton.setEnabled(selectedSearchResult < currentSearchResults.size() - 1);
    }

    /** Clear all search results both visually as well as all data. */
    private void clearCurrentSearchResults() {
        highlighter.clearSearchResults();

        if (searchResultNavigationContainer.getVisibility() != View.INVISIBLE) {
            searchResultNavigationContainer
                    .animate()
                    .translationY(-searchResultNavigationContainer.getHeight())
                    .setInterpolator(new AccelerateInterpolator(1.5f))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(final Animator animation) {
                            searchResultNavigationContainer.animate().setListener(null);
                            searchResultNavigationContainer.setTranslationY(0);
                            searchResultNavigationContainer.setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        }
    }

    private void showSearchResultList() {
        if (listViewContainer.getVisibility() != View.VISIBLE) {
            searchAction.setEnabled(false);
            listViewContainer.setVisibility(View.VISIBLE);
            createRevealAnimation(true).start();
        }
    }

    private void hideSearchResultsList() {
        if (listViewContainer.getVisibility() != View.INVISIBLE) {
            final Animator hideAnimator = createRevealAnimation(false);
            hideAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animation.removeListener(this);
                    listViewContainer.setVisibility(View.INVISIBLE);
                    searchAction.setEnabled(true);
                }
            });

            hideAnimator.start();
        }
    }

    private Animator createRevealAnimation(boolean showReveal) {
        final Point screenSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenSize);
        final float screenDiameter = (float) Math.sqrt(screenSize.x * screenSize.x + screenSize.y * screenSize.y);

        final float startRadius, endRadius;
        if (showReveal) {
            startRadius = 0;
            endRadius = screenDiameter;
        } else {
            startRadius = screenDiameter;
            endRadius = 0;
        }

        // Reveal is centered right below the action bar.
        return ViewAnimationUtils.createCircularReveal(listViewContainer, screenSize.x / 2, 0, startRadius, endRadius);
    }

    private static class ViewHolder {
        @NonNull
        public static ViewHolder get(View view, ViewGroup parent) {
            ViewHolder holder;

            if (view != null) {
                holder = (ViewHolder) view.getTag();
            } else {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_custom_search_ui_item, parent, false);
                holder = new ViewHolder(view);
                view.setTag(holder);
            }

            return holder;
        }

        public final View view;
        public final ImageView pagePreviewImageView;
        public final TextView searchResultsCountView;
        public final TextView pageNumberTextView;
        public final TextView previewTextView;
        public Disposable previewRenderSubscription;

        private ViewHolder(View view) {
            this.view = view;
            pagePreviewImageView = view.findViewById(R.id.pagePreviewImageView);
            searchResultsCountView = view.findViewById(R.id.searchResultsCountView);
            pageNumberTextView = view.findViewById(R.id.pageNumberTextView);
            previewTextView = view.findViewById(R.id.previewTextView);
        }
    }

    /**
     * List view adapter for presenting search results, grouped by page (that is one list item per
     * page with results).
     */
    private class SearchResultAdapter extends BaseAdapter {

        private final int previewImageWidth;
        /** List of all search results, grouped by page. */
        @Nullable
        private List<List<SearchResult>> searchResults;

        private SearchResultAdapter(@NonNull final Context context) {
            previewImageWidth =
                    context.getResources().getDimensionPixelSize(R.dimen.custom_search_ui_previewimage_width);
        }

        public void setSearchResults(@Nullable final List<SearchResult> searchResults) {
            if (searchResults == null) {
                this.searchResults = null;
            } else {
                // Group all search results by page.
                final Map<Integer, List<SearchResult>> groupedSearchResults = new HashMap<>();
                for (SearchResult result : searchResults) {
                    if (groupedSearchResults.get(result.pageIndex) == null) {
                        groupedSearchResults.put(result.pageIndex, new ArrayList<>());
                    }
                    groupedSearchResults.get(result.pageIndex).add(result);
                }

                // Store the groups inside a list, to make them accessible using adapter position.
                this.searchResults = new ArrayList<>();
                for (List<SearchResult> resultsOnPage : groupedSearchResults.values()) {
                    this.searchResults.add(resultsOnPage);
                }
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return searchResults == null ? 0 : searchResults.size();
        }

        @Override
        public List<SearchResult> getItem(int position) {
            return searchResults == null ? null : searchResults.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder = ViewHolder.get(convertView, parent);
            final List<SearchResult> item = getItem(position);
            final SearchResult displayedResult = item.get(0);
            final int resultsCount = item.size();
            assert displayedResult.snippet != null;

            if (holder.previewRenderSubscription != null) {
                holder.previewRenderSubscription.dispose();
            }

            // Calculate the size of the rendered preview image.
            final int width = previewImageWidth;
            final int height = calculateBitmapHeight(width, displayedResult.pageIndex);
            holder.previewRenderSubscription = document.renderPageToBitmapAsync(
                            parent.getContext(), displayedResult.pageIndex, width, height)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(holder.pagePreviewImageView::setImageBitmap);

            holder.pageNumberTextView.setText(
                    String.format(Locale.getDefault(), "Page %d", displayedResult.pageIndex + 1));
            holder.searchResultsCountView.setText(
                    getResources().getQuantityString(R.plurals.search_results, resultsCount, resultsCount));

            // Highlight the actual search results phrase.
            final SpannableString previewText = new SpannableString(displayedResult.snippet.text);
            previewText.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    displayedResult.snippet.rangeInSnippet.getStartPosition(),
                    displayedResult.snippet.rangeInSnippet.getEndPosition(),
                    0);
            previewText.setSpan(
                    new BackgroundColorSpan(Color.YELLOW),
                    displayedResult.snippet.rangeInSnippet.getStartPosition(),
                    displayedResult.snippet.rangeInSnippet.getEndPosition(),
                    0);
            holder.previewTextView.setText(previewText);

            return holder.view;
        }

        private int calculateBitmapHeight(final int width, @IntRange(from = 0) final int pageIndex) {
            final Size size = document.getPageSize(pageIndex);
            return (int) (size.height * (width / size.width));
        }
    }
}
