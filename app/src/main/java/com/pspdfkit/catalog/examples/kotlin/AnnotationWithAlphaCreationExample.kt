/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.UiThread
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.FreeTextAnnotation
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.annotations.LineAnnotation
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.annotations.PolygonAnnotation
import com.pspdfkit.annotations.PolylineAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.annotations.StampAnnotation
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.tasks.ExtractAssetTask.OnDocumentExtractedListener
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.preferences.PSPDFKitPreferences
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import java.io.File
import kotlin.getValue

class AnnotationWithAlphaCreationExample(context: Context) : SdkExample(context, R.string.annotationWithAlphaCreationExampleTitle, R.string.annotationWithAlphaCreationExampleDescription) {
    override fun launchExample(
        context: Context,
        configuration: PdfActivityConfiguration.Builder
    ) {
        configuration // Turn off saving, so we have the clean original document every time the example is
            // launched.
            .autosaveEnabled(false)
            .build()

        // The annotation creator written into newly created annotations. If not set, or set to null
        // a dialog will be shown when creating an annotation, asking you to enter a name.
        PSPDFKitPreferences.get(context).setAnnotationCreator("John Doe")

        // Extract the document from the assets. The launched activity will add annotations to that
        // document.
        ExtractAssetTask.extract(
            WELCOME_DOC,
            title,
            context,
            OnDocumentExtractedListener { documentFile: File? ->
                val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(AnnotationWithAlphaCreationActivity::class.java)
                    .build()
                context.startActivity(intent)
            }
        )
    }
}

class AnnotationWithAlphaCreationActivity : PdfActivity() {

    private val viewModel: AnnotationCreationViewModel by viewModels()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, ADD_STAMP_ITEM_ID, 0, "Add Stamp")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == ADD_STAMP_ITEM_ID) {
            createStamp()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        viewModel.createObjects {
            val pageIndex = 0
            createNoteAnnotation(pageIndex)
            createHighlightAnnotation(pageIndex, "PSPDFKit", Color.YELLOW)
            createHighlightAnnotation(pageIndex, "QuickStart", Color.GREEN)
            createFreeTextAnnotation(pageIndex)
            createInkAnnotation(pageIndex)
            createLineAnnotation(pageIndex)
            createPolyline(pageIndex)
            createPolygon(pageIndex)
            createSquareAnnotation(pageIndex)
        }
    }

    private fun createStamp() {
        val document = document ?: return
        val pageIndex = pageIndex
        if (pageIndex < 0) return

        val pageSize = document.getPageSize(pageIndex)
        val halfWidth = pageSize.width / 2
        val halfHeight = pageSize.height / 2
        val rect = RectF(halfWidth - 100, halfHeight + 100, halfWidth + 100, halfHeight - 100)

        val stamp = StampAnnotation(pageIndex, rect, "STAMP_SUBJECT")
        val color = Color.rgb(255, 0, 0)
        stamp.color = color
        stamp.fillColor = Color.rgb(255, 255, 255)
        stamp.alpha = ALPHA

        addAnnotationToPage(stamp)
    }

    private fun createNoteAnnotation(@IntRange(from = 0) pageIndex: Int) {
        val pageRect = RectF(180f, 692f, 212f, 660f)
        val contents = "This is note annotation was created from code."
        val icon = NoteAnnotation.CROSS
        val color = Color.GREEN

        // Create the annotation, and set its properties.
        val noteAnnotation = NoteAnnotation(pageIndex, pageRect, contents, icon)
        noteAnnotation.color = color
        noteAnnotation.alpha = ALPHA

        addAnnotationToPage(noteAnnotation)
    }

    private fun createHighlightAnnotation(
        @IntRange(from = 0) pageIndex: Int,
        highlightedText: String,
        @ColorInt color: Int
    ) {
        val document = document
        // Find the provided text on the current page.
        val textPosition = document!!.getPageText(pageIndex).indexOf(highlightedText)

        if (textPosition >= 0) {
            // To create a text highlight, extract the rects of the text to highlight and pass them
            // to the annotation constructor.
            val textRects =
                document.getPageTextRects(pageIndex, textPosition, highlightedText.length, true)
            val highlightAnnotation = HighlightAnnotation(pageIndex, textRects)

            highlightAnnotation.color = color
            highlightAnnotation.alpha = ALPHA

            addAnnotationToPage(highlightAnnotation)
        } else {
            Toast.makeText(this, "Can't find the text to highlight.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun createInkAnnotation(@IntRange(from = 0) pageIndex: Int) {
        val inkAnnotation = InkAnnotation(pageIndex)
        inkAnnotation.color = Color.rgb(255, 165, 0) // Orange
        inkAnnotation.alpha = ALPHA
        inkAnnotation.lineWidth = 10f

        // Create a line from a list of points.
        val line: MutableList<PointF> = ArrayList()
        var x = 120
        while (x < 720) {
            val y = if (((x % 120) == 0)) 400 else 350
            line.add(PointF(x.toFloat(), y.toFloat()))
            x += 60
        }

        // Ink annotations can hold multiple lines. This example only uses a single line.
        inkAnnotation.lines = listOf<List<PointF>>(line)

        addAnnotationToPage(inkAnnotation)
    }

    private fun createLineAnnotation(@IntRange(from = 0) pageIndex: Int) {
        val lineAnnotation = LineAnnotation(pageIndex, PointF(400f, 100f), PointF(500f, 100f))

        lineAnnotation.color = Color.CYAN
        lineAnnotation.alpha = ALPHA
        lineAnnotation.lineWidth = 20f

        addAnnotationToPage(lineAnnotation)
    }

    private fun createPolyline(@IntRange(from = 0) pageIndex: Int) {
        val polylineAnnotation = PolylineAnnotation(
            pageIndex,
            listOf(PointF(400f, 200f), PointF(500f, 200f), PointF(500f, 150f))
        )

        polylineAnnotation.alpha = ALPHA
        polylineAnnotation.color = Color.RED
        polylineAnnotation.lineWidth = 20f

        addAnnotationToPage(polylineAnnotation)
    }

    private fun createPolygon(@IntRange(from = 0) pageIndex: Int) {
        val polygonAnnotation = PolygonAnnotation(
            pageIndex,
            listOf(PointF(600f, 600f), PointF(600f, 500f), PointF(500f, 550f))
        )

        polygonAnnotation.alpha = ALPHA
        polygonAnnotation.color = Color.RED
        polygonAnnotation.fillColor = Color.BLUE
        polygonAnnotation.lineWidth = 20f

        addAnnotationToPage(polygonAnnotation)
    }

    private fun createSquareAnnotation(@IntRange(from = 0) pageIndex: Int) {
        val squareAnnotation = SquareAnnotation(pageIndex, RectF(0f, 100f, 100f, 0f))

        squareAnnotation.color = Color.RED
        squareAnnotation.fillColor = Color.RED
        squareAnnotation.alpha = ALPHA

        addAnnotationToPage(squareAnnotation)
    }

    private fun createFreeTextAnnotation(@IntRange(from = 0) pageIndex: Int) {
        val contents = "Add text to pages using FreeTextAnnotations"
        val pageRect = RectF(100f, 980f, 320f, 930f)

        val freeTextAnnotation = FreeTextAnnotation(pageIndex, pageRect, contents)
        freeTextAnnotation.color = Color.BLUE
        freeTextAnnotation.alpha = ALPHA
        freeTextAnnotation.textSize = 20f

        addAnnotationToPage(freeTextAnnotation)
    }

    /** Adds annotation to page asynchronously, refreshing rendering in all UI components.  */
    private fun addAnnotationToPage(annotation: Annotation) {
        pdfFragment?.addAnnotationToPage(annotation, false)
    }

    companion object {
        private const val ADD_STAMP_ITEM_ID = 1234
        private const val ALPHA = 0.4f
    }
}
