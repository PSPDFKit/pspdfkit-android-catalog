/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.ui.DocumentPickerActivity
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Simple example that shows how to zoom to page annotations, using the [PdfFragment.zoomTo] method.
 */
class ZoomExample(context: Context) : SdkExample(context, R.string.zoomExampleTitle, R.string.zoomExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // This example uses a custom activity which adds some option menu items.
        // The default menu items are deactivated for simplicity.
        configuration.searchEnabled(false)
            .outlineEnabled(false)
            .thumbnailGridEnabled(false)

        // Launch the picker activity to let users choose between default or custom document.
        val intent = Intent(context, ZoomExamplePickerActivity::class.java)
        intent.putExtra(DocumentPickerActivity.EXTRA_CONFIGURATION, configuration.build())
        context.startActivity(intent)
    }
}

/**
 * Activity that lets the user choose between picking a PDF and using the default document.
 */
class ZoomExamplePickerActivity : DocumentPickerActivity() {
    override val targetActivityClass = ZoomExampleActivity::class.java
}

/**
 * This example shows how to zoom/animate between annotations of a document using the [PdfFragment.zoomTo] method.
 */
class ZoomExampleActivity : PdfActivity() {
    companion object {
        /** Padding around annotation bounding box used when zooming. */
        private const val ANNOTATION_BOUNDING_BOX_PADDING_PX = 16
    }

    /**
     * This list holds all annotations of the loaded document. It is populated in onDocumentLoaded().
     */
    private val documentAnnotations = mutableListOf<Annotation>()

    /** This holds reference to the currently zoomed annotation. */
    private var currentAnnotation: Annotation? = null

    private var annotationLoadingJob: Job? = null

    private val viewModel: AnnotationCreationViewModel by viewModels()

    /**
     * Once the document is loaded, we extract all the annotations and put them into our list.
     * That way we can easily move forth and back between the annotations.
     */
    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        viewModel.createObjects {
            annotationLoadingJob = lifecycleScope.launch {
                val annotations = withContext(Dispatchers.IO) {
                    document.annotationProvider.getAllAnnotationsOfType(
                        AnnotationType.entries.toSet()
                    )
                }
                documentAnnotations.addAll(annotations)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cancel the annotation loading job.
        annotationLoadingJob?.cancel()
        annotationLoadingJob = null
    }

    /**
     * Creates our custom navigation menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // It's important to call super before inflating the
        // custom menu, or the custom menu won't be shown.
        super.onCreateOptionsMenu(menu)

        // Inflate our custom menu items, for navigation between annotations.
        menuInflater.inflate(R.menu.activity_zoom_example, menu)

        return true
    }

    /**
     * Handles clicks on the navigation option menu items.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nextAnnotation -> {
                zoomToNextAnnotation()
                true
            }
            R.id.previousAnnotation -> {
                zoomToPreviousAnnotation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Called when pressing the "next annotation" button. Finds the next annotation and issues a call
     * to [PdfFragment.zoomTo].
     */
    private fun zoomToNextAnnotation() {
        if (documentAnnotations.isEmpty()) return

        var currentAnnotation = this.currentAnnotation
        val currentAnnotationIndex = if (currentAnnotation == null) {
            -1
        } else {
            documentAnnotations.indexOf(currentAnnotation)
        }
        val nextAnnotationIndex = min(currentAnnotationIndex + 1, documentAnnotations.size - 1)

        if (nextAnnotationIndex != currentAnnotationIndex) {
            currentAnnotation = documentAnnotations[nextAnnotationIndex]
            this.currentAnnotation = currentAnnotation

            val boundingBox = currentAnnotation.boundingBox
            boundingBox.inset(-ANNOTATION_BOUNDING_BOX_PADDING_PX.toFloat(), -ANNOTATION_BOUNDING_BOX_PADDING_PX.toFloat())

            requirePdfFragment().zoomTo(boundingBox, currentAnnotation.pageIndex, 300)
        }
    }

    /**
     * Called when pressing the "previous annotation" button. Finds the previous annotation and issues a call
     * to [PdfFragment.zoomTo].
     */
    private fun zoomToPreviousAnnotation() {
        if (documentAnnotations.isEmpty()) return

        var currentAnnotation = this.currentAnnotation ?: return

        val currentAnnotationIndex = documentAnnotations.indexOf(currentAnnotation)
        val nextAnnotationIndex = max(currentAnnotationIndex - 1, 0)
        if (nextAnnotationIndex != currentAnnotationIndex) {
            currentAnnotation = documentAnnotations[nextAnnotationIndex]
            this.currentAnnotation = currentAnnotation

            val boundingBox = currentAnnotation.boundingBox
            boundingBox.inset(-ANNOTATION_BOUNDING_BOX_PADDING_PX.toFloat(), -ANNOTATION_BOUNDING_BOX_PADDING_PX.toFloat())
            requirePdfFragment().zoomTo(boundingBox, currentAnnotation.pageIndex, 300)
        }
    }
}
