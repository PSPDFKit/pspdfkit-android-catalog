/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * Showcases how to toggle annotation overlay visibility using
 * [com.pspdfkit.ui.PdfFragment.setAnnotationOverlayEnabled].
 */
class AnnotationOverlayVisibilityExample(context: Context) : SdkExample(
    context,
    R.string.annotationOverlayVisibilityExampleTitle,
    R.string.annotationOverlayVisibilityExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        configuration.layout(R.layout.activity_annotation_overlay_visibility)

        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(AnnotationOverlayVisibilityActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * Activity that displays a PDF with a FAB to toggle annotation overlay visibility.
 */
class AnnotationOverlayVisibilityActivity : PdfActivity() {

    private lateinit var toggleOverlayFab: FloatingActionButton
    private var overlayEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toggleOverlayFab = findViewById(R.id.fab_toggle_overlay)
        updateFabIcon()

        toggleOverlayFab.setOnClickListener {
            overlayEnabled = !overlayEnabled
            requirePdfFragment().setAnnotationOverlayEnabled(overlayEnabled)
            updateFabIcon()
        }
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        // Sync state with fragment once views are ready
        overlayEnabled = requirePdfFragment().isAnnotationOverlayEnabled
        updateFabIcon()
    }

    private fun updateFabIcon() {
        toggleOverlayFab.setImageResource(if (overlayEnabled) R.drawable.ic_show else R.drawable.ic_hide)
    }
}
