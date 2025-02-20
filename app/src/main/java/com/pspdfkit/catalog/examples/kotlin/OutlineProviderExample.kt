/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import com.pspdfkit.annotations.actions.GoToAction
import com.pspdfkit.annotations.actions.UriAction
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.OutlineElement
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.core.Single

/**
 * This example shows how to display a custom outline in [com.pspdfkit.ui.PdfOutlineView].
 */
class OutlineProviderExample(context: Context) :
    SdkExample(context, R.string.outlineProviderExample, R.string.outlineProviderExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(OutlineProviderActivity::class)
                .build()

            // Start the OutlineProviderActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}

/**
 * This subclass of [PdfActivity] sets a custom outline that replaces default document outline.
 */
class OutlineProviderActivity : PdfActivity() {

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        // Set outline provider that returns our custom outline elements on Activity's outline view.
        pspdfKitViews.outlineView?.setDocumentOutlineProvider { Single.just(getCustomOutline(document)) }
    }

    private fun getCustomOutline(document: PdfDocument): List<OutlineElement> {
        val outlineElements = mutableListOf<OutlineElement>()

        // The simplest outline elements is just a title with no action.
        outlineElements.add(OutlineElement.Builder("No Action").build())

        // Outline elements can have children - outline is a tree structure.
        outlineElements.add(
            OutlineElement.Builder("With Children")
                .setChildren(
                    listOf(
                        OutlineElement.Builder("Children 1").build(),
                        OutlineElement.Builder("Children 2").build(),
                        OutlineElement.Builder("Children 3").build()
                    )
                )
                // This property controls whether this element will be expanded or not when shown.
                .setExpanded(true)
                .build()
        )

        // Outline elements can have arbitrary actions.
        outlineElements.add(
            OutlineElement.Builder("Uri Action")
                // Outline text color and style are configurable.
                .setColor(Color.BLUE)
                .setStyle(Typeface.ITALIC)
                // Set action that opens PSPDFKit's website after clicking on the outline element.
                .setAction(UriAction("https://nutrient.io"))
                .build()
        )

        // You can directly create outline elements using their constructor too.
        outlineElements.add(OutlineElement.Builder(document, "Go to page 3", 3).build())
        // This is equivalent to the above outline element.
        outlineElements.add(
            OutlineElement.Builder("Go to page 3")
                .setAction(GoToAction(3))
                // The page label is optional. If set, it will be displayed in addition to title.
                .setPageLabel(document.getPageLabel(3, false))
                .build()
        )

        return outlineElements
    }
}
