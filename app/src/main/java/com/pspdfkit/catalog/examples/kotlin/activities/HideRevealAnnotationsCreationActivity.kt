/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.activities

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IntRange
import androidx.annotation.UiThread
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.drawable.PdfDrawable
import com.pspdfkit.ui.drawable.PdfDrawableProvider
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar
import com.pspdfkit.ui.toolbar.ContextualToolbar
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener
import com.pspdfkit.utils.Size
import org.json.JSONObject
import java.util.EnumSet

class HideRevealAnnotationsCreationActivity :
    PdfActivity(),
    OnContextualToolbarLifecycleListener {
    /**
     * A square annotation representing an area to hide on the page. Once the document is loaded,
     * we extract any existing hide area annotation from the document and store its reference here. When no
     * hide annotation is in the document, this reference holds `null`.
     */
    private var hideArea: SquareAnnotation? = null

    /**
     * A square annotation representing an area to reveal on the page. Once the document is loaded,
     * we extract any existing reveal annotation from the document and store its reference here. When no
     * reveal annotation is in the document, this reference holds `null`.
     */
    private var revealArea: SquareAnnotation? = null

    /**
     * To hide the entire page around the reveal annotation, we use a custom drawable provider that serves
     * drawables to the [PdfFragment]. We keep reference to it, so we can remove the drawable provider upon removing
     * the reveal annotation from the document.
     */
    private var revealAreaDrawableProvider: PdfDrawableProvider? = null

    /** This drawable implements the drawing logic for covering the page in black. */
    private var revealAreaDrawable: RevealAreaDrawable? = null

    /**
     * Create the two buttons for handling hide and reveal areas.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        // Loading the document is an asynchronous process. Depending on the lifecycle state of the activity,
        // the document might not be loaded when this method is called, in which case we don't populate the menu.
        if (document == null) {
            return false
        }

        menu.clear()
        // Set hide button state.
        if (hideArea == null) {
            // Annotation is not present.
            val addHideAreaItem = menu.add(0, HIDE_ITEM_ID, 0, "Hide Area")
            showWithText(addHideAreaItem)
        } else {
            // Annotation is present.
            val resetHideAreaItem = menu.add(0, RESET_HIDE_ITEM_ID, 0, "Reset Hide Area")
            showWithText(resetHideAreaItem)
        }

        // Set reveal button state.
        if (revealArea == null) {
            // Annotation is not present.
            val addRevealAreaItem = menu.add(0, REVEAL_ITEM_ID, 1, "Reveal Area")
            showWithText(addRevealAreaItem)
        } else {
            // Annotation is present.
            val resetRevealAreaItem = menu.add(0, RESET_REVEAL_ITEM_ID, 1, "Reset Reveal Area")
            showWithText(resetRevealAreaItem)
        }
        return true
    }

    /** Helper to show buttons with text. */
    private fun showWithText(menuItem: MenuItem) {
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT)
    }

    /** Set the corresponding action for every button in the toolbar. */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            HIDE_ITEM_ID -> {
                hideArea()
                true
            }
            RESET_HIDE_ITEM_ID -> {
                resetHideArea()
                true
            }
            REVEAL_ITEM_ID -> {
                revealArea()
                true
            }
            RESET_REVEAL_ITEM_ID -> {
                resetRevealArea()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // For the example, we deactivate the annotation editing toolbar, i.e. the toolbar that is shown when
        // selecting an annotation on the page. We do this by registering a contextual toolbar listener that hides
        // the annotation editing toolbar, whenever it would become visible.
        setOnContextualToolbarLifecycleListener(this)
    }

    /**
     * Called whenever a toolbar is about to become visible. We use this callback to hide the annotation editing toolbar
     * when selecting reveal and hide areas. This is part of the example, and should be changed as needed.
     */
    override fun onPrepareContextualToolbar(toolbar: ContextualToolbar<*>) {
        (toolbar as? AnnotationEditingToolbar)?.visibility = View.GONE
    }

    /** Adds a drawable provider which will serve the reveal area drawable for covering the page in black. */
    private fun addRevealAreaDrawableProvider(revealArea: SquareAnnotation?) {
        if (revealArea != null) {
            revealAreaDrawableProvider = object : PdfDrawableProvider() {
                override fun getDrawablesForPage(context: Context, document: PdfDocument, @IntRange(from = 0) pageIndex: Int): List<PdfDrawable> {
                    return mutableListOf<PdfDrawable>().apply {
                        if (pageIndex == revealArea.pageIndex) {
                            val drawable = RevealAreaDrawable(document.getPageSize(pageIndex), revealArea)
                            revealAreaDrawable = drawable
                            add(drawable)
                        }
                    }
                }
            }
            revealAreaDrawableProvider?.let {
                requirePdfFragment().addDrawableProvider(it)
            }
        }
    }

    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        // Restore the state of previously added annotations if any.
        if (hideArea == null) {
            hideArea = getCustomAnnotationIfPresent(document, HIDE_AREA_KEY)
        }
        if (revealArea == null) {
            revealArea = getCustomAnnotationIfPresent(document, REVEAL_AREA_KEY)
            addRevealAreaDrawableProvider(revealArea)
        }
        invalidateOptionsMenu()
    }

    /** Returns any annotation with the `customData` key set to `true`, or null if no such annotation exists. */
    private fun getCustomAnnotationIfPresent(document: PdfDocument, customData: String): SquareAnnotation? {
        document.annotationProvider.getAllAnnotationsOfType(EnumSet.of(AnnotationType.SQUARE)).forEach {
            if (it.customData?.optBoolean(customData) == true) {
                return it as SquareAnnotation
            }
        }

        return null
    }

    @SuppressLint("Range")
    private fun hideArea() {
        // Initial rect for the hide area annotation.
        val rect = RectF(360f, 632.5f, 561f, 80.5f)
        SquareAnnotation(pageIndex, rect).apply {
            hideArea = this
            fillColor = Color.BLACK
            customData = JSONObject().apply {
                put(HIDE_AREA_KEY, true)
            }
            addAnnotationToDocument(this)
            invalidateOptionsMenu()
        }
    }

    private fun resetHideArea() {
        val document = document ?: return
        val hideArea = hideArea ?: return
        document.annotationProvider.removeAnnotationFromPage(hideArea)
        this.hideArea = null
        invalidateOptionsMenu()
    }

    @SuppressLint("Range")
    private fun revealArea() {
        // Initial rect for the reveal area annotation.
        val rect = RectF(51f, 630f, 360f, 462f)
        SquareAnnotation(pageIndex, rect).apply {
            revealArea = this
            fillColor = Color.TRANSPARENT
            customData = JSONObject().apply {
                put(REVEAL_AREA_KEY, true)
            }
            addAnnotationToDocument(this)
            addRevealAreaDrawableProvider(this)
            invalidateOptionsMenu()
        }
    }

    private fun resetRevealArea() {
        val document = document ?: return
        val revealArea = revealArea ?: return
        revealAreaDrawableProvider?. let {
            requirePdfFragment().removeDrawableProvider(it)
            val thumbnailGridView = pspdfKitViews.thumbnailGridView
            thumbnailGridView?.removeDrawableProvider(it)
            revealAreaDrawableProvider = null
        }
        document.annotationProvider.removeAnnotationFromPage(revealArea)
        this.revealArea = null
        invalidateOptionsMenu()
    }

    /**
     * Whenever there's a touch event and the reveal area is selected, we turn it transparent for
     * simpler annotation placement.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val revealArea = revealArea
        val revealAreaDrawable = revealAreaDrawable
        if (revealArea != null && revealAreaDrawable != null) {
            if (ev.actionMasked == MotionEvent.ACTION_UP) {
                // Set it fully opaque.
                revealAreaDrawable.alpha = 255
            } else if (ev.actionMasked == MotionEvent.ACTION_DOWN && requirePdfFragment().selectedAnnotations.contains(revealArea)) {
                // Set alpha channel at 80%.
                revealAreaDrawable.alpha = 204
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Add the annotation to the document, and update the annotation in the UI.
     */
    private fun addAnnotationToDocument(annotation: Annotation) {
        requirePdfFragment().addAnnotationToPage(annotation, false)
    }

    /** Custom drawable that implements the logic for covering the area around the reveal area in black. */
    internal class RevealAreaDrawable(pageSize: Size, private val revealArea: Annotation) : PdfDrawable() {
        private val paint = Paint()
        private val pageCoordinates: RectF = RectF(0f, pageSize.height, pageSize.width, 0f)

        /** Auxiliary field used for keeping the screen coordinates of a page. */
        private val screenCoordinates = RectF()

        /** Auxiliary field used for keeping the screen coordinates of an annotation. */
        private val annotationScreenCoordinates = RectF()

        /** Auxiliary field used for keeping the page coordinates of an annotation. */
        private val annotationPageCoordinates = RectF()

        init {
            paint.color = Color.BLACK
            paint.style = Paint.Style.FILL
            paint.alpha = 255
        }

        /**
         * The annotation bounding box is first decreased to a narrower rectangle to make sure
         * there are no gaps in the black area. The rectangle is then converted from page coordinates
         * to screen coordinates and as a final step four black rectangles will be drawn around the
         * reveal area that will cover the whole page to give a reveal area effect.
         */
        override fun draw(canvas: Canvas) {
            revealArea.getBoundingBox(annotationPageCoordinates)
            // Decrease the transparent hole to make sure there are no gaps.
            // Using small values to make the rectangle narrower.
            // The `dy` value is negative because the annotation is in page coordinates.
            annotationPageCoordinates.inset(1f, -1f)
            getPdfToPageTransformation().mapRect(annotationScreenCoordinates, annotationPageCoordinates)
            val bounds = bounds
            // Left.
            canvas.drawRect(
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                annotationScreenCoordinates.left,
                bounds.bottom.toFloat(),
                paint
            )
            // Top.
            canvas.drawRect(
                annotationScreenCoordinates.left,
                bounds.top.toFloat(),
                annotationScreenCoordinates.right,
                annotationScreenCoordinates.top,
                paint
            )
            // Right
            canvas.drawRect(
                annotationScreenCoordinates.right,
                bounds.top.toFloat(),
                bounds.right.toFloat(),
                bounds.bottom.toFloat(),
                paint
            )
            // Bottom
            canvas.drawRect(
                annotationScreenCoordinates.left,
                annotationScreenCoordinates.bottom,
                annotationScreenCoordinates.right,
                bounds.bottom.toFloat(),
                paint
            )
        }

        /**
         * PSPDFKit calls this method every time the page was moved or resized on screen.
         * It will provide a fresh transformation for calculating screen coordinates from
         * PDF coordinates.
         */
        override fun updatePdfToViewTransformation(matrix: Matrix) {
            super.updatePdfToViewTransformation(matrix)
            updateScreenCoordinates()
        }

        private fun updateScreenCoordinates() {
            // Calculate the screen coordinates by applying the PDF-to-view transformation.
            getPdfToPageTransformation().mapRect(screenCoordinates, pageCoordinates)
            // Rounding out ensure no clipping of content.
            val bounds = bounds
            screenCoordinates.roundOut(bounds)
            setBounds(bounds)
        }

        @UiThread
        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
            // Drawable invalidation is only allowed from a UI-thread.
            invalidateSelf()
        }

        @UiThread
        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
            // Drawable invalidation is only allowed from a UI-thread.
            invalidateSelf()
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int {
            return PixelFormat.TRANSLUCENT
        }
    }

    // These methods are part of the [ContextualToolbarLifecycleListener] interface, but are not required for this example.
    override fun onDisplayContextualToolbar(toolbar: ContextualToolbar<*>) = Unit
    override fun onRemoveContextualToolbar(toolbar: ContextualToolbar<*>) = Unit

    companion object {
        private const val HIDE_ITEM_ID = 1234
        private const val RESET_HIDE_ITEM_ID = 1235
        private const val REVEAL_ITEM_ID = 1236
        private const val RESET_REVEAL_ITEM_ID = 1237
        private const val HIDE_AREA_KEY = "hideArea"
        private const val REVEAL_AREA_KEY = "revealArea"
    }
}
