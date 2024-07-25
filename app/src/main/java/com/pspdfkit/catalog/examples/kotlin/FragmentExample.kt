/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IntRange
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.LinkAnnotation
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.annotations.actions.ActionType
import com.pspdfkit.annotations.actions.UriAction
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSaveOptions
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.search.SearchResult
import com.pspdfkit.listeners.DocumentListener
import com.pspdfkit.listeners.OnDocumentLongPressListener
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.PdfOutlineView
import com.pspdfkit.ui.PdfThumbnailBar
import com.pspdfkit.ui.PdfThumbnailGrid
import com.pspdfkit.ui.outline.DefaultBookmarkAdapter
import com.pspdfkit.ui.outline.DefaultOutlineViewListener
import com.pspdfkit.ui.search.PdfSearchViewModular
import com.pspdfkit.ui.search.SearchResultHighlighter
import com.pspdfkit.ui.search.SimpleSearchResultListener
import com.pspdfkit.utils.PdfUtils
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * This example shows how to build a custom activity using the [PdfFragment] together
 * with some of the PSPDFKit views.
 */
class FragmentExample(context: Context) : SdkExample(context, R.string.fragmentExampleTitle, R.string.fragmentExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = Intent(context, CustomFragmentActivity::class.java)

            // We pass the Uri for the PDF file that should be opened in `PdfFragment` via Intent extra.
            intent.putExtra(CustomFragmentActivity.EXTRA_URI, Uri.fromFile(documentFile))

            // We pass the `PdfFragment` configuration via another extra.
            intent.putExtra(
                CustomFragmentActivity.EXTRA_CONFIGURATION,
                configuration.build().configuration
            )

            context.startActivity(intent)
        }
    }
}

/**
 * Custom activity that integrates [PdfFragment] together with some of the most important PSPDFKit views.
 */
class CustomFragmentActivity : AppCompatActivity(), DocumentListener, OnDocumentLongPressListener {

    private lateinit var fragment: PdfFragment
    private lateinit var thumbnailBar: PdfThumbnailBar
    private lateinit var configuration: PdfConfiguration
    private lateinit var modularSearchView: PdfSearchViewModular
    private lateinit var thumbnailGrid: PdfThumbnailGrid
    private lateinit var highlighter: SearchResultHighlighter
    private lateinit var pdfOutlineView: PdfOutlineView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_custom_fragment)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        modularSearchView.isDisplayed -> {
                            modularSearchView.hide()
                            return
                        }
                        thumbnailGrid.isDisplayed -> {
                            thumbnailGrid.hide()
                            return
                        }
                        pdfOutlineView.isDisplayed -> {
                            pdfOutlineView.hide()
                            return
                        }
                        else -> finish()
                    }
                }
            }
        )

        // Get the Uri provided when launching the activity.
        val documentUri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)
            ?: throw IllegalStateException("Activity Intent was missing Uri extra!")

        // Get the configuration from the provided Intent.
        configuration = intent.getSupportParcelableExtra(EXTRA_CONFIGURATION, PdfConfiguration::class.java)
            ?: throw IllegalStateException("Activity Intent was missing configuration extra!")

        // Extract the existing fragment from the layout. The fragment only exist if it has been created
        // previously (this could happen when the activity is recreated due to configuration change).
        fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as PdfFragment?
            // If no fragment was found, create a new one providing it with the configuration and document Uri.
            ?: run {
                val newFragment = PdfFragment.newInstance(documentUri, configuration)
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newFragment)
                    .commit()
                return@run newFragment
            }

        // Initialize all PSPDFKit UI components.
        initModularSearchViewAndButton()
        initOutlineViewAndButton()
        initThumbnailBar()
        initThumbnailGridAndButton()

        // Wire up fragment with this custom activity and all UI components.
        fragment.apply {
            addDocumentListener(this@CustomFragmentActivity)
            addDocumentListener(modularSearchView)
            addDocumentListener(thumbnailBar.documentListener)
            addDocumentListener(thumbnailGrid)
            addDocumentListener(pdfOutlineView.documentListener)
            setOnDocumentLongPressListener(this@CustomFragmentActivity)
        }
    }

    private fun initThumbnailGridAndButton() {
        thumbnailGrid = findViewById(R.id.thumbnailGrid)
            ?: throw IllegalStateException("Error while loading CustomFragmentActivity. The example layout was missing the thumbnail grid view.")

        thumbnailGrid.setOnPageClickListener { view, pageIndex ->
            fragment.pageIndex = pageIndex
            view.hide()
        }

        // The thumbnail grid is hidden by default. Set up a click listener to show it.
        val openThumbnailGridButton = findViewById<ImageView>(R.id.openThumbnailGridButton)
            ?: throw IllegalStateException(
                "Error while loading CustomFragmentActivity. The example layout" +
                    " was missing the open thumbnail grid button with id `R.id.openThumbnailGridButton`."
            )

        openThumbnailGridButton.apply {
            setImageDrawable(
                tintDrawable(
                    openThumbnailGridButton.drawable,
                    ContextCompat.getColor(this@CustomFragmentActivity, R.color.white)
                )
            )
            setOnClickListener {
                if (thumbnailGrid.isShown) thumbnailGrid.hide() else thumbnailGrid.show()
            }
        }
    }

    private fun initThumbnailBar() {
        thumbnailBar = findViewById(R.id.thumbnailBar)
            ?: throw IllegalStateException("Error while loading CustomFragmentActivity. The example layout was missing thumbnail bar view.")

        thumbnailBar.setOnPageChangedListener { _, pageIndex: Int -> fragment.pageIndex = pageIndex }
    }

    private fun initOutlineViewAndButton() {
        pdfOutlineView = findViewById(R.id.outlineView)
            ?: throw IllegalStateException("Error while loading CustomFragmentActivity. The example layout was missing the outline view.")

        pdfOutlineView.apply {
            val outlineViewListener = DefaultOutlineViewListener(fragment)
            setOnAnnotationTapListener(outlineViewListener)
            setOnOutlineElementTapListener(outlineViewListener)
            setBookmarkAdapter(DefaultBookmarkAdapter(fragment))
            fragment.addDocumentListener(pdfOutlineView.documentListener)
        }

        val openOutlineButton = findViewById<ImageView>(R.id.openOutlineButton)
            ?: throw IllegalStateException(
                "Error while loading CustomFragmentActivity. The example layout " +
                    "was missing the open outline view button with id `R.id.openOutlineButton`."
            )

        openOutlineButton.apply {
            setImageDrawable(
                tintDrawable(
                    openOutlineButton.drawable,
                    ContextCompat.getColor(this@CustomFragmentActivity, R.color.white)
                )
            )
            setOnClickListener {
                if (pdfOutlineView.isShown) pdfOutlineView.hide() else pdfOutlineView.show()
            }
        }
    }

    private fun initModularSearchViewAndButton() {
        // The search result highlighter will highlight any selected result.
        highlighter = SearchResultHighlighter(this).also {
            fragment.addDrawableProvider(it)
        }

        modularSearchView = findViewById(R.id.modularSearchView)
            ?: throw IllegalStateException("Error while loading CustomFragmentActivity. The example layout was missing the search view.")

        modularSearchView.setSearchViewListener(object : SimpleSearchResultListener() {
            override fun onMoreSearchResults(results: List<SearchResult>) {
                highlighter.addSearchResults(results)
            }

            override fun onSearchCleared() {
                highlighter.clearSearchResults()
            }

            override fun onSearchResultSelected(result: SearchResult?) {
                // Pass on the search result to the highlighter. If 'null' the highlighter will clear any selection.
                highlighter.setSelectedSearchResult(result)
                if (result != null) {
                    fragment.scrollTo(PdfUtils.createPdfRectUnion(result.textBlock.pageRects), result.pageIndex, 250, false)
                }
            }
        })

        // The search view is hidden by default (see layout). Set up a click listener that will show the view once pressed.
        val openSearchButton = findViewById<ImageView>(R.id.openSearchButton)
            ?: throw IllegalStateException(
                "Error while loading CustomFragmentActivity. The example layout " +
                    "was missing the open search button with id `R.id.openSearchButton`."
            )

        openSearchButton.apply {
            setImageDrawable(
                tintDrawable(
                    drawable,
                    ContextCompat.getColor(this@CustomFragmentActivity, R.color.white)
                )
            )
            setOnClickListener {
                if (modularSearchView.isShown) modularSearchView.hide() else modularSearchView.show()
            }
        }
    }

    private fun createNoteAnnotation() {
        val pageRect = RectF(180f, 692f, 212f, 660f)
        val contents = "This is note annotation was created from code."
        val icon = NoteAnnotation.CROSS
        val color = Color.GREEN

        // Create the annotation, and set the color.
        NoteAnnotation(1, pageRect, contents, icon).also { noteAnnotation ->
            noteAnnotation.color = color
            fragment.addAnnotationToPage(noteAnnotation, false)
        }
    }

    /**
     * This method binds the thumbnail bar and the search view to the fragment, once the document is loaded.
     */
    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        fragment.addDocumentListener(modularSearchView)
        thumbnailBar.setDocument(document, configuration)
        modularSearchView.setDocument(document, configuration)
        pdfOutlineView.setDocument(document, configuration)
        thumbnailGrid.setDocument(document, configuration)

        // Adding note annotation to populate Annotation section in PdfOutlineView
        createNoteAnnotation()
    }

    override fun onDocumentLongPress(
        document: PdfDocument,
        @IntRange(from = 0) pageIndex: Int,
        event: MotionEvent?,
        pagePosition: PointF?,
        longPressedAnnotation: Annotation?
    ): Boolean {
        // This code showcases how to handle long click gesture on the document links.
        fragment.view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        if (longPressedAnnotation is LinkAnnotation) {
            val action = longPressedAnnotation.action
            if (action?.type == ActionType.URI) {
                val uri = (action as UriAction).uri ?: return true
                Toast.makeText(this@CustomFragmentActivity, uri, Toast.LENGTH_LONG).show()
                return true
            }
        }
        return false
    }

    // Rest of the `DocumentListener` methods are unused.
    override fun onDocumentLoadFailed(exception: Throwable) = Unit

    override fun onDocumentSave(document: PdfDocument, saveOptions: DocumentSaveOptions): Boolean = true

    override fun onDocumentSaved(document: PdfDocument) = Unit

    override fun onDocumentSaveFailed(document: PdfDocument, exception: Throwable) = Unit

    override fun onDocumentSaveCancelled(document: PdfDocument) = Unit

    override fun onPageClick(
        document: PdfDocument,
        @IntRange(from = 0) pageIndex: Int,
        event: MotionEvent?,
        pagePosition: PointF?,
        clickedAnnotation: Annotation?
    ): Boolean = false

    override fun onDocumentClick(): Boolean = false

    override fun onPageChanged(document: PdfDocument, @IntRange(from = 0) pageIndex: Int) = Unit

    override fun onDocumentZoomed(document: PdfDocument, @IntRange(from = 0) pageIndex: Int, scaleFactor: Float) = Unit

    override fun onPageUpdated(document: PdfDocument, @IntRange(from = 0) pageIndex: Int) = Unit

    /**
     * Applies the `tint` color to the given `drawable`.
     */
    private fun tintDrawable(drawable: Drawable, tint: Int): Drawable {
        val tintedDrawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTint(tintedDrawable, tint)
        return tintedDrawable
    }

    companion object {
        const val EXTRA_CONFIGURATION = "CustomFragmentActivity.EXTRA_CONFIGURATION"
        const val EXTRA_URI = "CustomFragmentActivity.EXTRA_URI"
    }
}
