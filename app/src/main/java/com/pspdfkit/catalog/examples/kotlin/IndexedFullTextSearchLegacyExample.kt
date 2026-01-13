/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.snackbar.Snackbar
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.TAG
import com.pspdfkit.catalog.utils.Utils
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.library.IndexingOptions
import com.pspdfkit.document.library.PdfLibrary
import com.pspdfkit.document.library.QueryOptions
import com.pspdfkit.document.library.QueryPreviewResult
import com.pspdfkit.document.library.QueryResultListener
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.core.Observable
import java.io.IOException
import java.lang.IllegalStateException
import java.util.Locale

/**
 * This activity showcases [PdfLibrary] by indexing all PDFs of the catalog app, making them
 * searchable.
 * This example uses the legacy [PdfLibrary] API. For a more modern approach, see
 */
class IndexedFullTextSearchLegacyExample(context: Context) : SdkExample(context, R.string.indexedFtsLegacyExampleTitle, R.string.indexedFtsLegacyExampleDescription) {
    override fun launchExample(
        context: Context,
        configuration: PdfActivityConfiguration.Builder
    ) {
        context.startActivity(Intent(context, IndexedFullTextSearchLegacyActivity::class.java))
    }
}

class IndexedFullTextSearchLegacyActivity : AppCompatActivity() {

    companion object {
        /** Name of the Full-text search library file. */
        private const val FTS_SEARCH_LIBRARY_NAME = "fts-library.db"
    }

    /** UI-thread handler for updating the UI from a background thread. */
    private val handler = Handler(Looper.getMainLooper())

    /** List view adapter for displaying search results. */
    private val adapter = SearchResultAdapter()

    /** Contains document paths (relative to the assets) keyed by the document UID. */
    private val indexedDocumentPaths = mutableMapOf<String, String>()

    /** FTS indexing library. */
    private lateinit var library: PdfLibrary

    /** This [Snackbar] is used to show a ongoing indexing process. */
    private var progressIndicator: Snackbar? = null

    /** Search option set by the user, to ignore search results inside the document body. */
    private var ignoreDocumentText = false

    /** Search option set by the user, to ignore search results inside annotations. */
    private var ignoreAnnotations = false

    /** Create the Full-text search indexing library and prepare the search result list view. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fts_indexing)

        // Open the PdfLibrary on a desired location. This example stores the FTS index library
        // inside the app's private database directory.
        try {
            val databaseFile = getDatabasePath(FTS_SEARCH_LIBRARY_NAME)
            if (!databaseFile.mkdirs() && (databaseFile.parentFile?.exists() != true)) {
                Log.w(TAG, "Could not create the FTS indexing database directory.")
            }

            library = PdfLibrary(databaseFile.absolutePath)
        } catch (e: IOException) {
            Log.e(TAG, "Error while creating the FTS library database.", e)
            Toast.makeText(
                this,
                "Could not create FTS library - see logcat for error. Exiting example.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        // Prepare the list view and adapter for displaying search results.
        val searchResultsList = findViewById<ListView>(android.R.id.list)
        searchResultsList.adapter = adapter

        // Create a search result click listener that launches the PdfActivity showing the results.
        searchResultsList.setOnItemClickListener { _, _, position, _ ->
            val clickedSearchResult = adapter.getItem(position)
            val clickedDocumentPath = indexedDocumentPaths[clickedSearchResult.uid]!!
            val dataProvider = AssetDataProvider(clickedDocumentPath)

            // Open the touched search result on the correct page.
            val configuration = PdfActivityConfiguration.Builder(this)
                .page(clickedSearchResult.pageIndex)
                .build()

            val intent = PdfActivityIntentBuilder.fromDataProvider(this, dataProvider)
                .configuration(configuration)
                .build()
            startActivity(intent)
        }

        searchResultsList.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                // Hide the keyboard when scrolling the list.
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL ||
                    scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
                ) {
                    Utils.hideKeyboard(view)
                }
            }

            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                // No-op
            }
        })

        // Initially trigger document indexing.
        performIndexing()
    }

    /** Set up search inside the action bar. */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_fts_indexing, menu)

        val item = menu.findItem(R.id.action_search) ?: throw IllegalStateException("No search menu item found.")
        val searchIcon = DrawableCompat.wrap(item.icon!!)
        DrawableCompat.setTint(searchIcon, Color.BLACK)
        item.icon = searchIcon
        item.expandActionView()

        val searchView = getSearchView(item)

        val checkboxIgnoreDocumentText = menu.findItem(R.id.checkboxIgnoreDocumentText)
        checkboxIgnoreDocumentText.setOnMenuItemClickListener { menuItem ->
            menuItem.isChecked = !menuItem.isChecked
            ignoreDocumentText = menuItem.isChecked
            performSearch(searchView.query.toString())
            true
        }

        val checkboxIgnoreAnnotations = menu.findItem(R.id.checkboxIgnoreAnnotations)
        checkboxIgnoreAnnotations.setOnMenuItemClickListener { menuItem ->
            menuItem.isChecked = !menuItem.isChecked
            ignoreAnnotations = menuItem.isChecked
            performSearch(searchView.query.toString())
            true
        }

        return true
    }

    private fun getSearchView(item: MenuItem): SearchView {
        val searchView = item.actionView as SearchView
        searchView.setIconifiedByDefault(false)
        searchView.requestFocus()
        searchView.queryHint = "Search PDF documents..."

        // Search is started as soon as the user starts writing.
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean = false

            override fun onQueryTextChange(newText: String): Boolean = performSearch(newText)
        })
        return searchView
    }

    /** Runs a search query on all indexed documents. */
    private fun performSearch(query: String): Boolean {
        return if (query.length > 2) {
            library.stopSearch()
            val options = QueryOptions.Builder()
                .ignoreDocumentText(ignoreDocumentText)
                .ignoreAnnotations(ignoreAnnotations)
                .generateTextPreviews(true)
                .build()

            library.search(
                query,
                options,
                object : QueryResultListener {
                    override fun onSearchCompleted(
                        searchString: String,
                        results: Map<String, Set<Int>>
                    ) {
                        Log.d(
                            TAG,
                            "onSearchCompleted() called with: searchString = [$searchString], results = [$results]"
                        )
                    }

                    override fun onSearchPreviewsGenerated(
                        searchString: String,
                        results: Map<String, Set<QueryPreviewResult>>
                    ) {
                        Log.d(
                            TAG,
                            "onSearchPreviewsGenerated() called with: searchString = [$searchString], results = [$results]"
                        )

                        // Search results are returned on a background thread. Post the results
                        // to the adapter, on the main thread.
                        runOnUiThread { adapter.setSearchResults(results) }
                    }
                }
            )
            true
        } else {
            // Clear the search results if the query is too short.
            adapter.setSearchResults(null)
            false
        }
    }

    /** Performs indexing of all available documents in the app's assets. */
    private fun performIndexing() {
        val indexingQueue = mutableListOf<PdfDocument>()

        // This example always clears the FTS index, just for demo purposes. In a real-world app
        // this is not necessary.
        library.clearIndex()

        try {
            // List all top-level assets of the app and filter them for PDF files.
            val assets = Observable.fromArray(*assets.list("")!!)
                .filter { it.endsWith(".pdf") }
                .toList()
                .blockingGet()

            // Open and collect all documents that should be indexed.
            for (asset in assets) {
                try {
                    // Try to open the document and if successful, enqueue it for indexing.
                    val document = PdfDocumentLoader.openDocumentAsync(
                        this,
                        DocumentSource(AssetDataProvider(asset))
                    ).blockingGet()
                    indexingQueue.add(document)

                    // Store the asset's path and name using its UID. This allows us to retrieve the
                    // document later when a search is performed.
                    indexedDocumentPaths[document.uid] = asset
                } catch (ex: Exception) {
                    // This example catches any action that happens while opening the document (e.g.
                    // if a password would be needed).
                    // If an exception is thrown, the document will not be indexed.
                    Log.w(
                        TAG,
                        "Could not open document '$asset' from assets. See exception for reason.",
                        ex
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error while trying to index all catalog app assets.", e)
            Toast.makeText(
                this,
                "Error listing asset files to index - see logcat for detailed error message.",
                Toast.LENGTH_LONG
            ).show()
        }

        if (indexingQueue.isNotEmpty()) {
            // Start the actual document indexing.
            library.enqueueDocuments(indexingQueue, IndexingOptions())

            // Show to the user that indexing is in progress.
            progressIndicator = Snackbar.make(
                findViewById(android.R.id.content),
                "Indexing...",
                Snackbar.LENGTH_INDEFINITE
            )
            progressIndicator?.show()
            scheduleProgressIndicatorUpdate()
        }
    }

    /** Hides progress snackbar as soon as [PdfLibrary] has finished document indexing. */
    private fun scheduleProgressIndicatorUpdate() {
        handler.postDelayed({
            progressIndicator?.let { indicator ->
                if (library.isIndexing) {
                    scheduleProgressIndicatorUpdate()
                } else {
                    indicator.dismiss()
                    progressIndicator = null
                }
            }
        }, 1000)
    }

    private class ViewHolder private constructor(val view: View) {
        val documentTitleTextView: TextView = view.findViewById(R.id.documentTitleTextView)
        val pageNumberTextView: TextView = view.findViewById(R.id.pageNumberTextView)
        val previewTextView: TextView = view.findViewById(R.id.previewTextView)

        companion object {
            fun get(view: View?, parent: ViewGroup): ViewHolder {
                return if (view != null) {
                    view.tag as ViewHolder
                } else {
                    val newView = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fts_result, parent, false)
                    val holder = ViewHolder(newView)
                    newView.tag = holder
                    holder
                }
            }
        }
    }

    /** List view adapter for presenting search results. */
    private inner class SearchResultAdapter : BaseAdapter() {

        private val listItems = mutableListOf<QueryPreviewResult>()

        fun setSearchResults(searchResults: Map<String, Set<QueryPreviewResult>>?) {
            listItems.clear()

            searchResults?.let { results ->
                for (documentUID in results.keys) {
                    results[documentUID]?.let { listItems.addAll(it) }
                }
            }

            notifyDataSetChanged()
        }

        override fun getCount(): Int = listItems.size

        override fun getItem(position: Int): QueryPreviewResult = listItems[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val holder = ViewHolder.get(convertView, parent)
            val item = getItem(position)
            holder.documentTitleTextView.text = indexedDocumentPaths[item.uid]
            holder.pageNumberTextView.text = String.format(
                Locale.getDefault(),
                "Page %d",
                item.pageIndex + 1
            )

            // Highlight the actual search results phrase.
            val highlightedRange = item.rangeInPreviewText
            val previewText = SpannableString(item.previewText)
            previewText.setSpan(
                StyleSpan(Typeface.BOLD),
                highlightedRange.startPosition,
                highlightedRange.endPosition,
                0
            )
            previewText.setSpan(
                BackgroundColorSpan(Color.YELLOW),
                highlightedRange.startPosition,
                highlightedRange.endPosition,
                0
            )
            holder.previewTextView.text = previewText

            return holder.view
        }
    }
}
