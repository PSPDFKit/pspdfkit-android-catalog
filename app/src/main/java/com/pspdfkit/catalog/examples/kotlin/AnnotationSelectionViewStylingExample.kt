/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.theming.AnnotationSelectionViewThemeConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.special_mode.controller.AnnotationSelectionController
import com.pspdfkit.ui.special_mode.manager.AnnotationManager.OnAnnotationSelectedListener

/**
 * This example shows how to customize the annotation selection layout.
 */
class AnnotationSelectionViewStylingExample(context: Context) : SdkExample(context, R.string.annotationSelectionViewStylingExampleTitle, R.string.annotationSelectionViewStylingExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Set the new theme that overrides annotation selection color,
        // scale handle drawables and background drawable.
        configuration.theme(R.style.AnnotationSelectionExample_Theme)

        // Extract the example document from the app's assets.
        ExtractAssetTask.extract("Annotation-Selection.pdf", title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(AnnotationSelectionViewStylingActivity::class)
                .build()

            context.startActivity(intent)
        }
    }
}

/**
 * This activity shows how to customize the annotation selection view programmatically.
 */
class AnnotationSelectionViewStylingActivity : PdfActivity(), OnAnnotationSelectedListener {

    private var collaborate: Drawable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collaborate = AppCompatResources.getDrawable(this, R.drawable.ic_collaborate)
    }

    override fun onDocumentSaved(document: PdfDocument) {
        // When a document is saved it's important to remove
        // the previous listener otherwise setting it again
        // in the `onDocumentLoaded` callback will be a no-op.
        requirePdfFragment().removeOnAnnotationSelectedListener(this)
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        requirePdfFragment().addOnAnnotationSelectedListener(this)
    }

    /**
     * Called immediately before annotation is going to be selected.
     *
     * @param controller Selection controller that is performing the selection.
     * @param annotation Annotation that is going to be selected.
     * @param annotationCreated `true` if the annotation is being created.
     * @return `true` when you want [AnnotationSelectionController] to proceed with the selection. Returning `false`
     * will prevent annotation from being selected.
     */
    override fun onPrepareAnnotationSelection(controller: AnnotationSelectionController, annotation: Annotation, annotationCreated: Boolean): Boolean {
        // Extract current annotation selection view theme configuration.
        val extractedCurrentConfiguration = controller.annotationSelectionViewThemeConfiguration
        // Build a new annotation selection view theme configuration using the current configuration as a base configuration.
        val annotationSelectionViewThemeConfiguration = build(extractedCurrentConfiguration) {
            // Customize the bottom right scale handle drawable depending on the annotation type.
            if (annotation.type == AnnotationType.STAMP) {
                setRotationHandleDrawable(null)
                setBottomRightScaleHandleDrawable(collaborate)
            }
        }
        // Apply the new configuration to the annotation selection view.
        controller.annotationSelectionViewThemeConfiguration = annotationSelectionViewThemeConfiguration
        return true
    }

    override fun onAnnotationSelected(annotation: Annotation, annotationCreated: Boolean) {
        // Not used.
    }
}

/**
 * Returns an [AnnotationSelectionViewThemeConfiguration] with all options supplied to the build block, e.g:
 * ```
 * val annotationSelectionViewThemeConfiguration = AnnotationSelectionViewThemeConfiguration.build() {
 *     setSelectionPadding(8)
 * }
 * ```
 */
private inline fun build(block: AnnotationSelectionViewThemeConfiguration.Builder.() -> Unit) = AnnotationSelectionViewThemeConfiguration.Builder().apply(block).build()

/**
 * Returns an [AnnotationSelectionViewThemeConfiguration] from an existing annotation view theme configuration
 * with all options supplied to the build block, e.g:
 * ```
 * val annotationSelectionViewThemeConfiguration = AnnotationSelectionViewThemeConfiguration.build(extractedCurrentConfiguration) {
 *     setSelectionPadding(8)
 * }
 * ```
 */
private inline fun build(
    annotationSelectionViewThemeConfiguration: AnnotationSelectionViewThemeConfiguration,
    block: AnnotationSelectionViewThemeConfiguration.Builder.() -> Unit
) = AnnotationSelectionViewThemeConfiguration.Builder(annotationSelectionViewThemeConfiguration).apply(block).build()
