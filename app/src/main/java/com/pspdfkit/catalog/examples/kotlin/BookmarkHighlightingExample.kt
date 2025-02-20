/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.ContextCompat
import com.pspdfkit.bookmarks.Bookmark
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.ThumbnailBarMode
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.drawable.PdfDrawable
import com.pspdfkit.ui.drawable.PdfDrawableProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

/**
 * Example showing how to use the drawable API to put a drawn highlight on all pages that contain a bookmark.
 */
class BookmarkHighlightingExample(private val context: Context) : SdkExample(context, R.string.bookmarkHighlightingExampleTitle, R.string.bookmarkHighlightingExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // This uses larger thumbnails making our bookmark indicator more easily visible.
        configuration
            .setThumbnailBarMode(ThumbnailBarMode.THUMBNAIL_BAR_MODE_SCROLLABLE)
            .title("Bookmark Indicator")

        // Start the activity once the example document has been extracted from the app's assets.
        ExtractAssetTask.extract(BOOKMARK_DOCUMENT, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(BookmarkHighlightingActivity::class)
                .build()

            // Start the BookmarkHighlightingActivity for the extracted document.
            context.startActivity(intent)
        }
    }

    companion object {
        const val BOOKMARK_DOCUMENT = "bookmark_highlight_example.pdf"
    }
}

class BookmarkHighlightingActivity : PdfActivity() {

    /** List of bookmarks that current exist in the document. */
    private val currentBookmarks = mutableListOf<Bookmark>()

    /** Drawable provider that will put a bookmark icon on bookmarked pages. */
    private val drawableProvider = object : PdfDrawableProvider() {
        override fun getDrawablesForPage(context: Context, document: PdfDocument, pageIndex: Int): List<PdfDrawable> {
            if (currentBookmarks.any { it.pageIndex == pageIndex }) {
                // If there's bookmark for the given page index, add our drawable.
                return listOf(BookmarkDrawable(this@BookmarkHighlightingActivity))
            } else {
                // Otherwise we draw nothing.
                return emptyList()
            }
        }
    }

    /** Used to stop the bookmark loading process when closing the activity. */
    private var bookmarkLoadingDisposable: Disposable? = null

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        // When the document is initially loaded we prepare the first set of bookmarks to be displayed.
        bookmarkLoadingDisposable = document
            .bookmarkProvider
            .bookmarksAsync
            .firstOrError()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { bookmarks ->
                updateBookmarkDrawables(bookmarks)
            }

        // Afterwards we subscribe to be updated whenever the list of bookmarks change.
        document.bookmarkProvider.addBookmarkListener {
            // This is always called on the UI thread so no need for special concurrency handling.
            updateBookmarkDrawables(it)
        }
    }

    private fun updateBookmarkDrawables(newBookmarks: List<Bookmark>) {
        currentBookmarks.clear()
        currentBookmarks.addAll(newBookmarks)

        // We need to add and remove the provider so the drawables are correctly redrawn.
        pspdfKitViews.thumbnailBarView?.apply {
            removeDrawableProvider(drawableProvider)
            addDrawableProvider(drawableProvider)
        }
        pspdfKitViews.thumbnailGridView?.apply {
            removeDrawableProvider(drawableProvider)
            addDrawableProvider(drawableProvider)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to dispose of this before closing the activity.
        bookmarkLoadingDisposable?.dispose()
    }
}

/**
 * An implementation of [PdfDrawable], which shows an indicator on bookmarked pages.
 */
private class BookmarkDrawable(private val context: Context) : PdfDrawable() {

    // This has to be non null, otherwise this whole example will do nothing.
    private val bookmarkDrawable: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_bookmark_highlight)!!

    /**
     * This will draw our bookmarkDrawable on to the page.
     */
    @SuppressLint("CanvasSize")
    override fun draw(canvas: Canvas) {
        // First we convert our DP constants to their respective pixel sizes.
        val baseSize = BASE_SIZE_DP * context.resources.displayMetrics.density
        val bookmarkSize = BOOKMARK_SIZE_DP * context.resources.displayMetrics.density
        val margin = MARGIN_DP * context.resources.displayMetrics.density

        // We use the canvas size here since that will be the size of the entire page in the thumbnail bar / grid.
        // Based on this size we calculate the real size our bookmark icon will be.
        val ratio = canvas.width / baseSize
        val effectiveBookmarkSize = bookmarkSize * ratio
        val effectiveMargin = margin * ratio

        // Then we place the bookmark in top right edge, with some margin to the right,
        // and moving it up a bit so it sits flush with the page edge.
        bookmarkDrawable.bounds = Rect(
            (canvas.width - effectiveBookmarkSize - effectiveMargin).toInt(),
            -effectiveMargin.toInt(),
            (canvas.width - effectiveMargin).toInt(),
            effectiveBookmarkSize.toInt()
        )

        // Finally we simply draw it onto the page.
        bookmarkDrawable.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        // Not required.
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // Not required.
    }

    companion object {
        /** The width in DP at which the bookmark icon will have it's size, used to scale depending on context. */
        const val BASE_SIZE_DP = 100

        /** The size of the bookmark icon assuming the size of the canvas is BASE_SIZE_DP. */
        const val BOOKMARK_SIZE_DP = 24

        /** The margin to apply to the bookmark icon. */
        const val MARGIN_DP = 8
    }
}
