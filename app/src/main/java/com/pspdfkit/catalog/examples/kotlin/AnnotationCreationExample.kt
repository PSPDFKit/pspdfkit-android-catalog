/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.UiThread
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.BorderEffect
import com.pspdfkit.annotations.BorderStyle
import com.pspdfkit.annotations.FreeTextAnnotation
import com.pspdfkit.annotations.HighlightAnnotation
import com.pspdfkit.annotations.InkAnnotation
import com.pspdfkit.annotations.LineEndType
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.annotations.SoundAnnotation
import com.pspdfkit.annotations.SquareAnnotation
import com.pspdfkit.annotations.StampAnnotation
import com.pspdfkit.annotations.appearance.AssetAppearanceStreamGenerator
import com.pspdfkit.annotations.sound.AudioExtractor
import com.pspdfkit.annotations.stamps.StampType
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.preferences.PSPDFKitPreferences
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import com.pspdfkit.utils.EdgeInsets
import java.io.IOException

/**
 * This activity shows how to create various annotations programmatically.
 */
class AnnotationCreationExample(context: Context) : SdkExample(context, R.string.annotationCreationExampleTitle, R.string.annotationCreationExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Turn off saving, so we have the clean original document every time the example is launched.
        configuration.autosaveEnabled(false)

        // By default, all supported annotation types are editable.
        // You can selectively enable editing for only certain types by providing them here.
        configuration.editableAnnotationTypes(
            listOf(
                AnnotationType.NOTE,
                AnnotationType.HIGHLIGHT,
                AnnotationType.FREETEXT,
                AnnotationType.STAMP,
                AnnotationType.SQUARE,
                AnnotationType.SOUND
            )
        )

        // You can also specify which annotations tools are enabled. Note that annotation tool will
        // be enabled only when the underlying annotation type (see AnnotationTool.toAnnotationType())
        // is editable.
        configuration.enabledAnnotationTools(
            listOf(
                // This will enable signature tool but won't show ink tool
                // since AnnotationTool.INK is not included in this list.
                AnnotationTool.SIGNATURE,
                AnnotationTool.NOTE,
                AnnotationTool.HIGHLIGHT,
                AnnotationTool.FREETEXT,
                AnnotationTool.SQUARE,
                AnnotationTool.SOUND
            )
        )

        // The annotation creator written into newly created annotations. If not set, or set to `null`
        // a dialog will normally be shown when creating an annotation, asking the user to enter a name.
        // We are going to skip this part and set it as "John Doe" only if it was not yet set.
        if (!PSPDFKitPreferences.get(context).isAnnotationCreatorSet) {
            PSPDFKitPreferences.get(context).setAnnotationCreator("John Doe")
        }

        // Extract the document from the assets. The launched activity will add annotations to that document.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(AnnotationCreationActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * This activity will create multiple annotations on the loaded document to showcase the annotation creation API.
 */
class AnnotationCreationActivity : PdfActivity() {

    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        // We'll create all annotations on the first page of the document.
        val pageIndex = 0

        // Highlight annotations can be used to highlight page text.
        createHighlightAnnotation(pageIndex, "PSPDFKit", Color.YELLOW)
        createHighlightAnnotation(pageIndex, "QuickStart", Color.GREEN)

        // Notes are also supported. These come in 2 variants:
        // Note annotations represent single textual stick it notes represented by an icon displayed on the page.
        createNoteAnnotation(pageIndex)
        // Compared to free-text annotation which text is displayed as part of the page.
        createFreeTextAnnotation(pageIndex)
        // Free-text annotations can also be accompanied with line pointing to content. PDF specification calls these callouts.
        createFreeTextCallout(pageIndex)

        // Ink annotations represent freehand drawing composed of multiple bezier lines.
        // Note that editing of ink annotations is disabled inside the AnnotationCreationExample class.
        // To edit ink annotations, add it to the list of editable types.
        createInkAnnotation(pageIndex)

        // Annotations representing primitive shapes are also supported.
        // These include lines, rectangles (square annotation), ellipses (circle annotation), polylines and polygons.
        createCloudySquareAnnotation(pageIndex)

        // Stamp annotations are simple rectangular annotations.
        createStamp(pageIndex)

        // You can define custom appearance (AP stream) that should be used for the
        // specific annotation instead of their default appearance.
        createStampAnnotationWithCustomApStream(pageIndex)

        // There are also multiple annotation types that can be used to embed more complex data
        // to documents such as sound notes or embedded files.
        createSoundAnnotation(pageIndex)
    }

    private fun createHighlightAnnotation(
        @IntRange(from = 0) pageIndex: Int,
        highlightedText: String,
        @ColorInt color: Int
    ) {
        val document = document ?: return

        // Find the provided text on the current page.
        val textPosition = document.getPageText(pageIndex).indexOf(highlightedText, ignoreCase = true)
        if (textPosition >= 0) {
            // To create a text highlight, extract the rects of the text
            // to highlight and pass them to the annotation constructor.
            val textRects = document.getPageTextRects(pageIndex, textPosition, highlightedText.length, true)

            val highlightAnnotation = HighlightAnnotation(pageIndex, textRects).apply {
                this.color = color
            }

            addAnnotationToDocument(highlightAnnotation)
        } else {
            Toast.makeText(this, "Can't find the text to highlight.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNoteAnnotation(@IntRange(from = 0) pageIndex: Int) {
        val pageRect = RectF(180f, 692f, 212f, 660f)

        val contents = "This is a note annotation created from code."
        val icon = NoteAnnotation.CROSS
        val color = Color.GREEN

        // Create the annotation, and set its color.
        val noteAnnotation = NoteAnnotation(pageIndex, pageRect, contents, icon).apply {
            this.color = color
        }

        addAnnotationToDocument(noteAnnotation)
    }

    private fun createFreeTextAnnotation(@IntRange(from = 0) pageIndex: Int) {
        val contents = "Add text to pages using FreeTextAnnotations"
        val pageRect = RectF(100f, 980f, 320f, 930f)

        val freeTextAnnotation = FreeTextAnnotation(pageIndex, pageRect, contents).apply {
            color = Color.BLUE
            textSize = 20f
        }

        addAnnotationToDocument(freeTextAnnotation)
    }

    private fun createFreeTextCallout(@IntRange(from = 0) pageIndex: Int) {
        val contents = "Call out things using call outs"
        val pageRect = RectF(250f, 100f, 620f, 200f)

        val freeTextAnnotation = FreeTextAnnotation(pageIndex, pageRect, contents).apply {
            color = Color.BLUE
            textSize = 20f
            textInsets = EdgeInsets(0f, 150f, 0f, 0f)

            // Change free-text annotation to callout by setting its intent.
            intent = FreeTextAnnotation.FreeTextAnnotationIntent.FREE_TEXT_CALLOUT

            // We need to specify 2 or 3 callout points.
            callOutPoints = listOf(PointF(255f, 195f), PointF(325f, 150f), PointF(400f, 150f))

            // Callouts can also have borders.
            borderWidth = 1.5f
            borderStyle = BorderStyle.SOLID
            borderColor = Color.BLACK

            // Callout line end can be configured.
            lineEnd = LineEndType.CLOSED_ARROW
        }

        addAnnotationToDocument(freeTextAnnotation)
    }

    private fun createInkAnnotation(@IntRange(from = 0) pageIndex: Int) {
        val inkAnnotation = InkAnnotation(pageIndex).apply {
            color = Color.rgb(255, 165, 0)
            lineWidth = 10f

            // Create a line from a list of points.
            val line: MutableList<PointF> = ArrayList()
            var x = 120
            while (x < 720) {
                val y = if (x % 120 == 0) 400 else 350
                line.add(PointF(x.toFloat(), y.toFloat()))
                x += 60
            }

            // Ink annotations can hold multiple lines. This example only uses a single line.
            lines = listOf(line)
        }

        addAnnotationToDocument(inkAnnotation)
    }

    private fun createCloudySquareAnnotation(pageIndex: Int) {
        val pageRect = RectF(100f, 900f, 320f, 850f)

        val squareAnnotation = SquareAnnotation(pageIndex, pageRect).apply {
            color = Color.RED
            borderEffect = BorderEffect.CLOUDY
            borderEffectIntensity = 3f
        }

        addAnnotationToDocument(squareAnnotation)
    }

    private fun createStamp(@IntRange(from = 0) pageIndex: Int) {
        val document = document ?: return

        // Create stamp in the middle of the page.
        val pageSize = document.getPageSize(pageIndex)
        val halfWidth = pageSize.width / 2
        val halfHeight = pageSize.height / 2
        val rect = RectF(
            halfWidth - 100,
            halfHeight + 100,
            halfWidth + 100,
            halfHeight - 100
        )

        // PSPDFKit ships with multiple pre-built stamp types.
        val stamp = StampAnnotation(pageIndex, rect, StampType.ACCEPTED).apply {
            // Stamp border color.
            color = Color.rgb(255, 0, 0)

            // Stamp fill color.
            fillColor = Color.rgb(255, 255, 255)
        }

        // Stamps with custom text are also supported
        // val stamp = StampAnnotation(pageIndex, rect, "Custom stamp title")

        // As well as image stamps created from Bitmap.
        // val stamp = StampAnnotation(pageIndex, rect, bitmap)

        addAnnotationToDocument(stamp)
    }

    private fun createStampAnnotationWithCustomApStream(pageIndex: Int) {
        // In order for rotation to work properly your stamps with custom AP streams need
        // to match the source aspect ratio exactly. Source logo PDF is 320x360 points big.
        val pageRect = RectF(500f, 980f, 660f, 800f)

        val stampAnnotation = StampAnnotation(pageIndex, pageRect, "Stamp with custom AP stream").apply {
            // Set PDF from assets containing vector logo as annotation's appearance stream generator.
            appearanceStreamGenerator = AssetAppearanceStreamGenerator("images/PSPDFKit_Logo.pdf")
        }

        addAnnotationToDocument(stampAnnotation)
    }

    @SuppressLint("CheckResult")
    private fun createSoundAnnotation(pageIndex: Int) {
        try {
            // Extract first audio track from sample video in assets.
            // Audio extractor supports decoding audio tracks from all media formats that are supported by Android's `MediaExtractor`.
            val audioExtractor = AudioExtractor(this, Uri.parse("file:///android_asset/media/videos/small.mp4"))
            audioExtractor.selectAudioTrack(0)
            audioExtractor.extractAudioTrackAsync().subscribe { embeddedAudioSource ->
                // Create new sound annotation from the extracted audio track.
                val soundAnnotation = SoundAnnotation(pageIndex, RectF(580f, 700f, 600f, 685f), embeddedAudioSource)
                addAnnotationToDocument(soundAnnotation)
            }
        } catch (e: IOException) {
            // Handle possible IOException, thrown when the Uri does not point to correct file/asset.
        }
    }

    /**
     * Add the annotation to the document, and update the annotation in the UI.
     */
    private fun addAnnotationToDocument(annotation: Annotation) {
        // You can add annotation to document and notify PdfFragment to refresh the UI.
        // getDocument().getAnnotationProvider()
        //     .addAnnotationToPageAsync(annotation)
        //     .subscribe(() -> getPdfFragment().notifyAnnotationHasChanged(annotation));

        // Or use the convenience method for adding annotations to page in PdfFragment:
        requirePdfFragment().addAnnotationToPage(annotation, false)
    }
}
