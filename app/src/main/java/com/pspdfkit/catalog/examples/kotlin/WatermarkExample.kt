/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import androidx.annotation.IntRange
import androidx.annotation.UiThread
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.examples.java.activities.WatermarkExampleActivity
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.PdfThumbnailBar
import com.pspdfkit.ui.PdfThumbnailGrid
import com.pspdfkit.ui.drawable.PdfDrawable
import com.pspdfkit.ui.drawable.PdfDrawableProvider

/**
 * This example shows how to create page watermarks using Drawable API.
 */
class WatermarkExample(context: Context) : SdkExample(context, R.string.watermarkExampleTitle, R.string.watermarkExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = PdfActivityIntentBuilder.fromDataProvider(context, AssetDataProvider(QUICK_START_GUIDE))
            .configuration(configuration.build())
            .activityClass(WatermarkExampleActivity::class.java)
            .build()
        context.startActivity(intent)
    }
}

/**
 * Example activity showing how to draw custom drawables on the [PdfFragment],
 * the [PdfThumbnailGrid], and the [PdfThumbnailBar] using Drawable API.
 */
class WatermarkExampleActivity : PdfActivity() {

    /**
     * Drawable provider that provides example watermarks.
     */
    private val customTestDrawableProvider: PdfDrawableProvider = object : PdfDrawableProvider() {
        override fun getDrawablesForPage(context: Context, document: PdfDocument, @IntRange(from = 0) pageIndex: Int): List<PdfDrawable> {
            return listOf(
                // Text watermark, tilted by 45 degrees with a bottom-left corner at (350, 350) in PDF coordinates.
                WatermarkDrawable("Watermark", PointF(350f, 350f)),
                // Simple watermark consisting of 2 square shapes.
                TwoSquaresDrawable(RectF(0f, 400f, 400f, 0f))
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register our drawable provider on `PdfFragment` to provide drawables to document pages...
        requirePdfFragment().addDrawableProvider(customTestDrawableProvider)

        // Also register drawable provider on thumbnail bar, thumbnail grid.
        pspdfKitViews.thumbnailBarView?.addDrawableProvider(customTestDrawableProvider)
        pspdfKitViews.thumbnailGridView?.addDrawableProvider(customTestDrawableProvider)

        // Outline displays page previews in bookmarks list. Bookmarks are enabled in this example
        // so we need to register our drawable provider on the outline view too.
        pspdfKitViews.outlineView?.addDrawableProvider(customTestDrawableProvider)
    }
}

/**
 * This is a simple Kotlin extension function on [android.graphics.Rect]
 * so we can have a fluent API for converting to [RectF].
 */
private fun Rect.toRectF(): RectF {
    return RectF(this)
}

/**
 * An implementation of [PdfDrawable], which shows two semi-transparent squares in the bottom-left
 * corner of the page.
 */
private class TwoSquaresDrawable(private val pageCoordinates: RectF) : PdfDrawable() {

    private val redPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        alpha = 50
    }

    private val bluePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        alpha = 50
    }

    private val screenCoordinates = RectF()

    /**
     * This method performs all the drawing required by this drawable.
     * Keep this method fast to maintain performant UI.
     */
    override fun draw(canvas: Canvas) {
        val bounds = bounds.toRectF()
        canvas.drawRect(
            bounds.left,
            bounds.top,
            bounds.right - bounds.width() / 2f,
            bounds.bottom - bounds.height() / 2f,
            redPaint
        )
        canvas.drawRect(
            bounds.left + bounds.width() / 2f,
            bounds.top + bounds.height() / 2f,
            bounds.right,
            bounds.bottom,
            bluePaint
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

        // Rounding out to ensure that content does not clip.
        val bounds = Rect()
        screenCoordinates.roundOut(bounds)
        this.bounds = bounds
    }

    @UiThread
    override fun setAlpha(alpha: Int) {
        bluePaint.alpha = alpha
        redPaint.alpha = alpha
        invalidateSelf()
    }

    @UiThread
    override fun setColorFilter(colorFilter: ColorFilter?) {
        bluePaint.colorFilter = colorFilter
        redPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}

/**
 * An implementation of [PdfDrawable], which shows a semi-transparent text tilted by 45 degrees.
 */
private class WatermarkDrawable(private val text: String, startingPoint: PointF) : PdfDrawable() {

    private val redPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        alpha = 50
        textSize = 100f
    }

    private val pageCoordinates = RectF()
    private val screenCoordinates = RectF()

    init {
        calculatePageCoordinates(text, startingPoint)
    }

    private fun calculatePageCoordinates(text: String, point: PointF) {
        val textBounds = Rect()
        redPaint.getTextBounds(text, 0, text.length, textBounds)
        pageCoordinates.set(
            point.x,
            point.y + textBounds.height().toFloat(),
            point.x + textBounds.width().toFloat(),
            point.y
        )
    }

    private fun updateScreenCoordinates() {
        getPdfToPageTransformation().mapRect(screenCoordinates, pageCoordinates)

        // Rounding out ensure no clipping of content.
        val bounds = bounds
        screenCoordinates.roundOut(bounds)
        this.bounds = bounds
    }

    /**
     * This method performs all the drawing required by this drawable.
     * Keep this method fast to maintain performant UI.
     */
    override fun draw(canvas: Canvas) {
        val bounds = bounds.toRectF()
        canvas.save()

        // Rotate canvas by 45 degrees.
        canvas.rotate(-45f, bounds.left, bounds.bottom)
        // Recalculate text size to much new bounds.
        setTextSizeForWidth(redPaint, bounds.width(), text)
        // Draw the text on rotated canvas.
        canvas.drawText(text, bounds.left, bounds.bottom, redPaint)

        canvas.restore()
    }

    private fun setTextSizeForWidth(
        paint: Paint,
        desiredWidth: Float,
        text: String
    ) {
        // Pick a reasonably large value for the test.
        val testTextSize = 60f

        // Get the bounds of the text, using our testTextSize.
        paint.textSize = testTextSize

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        // Calculate the desired size as a proportion of our testTextSize.
        val desiredTextSize = testTextSize * desiredWidth / bounds.width()

        // Set the paint for that size.
        paint.textSize = desiredTextSize
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

    @UiThread
    override fun setAlpha(alpha: Int) {
        redPaint.alpha = alpha
        invalidateSelf()
    }

    @UiThread
    override fun setColorFilter(colorFilter: ColorFilter?) {
        redPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}
