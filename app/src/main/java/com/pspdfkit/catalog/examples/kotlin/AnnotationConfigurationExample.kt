/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import androidx.annotation.UiThread
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.configuration.AnnotationProperty
import com.pspdfkit.annotations.configuration.FreeTextAnnotationConfiguration
import com.pspdfkit.annotations.configuration.InkAnnotationConfiguration
import com.pspdfkit.annotations.configuration.MarkupAnnotationConfiguration
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.fonts.Font
import com.pspdfkit.ui.special_mode.controller.AnnotationTool
import com.pspdfkit.ui.special_mode.controller.AnnotationToolVariant
import java.util.EnumSet

/**
 * This example shows how to change the annotation configuration in a [PdfActivity] or [PdfFragment].
 */
class AnnotationConfigurationExample(context: Context) :
    SdkExample(context, R.string.annotationConfigurationExampleTitle, R.string.annotationConfigurationExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Extract the example document from the app's assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // To start the AnnotationConfigurationExampleActivity create a launch intent using the builder.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(AnnotationConfigurationExampleActivity::class)
                .build()

            context.startActivity(intent)
        }
    }
}

/**
 * Annotation configurations can be used to define the default properties of annotations created
 * with annotation tools inside a [PdfFragment], as well as the available annotation properties that
 * can be modified when creating or editing annotations in the fragment.
 *
 * The annotation configuration is local to a specific [PdfFragment] instance, and therefore has to
 * be configured per [PdfFragment] instance. To change the annotation configuration, it is necessary
 * to extend [PdfActivity] and call the approriate APIs on the hosted fragment.
 */
class AnnotationConfigurationExampleActivity : PdfActivity() {

    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        // Configurations can be defined for any given annotation type...
        configureFreeTextAnnotationType()
        // ... or for particular annotation tools and their tool variants.
        configureInkTool()

        // This example shows how to disable the annotation inspector for highlight annotations.
        configureHighlightWithNoEditableProperties()
    }

    private fun configureFreeTextAnnotationType() {
        // The annotation configuration can be configured through `PdfFragment` for each annotation type.
        requirePdfFragment().annotationConfiguration.put(
            AnnotationType.FREETEXT,
            FreeTextAnnotationConfiguration.builder(this)
                // Configure which color is used when creating free-text annotations.
                .setDefaultColor(Color.rgb(0, 0, 0))
                // Configure which colors are going to be available in the color picker.
                .setAvailableColors(
                    listOf(
                        Color.rgb(255, 255, 255),
                        Color.rgb(224, 224, 224),
                        Color.rgb(158, 158, 158),
                        Color.rgb(66, 66, 66),
                        Color.rgb(0, 0, 0)
                    )
                )
                // Configure the custom font to use
                .setForceDefaults(true)
                .setDefaultFont(Font("Meddon", Typeface.SANS_SERIF))
                // Configure the default text size (in pt).
                .setDefaultTextSize(24f)
                // Only the color property will be editable in the annotation inspector.
                .setSupportedProperties(EnumSet.of(AnnotationProperty.COLOR, AnnotationProperty.FONT))
                // Disable the annotation preview for free-text annotation.
                .setPreviewEnabled(false)
                .build()
        )
    }

    private fun configureInkTool() {
        // In addition to defining configurations per annotation type, you can specify different
        // configurations based on the annotation tool that is used to create annotations. For example,
        // multiple tools exist that can create ink annotations, such as the `AnnotationTool.INK` or
        // the `AnnotationTool.SIGNATURE`, and each of them can have a different configuration which
        // defines default properties, and the set of modifiable properties.
        requirePdfFragment().annotationConfiguration.put(
            AnnotationTool.INK,
            InkAnnotationConfiguration.builder(this)
                // Configure which color is used when creating ink annotations.
                .setDefaultColor(Color.rgb(252, 237, 140))
                // Configure which colors are going to be available in the color picker.
                .setAvailableColors(
                    listOf(
                        Color.rgb(244, 67, 54), // RED
                        Color.rgb(139, 195, 74), // LIGHT GREEN
                        Color.rgb(33, 150, 243), // BLUE
                        Color.rgb(252, 237, 140), // YELLOW
                        Color.rgb(233, 30, 99) // PINK
                    )
                )
                // Configure thickness picker range and default thickness.
                .setDefaultThickness(5f)
                .setMinThickness(1f)
                .setMaxThickness(20f)
                // When `true` attributes like default color are always used as default when
                // creating annotations. When `false` last edited value is used, value from
                // configuration is used only when creating annotation for the first time.
                .setForceDefaults(true)
                .setPreviewEnabled(false)
                .build()
        )

        // Annotation configuration can be also specified for the annotation tool variant.
        requirePdfFragment().annotationConfiguration.put(
            AnnotationTool.INK,
            AnnotationToolVariant.fromPreset(AnnotationToolVariant.Preset.HIGHLIGHTER),
            InkAnnotationConfiguration.builder(this)
                // Configure which color is used when creating ink annotations.
                .setDefaultColor(Color.rgb(252, 237, 140))
                // Configure which colors are going to be available in the color picker.
                .setAvailableColors(
                    listOf(
                        Color.rgb(244, 67, 54), // RED
                        Color.rgb(139, 195, 74), // LIGHT GREEN
                        Color.rgb(33, 150, 243), // BLUE
                        Color.rgb(252, 237, 140), // YELLOW
                        Color.rgb(233, 30, 99) // PINK
                    )
                )
                // Configure thickness picker range and default thickness.
                .setDefaultThickness(5f)
                .setMinThickness(1f)
                .setMaxThickness(20f)
                .setForceDefaults(true)
                .setPreviewEnabled(false)
                .build()
        )
    }

    /**
     * Shows how to disable annotation inspector for highlight annotation.
     */
    private fun configureHighlightWithNoEditableProperties() {
        requirePdfFragment().annotationConfiguration.put(
            AnnotationType.HIGHLIGHT,
            MarkupAnnotationConfiguration.builder(this, AnnotationType.HIGHLIGHT)
                // Makes yellow default highlight color.
                .setDefaultColor(Color.rgb(252, 237, 140))
                // Configure no supported properties. This disables annotation inspector.
                .setSupportedProperties(EnumSet.noneOf(AnnotationProperty::class.java))
                .build()
        )
    }
}
