/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.ui.DocumentPickerActivity
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity

/**
 * Exercises the SDK's native memory-pressure handling on heavy documents, with the full
 * [PdfActivity] UI (including the thumbnail bar) active.
 *
 * Under sustained memory pressure the SDK sends critical memory notifications into core,
 * which trims page caches and aborts in-flight page-content parses; an aborted page backs
 * off (blank, no auto-retry) and is re-rendered automatically once memory recovers. A
 * rasterisation that already started is allowed to finish. If pressure persists despite
 * the automatic recovery, [com.pspdfkit.listeners.OnSustainedMemoryPressureListener]
 * fires (once per episode) so the host can offer to close the document — demonstrated
 * here with a dialog.
 *
 * Open a large document and scroll through its high-resolution pages to drive the device
 * into real memory pressure, then watch the SDK trim caches, blank and re-render pages, and
 * — if pressure persists — fire the host advisory dialog. Observe with:
 * `adb logcat -s PSPDF.MemTrace MemoryHandling DocumentProvider PSPDF.MemoryNotHandler`.
 */
class MemoryPressureExample(context: Context) :
    SdkExample(
        context,
        R.string.memoryPressureExampleTitle,
        R.string.memoryPressureExampleDescription,
    ) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = Intent(context, MemoryPressureExamplePickerActivity::class.java)
        intent.putExtra(DocumentPickerActivity.EXTRA_CONFIGURATION, configuration.build())
        context.startActivity(intent)
    }
}

/** Activity that lets the user choose between picking a PDF and using the default document. */
class MemoryPressureExamplePickerActivity : DocumentPickerActivity() {
    override val targetActivityClass = MemoryPressureActivity::class.java
}

class MemoryPressureActivity : PdfActivity() {
    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)
        // The advisory only fires when critical pressure persists across several memory
        // polls despite the SDK's automatic recovery — transient episodes stay silent.
        // The recommended host response is to release the document.
        pdfFragment?.setOnSustainedMemoryPressureListener {
            AlertDialog.Builder(this)
                .setTitle("Device is low on memory")
                .setMessage(
                    "This document needs more memory than the device can sustain. Some pages may " +
                        "stay blank until memory recovers. Closing the document is recommended.",
                )
                .setPositiveButton("Close document") { _, _ -> finish() }
                .setNegativeButton("Keep viewing", null)
                .show()
        }
    }
}
