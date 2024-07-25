/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationProvider.OnAnnotationUpdatedListener
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.utils.Utils
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.drawable.PdfDrawable
import com.pspdfkit.ui.drawable.PdfDrawableProvider

/**
 * Shows how to create a custom annotation note hinter extending [PdfDrawableProvider].
 */
class CustomAnnotationNoteHinterProviderExample(context: Context) : SdkExample(
    context,
    R.string.customAnnotationNoteHinterProviderExampleTitle,
    R.string.customAnnotationNoteHinterProviderExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Disable default annotation hinter provider.
        configuration.setAnnotationNoteHintingEnabled(false)

        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // To start the CustomAnnotationNoteHinterProviderActivity create a launch intent using the builder.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(CustomAnnotationNoteHinterProviderActivity::class)
                .build()

            // Start the activity for the extracted document.
            context.startActivity(intent)
        }
    }
}

class CustomAnnotationNoteHinterProviderActivity : PdfActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val customAnnotationNoteHinter = CustomAnnotationNoteHinter(this)

        // Inject our custom hinter to the framework.
        requirePdfFragment().apply {
            // We need to register custom drawable provider so we can provide our custom hint drawables.
            addDrawableProvider(customAnnotationNoteHinter)

            //  We also want to react to annotation updated events in our hinter.
            requirePdfFragment().addOnAnnotationUpdatedListener(customAnnotationNoteHinter)
        }
    }
}

/**
 * A custom annotation note hinter provider that works only for ink annotations.
 */
private class CustomAnnotationNoteHinter internal constructor(private val pdfActivity: PdfActivity) : PdfDrawableProvider(), OnAnnotationUpdatedListener {
    private val noteIcon: Drawable = ContextCompat.getDrawable(pdfActivity, R.drawable.ic_bookmark)
        ?: throw IllegalStateException("Can't retrieve note drawable from resources.")

    override fun getDrawablesForPage(
        context: Context,
        document: PdfDocument,
        @IntRange(from = 0) pageIndex: Int
    ): List<PdfDrawable> {
        return document.annotationProvider.getAnnotations(pageIndex)
            .asSequence()
            .filter { it.type == AnnotationType.INK }
            .map { NoteInkHinterDrawable(pdfActivity, noteIcon, it) }
            .toList()
    }

    // We notify change to provided drawables whenever any ink annotation changes (is created, updated or removed).
    override fun onAnnotationCreated(annotation: Annotation) = notifyDrawablesChangedIfSupported(annotation)
    override fun onAnnotationUpdated(annotation: Annotation) = notifyDrawablesChangedIfSupported(annotation)
    override fun onAnnotationRemoved(annotation: Annotation) = notifyDrawablesChangedIfSupported(annotation)

    private fun notifyDrawablesChangedIfSupported(annotation: Annotation) {
        if (annotation.type == AnnotationType.INK) {
            notifyDrawablesChanged()
        }
    }

    // There's no need to update note hinter drawables on Annotation Z-order changes. This method is a no-op.
    override fun onAnnotationZOrderChanged(pageIndex: Int, oldOrder: List<Annotation>, newOrder: List<Annotation>) = Unit
}

private class NoteInkHinterDrawable internal constructor(
    private val activity: PdfActivity,
    private val noteIcon: Drawable,
    private val annotation: Annotation
) : PdfDrawable() {
    private val viewBoundingBoxRounded: Rect = Rect()
    private val pdfBoundingBox: RectF = RectF()
    private val viewPoint: PointF = PointF()
    private val viewBoundingBox: RectF = RectF()
    private val widthPx: Int
    private val heightPx: Int
    private val halfWidthPx: Int
    private val halfHeightPx: Int

    init {
        annotation.getBoundingBox(pdfBoundingBox)
        widthPx = Utils.dpToPx(activity, 24)
        heightPx = Utils.dpToPx(activity, 24)
        halfWidthPx = widthPx / 2
        halfHeightPx = heightPx / 2
    }

    override fun draw(canvas: Canvas) {
        invalidateSelf()
        if (TextUtils.isEmpty(annotation.contents)) {
            return
        }
        DrawableCompat.setTint(noteIcon, annotation.color)
        noteIcon.bounds = viewBoundingBoxRounded
        noteIcon.draw(canvas)
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {
        noteIcon.alpha = ALPHA
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        noteIcon.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun updatePdfToViewTransformation(matrix: Matrix) {
        super.updatePdfToViewTransformation(matrix)

        annotation.getBoundingBox(pdfBoundingBox)
        viewPoint.set(pdfBoundingBox.centerX(), pdfBoundingBox.centerY())

        activity.requirePdfFragment().viewProjection.toViewPoint(viewPoint, annotation.pageIndex)

        viewBoundingBox.set(
            viewPoint.x - halfWidthPx,
            viewPoint.y - halfHeightPx,
            viewPoint.x + halfWidthPx,
            viewPoint.y + halfHeightPx
        )
        viewBoundingBox.round(viewBoundingBoxRounded)
    }

    companion object {
        private const val ALPHA = 255
    }
}
