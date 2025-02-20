/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.configuration.activity.PdfActivityConfiguration

/**
 * This example shows how to configure [com.pspdfkit.ui.PdfActivity] to only render a custom set of annotation types.
 */
class AnnotationRenderingExample(context: Context) : AssetExample(context, R.string.annotationRenderingExampleTitle, R.string.annotationRenderingExampleDescription) {

    override val assetPath: String = ANNOTATIONS_EXAMPLE

    override fun prepareConfiguration(configuration: PdfActivityConfiguration.Builder) {
        // List of annotation types that should be excluded from rendering can be set via configuration.
        configuration.excludedAnnotationTypes(listOf(AnnotationType.NOTE, AnnotationType.HIGHLIGHT))
    }
}
