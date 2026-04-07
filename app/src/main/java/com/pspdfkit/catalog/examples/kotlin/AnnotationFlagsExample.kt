/*
 *   Copyright © 2017-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationFlags
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.listeners.OnPreparePopupToolbarListener
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.toolbar.popup.AnnotationPopupToolbar
import com.pspdfkit.ui.toolbar.popup.PopupToolbarMenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.EnumSet

/**
 * Shows how to use [com.pspdfkit.annotations.AnnotationFlags] to modify annotation
 * characteristics.
 */
class AnnotationFlagsExample(context: Context) :
    SdkExample(
        context,
        R.string.annotationFlagsExampleTitle,
        R.string.annotationFlagsExampleDescription,
    ) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        configuration
            // Turn off saving, so we have the clean original document every time the example is
            // launched.
            .autosaveEnabled(false)
            // Notes are treated as if they had the NOZOOM flag set by default.
            // We override this behavior here by enabling NOZOOM flag handling for note
            // annotations.
            .setEnableNoteAnnotationNoZoomHandling(true)

        // Extract the document from the assets.
        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, title, context) { documentFile ->
            // To start the AnnotationFlagsActivity create a launch intent using the
            // builder.
            val intent =
                PdfActivityIntentBuilder
                    .fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(AnnotationFlagsActivity::class.java)
                    .build()

            context.startActivity(intent)
        }
    }
}

/** This example showcases supported [AnnotationFlags]. */
class AnnotationFlagsActivity :
    PdfActivity(),
    OnPreparePopupToolbarListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfFragment?.setOnPreparePopupToolbarListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, R.id.reset_all_flags, 0, "Reset All Flags")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.reset_all_flags) {
            setFlagsOnAllAnnotations(EnumSet.of(AnnotationFlags.PRINT, AnnotationFlags.NOZOOM))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareAnnotationPopupToolbar(toolbar: AnnotationPopupToolbar) {
        // Only show the flags button for single annotation selection.
        if (toolbar.annotations.size != 1) return

        val items = toolbar.menuItems.toMutableList()
        items.add(PopupToolbarMenuItem(R.id.annotation_flags_popup_button, R.string.annotationFlagsExamplePopupButton))
        toolbar.menuItems = items

        toolbar.setOnPopupToolbarItemClickedListener { item ->
            if (item.id == R.id.annotation_flags_popup_button) {
                showAnnotationFlagsDialog(toolbar.annotations.first())
                true
            } else {
                false
            }
        }
    }

    private fun showAnnotationFlagsDialog(annotation: Annotation) {
        val flagEntries = buildList {
            add(FlagEntry("Read-Only", AnnotationFlags.READONLY))
            add(FlagEntry("Hidden", AnnotationFlags.HIDDEN))
            add(FlagEntry("Print", AnnotationFlags.PRINT))
            add(FlagEntry("No-View", AnnotationFlags.NOVIEW))
            add(FlagEntry("Locked", AnnotationFlags.LOCKED))
            add(FlagEntry("Locked-Contents", AnnotationFlags.LOCKEDCONTENTS))
            if (annotation.type == AnnotationType.NOTE ||
                annotation.type == AnnotationType.FREETEXT ||
                annotation.type == AnnotationType.FILE ||
                annotation.type == AnnotationType.STAMP
            ) {
                add(FlagEntry("No-Zoom", AnnotationFlags.NOZOOM))
            }
        }

        val names = flagEntries.map { it.name }.toTypedArray()
        val checked = flagEntries.map { annotation.hasFlag(it.flag) }.toBooleanArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Annotation Flags")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                setFlagOnAnnotation(annotation, flagEntries[which].flag, isChecked)
            }
            .setPositiveButton("Done", null)
            .show()
    }

    /** Sets/unsets [AnnotationFlags] on single annotation. */
    private fun setFlagOnAnnotation(annotation: Annotation, flag: AnnotationFlags, isSet: Boolean) {
        val flags = annotation.flags
        if (isSet) {
            flags.add(flag)
        } else {
            flags.remove(flag)
        }
        annotation.flags = flags
    }

    /** Set flags on all annotations in document. */
    private fun setFlagsOnAllAnnotations(newFlags: EnumSet<AnnotationFlags>) {
        val doc = document ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val annotations =
                    doc.annotationProvider.getAllAnnotationsOfType(
                        EnumSet.allOf(AnnotationType::class.java),
                    )
                annotations.forEach { annotation -> annotation.flags = newFlags }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to set flags on annotations", e)
            }
        }
    }

    private data class FlagEntry(val name: String, val flag: AnnotationFlags)

    companion object {
        private const val LOG_TAG = "AnnotationFlagsActivity"
    }
}
