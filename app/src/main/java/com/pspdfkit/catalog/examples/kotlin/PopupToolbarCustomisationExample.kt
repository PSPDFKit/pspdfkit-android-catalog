/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.pspdfkit.annotations.NoteAnnotation
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.listeners.OnPreparePopupToolbarListener
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.PopupToolbar
import com.pspdfkit.ui.toolbar.popup.AnnotationPopupToolbar
import com.pspdfkit.ui.toolbar.popup.PopupToolbarMenuItem
import com.pspdfkit.ui.toolbar.popup.TextSelectionPopupToolbar

/**
 * Example showing how to customise all popup toolbar types using [OnPreparePopupToolbarListener].
 */
class PopupToolbarCustomisationExample(context: Context) :
    SdkExample(
        context,
        R.string.popupToolbarCustomisationTitle,
        R.string.popupToolbarCustomisationDescription,
    ) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent =
                PdfActivityIntentBuilder
                    .fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(PopupToolbarCustomisationActivity::class)
                    .build()
            context.startActivity(intent)
        }
    }
}

class PopupToolbarCustomisationActivity :
    PdfActivity(),
    OnPreparePopupToolbarListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfFragment?.setOnPreparePopupToolbarListener(this)
    }

    /**
     * Customise the text selection popup toolbar.
     * Here we remove most items and keep only Copy, Highlight, and a custom "Word Count" action.
     * The custom item uses text only (no icon) to demonstrate text-based menu items.
     */
    override fun onPrepareTextSelectionPopupToolbar(toolbar: TextSelectionPopupToolbar) {
        val keepIds = setOf(
            com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_copy,
            com.pspdfkit.R.id.pspdf__text_selection_toolbar_item_highlight,
        )
        val filtered = toolbar.menuItems.filter { it.id in keepIds }

        // Add a custom text-only item (no icon). Using the two-arg constructor omits the icon,
        // so the toolbar displays the item's title as text instead.
        val wordCountItem = PopupToolbarMenuItem(R.id.popup_custom_word_count, R.string.popupToolbarWordCount)

        toolbar.menuItems = filtered + wordCountItem

        toolbar.setOnPopupToolbarItemClickedListener { item ->
            if (item.id == R.id.popup_custom_word_count) {
                val selectedText = pdfFragment?.textSelection?.text ?: ""
                val count = selectedText.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                Toast.makeText(this, "Word count: $count", Toast.LENGTH_SHORT).show()
                true
            } else {
                // Return false to let the SDK handle default actions (copy, highlight).
                false
            }
        }
    }

    /**
     * Customise the annotation selection popup toolbar.
     * Here we add a custom "Info" button that shows annotation details.
     */
    override fun onPrepareAnnotationPopupToolbar(toolbar: AnnotationPopupToolbar) {
        // Add a custom item with an icon.
        val infoItem = PopupToolbarMenuItem(
            R.id.popup_custom_annotation_info,
            R.string.popupToolbarAnnotationInfo,
            com.pspdfkit.R.drawable.pspdf__ic_info,
            true,
        )

        toolbar.menuItems = toolbar.menuItems + infoItem

        toolbar.setOnPopupToolbarItemClickedListener { item ->
            if (item.id == R.id.popup_custom_annotation_info) {
                val annotations = toolbar.annotations
                val message = annotations.joinToString("\n") { annotation ->
                    "${annotation.type.name} by ${annotation.creator ?: "unknown"}"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                true
            } else {
                false
            }
        }
    }

    /**
     * Customise the long-press popup toolbar.
     * This toolbar appears when long-pressing an empty area. By default it shows
     * "Paste", "Annotate", and "Content Editing" items. Here we add a custom "Add Note" action
     * using text only (no icon) that creates a note annotation at the long-press location.
     */
    override fun onPrepareLongPressPopupToolbar(toolbar: PopupToolbar, pageIndex: Int, pdfPoint: PointF) {
        val addNoteItem = PopupToolbarMenuItem(
            R.id.popup_custom_add_note,
            R.string.popupToolbarAddNote,
        )
        toolbar.menuItems = toolbar.menuItems + addNoteItem

        toolbar.setOnPopupToolbarItemClickedListener { item ->
            if (item.id == R.id.popup_custom_add_note) {
                // Create a note annotation directly at the long-press location.
                val noteSize = 24f
                val noteRect = RectF(
                    pdfPoint.x,
                    pdfPoint.y - noteSize,
                    pdfPoint.x + noteSize,
                    pdfPoint.y,
                )
                val note = NoteAnnotation(pageIndex, noteRect, "New note", NoteAnnotation.COMMENT)
                pdfFragment?.addAnnotationToPage(note, false)
                toolbar.dismiss()
                true
            } else {
                false
            }
        }
    }

    /**
     * Customise the content editing popup toolbar.
     * This toolbar appears when long-pressing in content editing mode. Here we remove the
     * "Clear" action from text block toolbars, and add a custom "Select All" text-only action.
     */
    override fun onPrepareContentEditingPopupToolbar(toolbar: PopupToolbar, pageIndex: Int, pdfPoint: PointF) {
        val removeIds = setOf(
            com.pspdfkit.R.id.pspdf__content_editing_popuptoolbar_clear,
        )
        val filtered = toolbar.menuItems.filter { it.id !in removeIds }

        val selectAllItem = PopupToolbarMenuItem(
            R.id.popup_custom_select_all,
            R.string.popupToolbarSelectAll,
        )

        toolbar.menuItems = filtered + selectAllItem

        toolbar.setOnPopupToolbarItemClickedListener { item ->
            if (item.id == R.id.popup_custom_select_all) {
                Toast.makeText(this, "Select All tapped!", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
    }
}
