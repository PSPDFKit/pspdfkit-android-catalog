/*
 *   Copyright © 2018-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask.extract
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.formatters.XfdfFormatter
import com.pspdfkit.document.providers.ContentResolverDataProvider
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.util.EnumSet

/** Shows how to import and export annotations in XFDF format. */
class XfdfExample(context: Context) :
    SdkExample(
        context,
        R.string.xfdfExampleTitle,
        R.string.xfdfExampleDescription,
    ) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Extract the document from the assets and launch example activity.
        extract(WELCOME_DOC, title, context) { documentFile ->
            val intent =
                PdfActivityIntentBuilder
                    .fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(XfdfExampleActivity::class.java)
                    .build()
            context.startActivity(intent)
        }
    }
}

/** This activity shows how to import and export annotations in XFDF format. */
class XfdfExampleActivity : PdfActivity() {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menu.add(0, EXPORT_TO_XFDF_ITEM_ID, 0, "Export to XFDF")
        menu.add(0, IMPORT_FROM_XFDF_ITEM_ID, 0, "Import from XFDF")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        EXPORT_TO_XFDF_ITEM_ID -> {
            pickFileForXfdfExport()
            true
        }

        IMPORT_FROM_XFDF_ITEM_ID -> {
            pickFileForXfdfImport()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    /**
     * Fires up [Intent.ACTION_CREATE_DOCUMENT] to allows user to pick output file for XFDF
     * export.
     */
    @Suppress("DEPRECATION")
    private fun pickFileForXfdfExport() {
        if (document == null) return

        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "application/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                // This sets the default name of the output file.
                putExtra(Intent.EXTRA_TITLE, "annotations.xfdf")
            }

        startActivityForResult(intent, PICK_EXPORT_FILE_RESULT)
    }

    /**
     * Fires up [Intent.ACTION_OPEN_DOCUMENT] to allows user to pick input file for XFDF
     * import.
     */
    @Suppress("DEPRECATION")
    private fun pickFileForXfdfImport() {
        if (document == null) return

        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "application/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }

        startActivityForResult(intent, PICK_IMPORT_FILE_RESULT)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != AppCompatActivity.RESULT_OK || data == null) return
        val uri = data.data ?: return

        when (requestCode) {
            PICK_EXPORT_FILE_RESULT -> exportToXfdf(uri)
            PICK_IMPORT_FILE_RESULT -> importFromXfdf(uri)
        }
    }

    /**
     * Exports annotations from the first page to the XFDF file.
     *
     * @param uri Uri of the target file for XFDF export.
     */
    private fun exportToXfdf(uri: Uri) {
        try {
            val outputStream = contentResolver.openOutputStream(uri) ?: return

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val doc = document ?: return@launch

                    val allAnnotations =
                        doc.annotationProvider.getAllAnnotationsOfType(
                            EnumSet.allOf(AnnotationType::class.java),
                        )
                    withContext(Dispatchers.IO) {
                        XfdfFormatter.writeXfdf(doc, allAnnotations, emptyList(), outputStream)
                        outputStream.close()
                    }
                    Toast
                        .makeText(
                            this@XfdfExampleActivity,
                            "Annotations successfully exported",
                            Toast.LENGTH_LONG,
                        ).show()
                } catch (e: Exception) {
                    Toast
                        .makeText(
                            this@XfdfExampleActivity,
                            "Annotations export failed",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        } catch (ignored: FileNotFoundException) {
        }
    }

    /**
     * Imports annotations from XFDF file and adds them to the document.
     *
     * @param uri Uri of the XFDF file from which to import annotations.
     */
    private fun importFromXfdf(uri: Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val doc = document ?: return@launch
                val annotations =
                    withContext(Dispatchers.IO) {
                        XfdfFormatter.parseXfdf(doc, ContentResolverDataProvider(uri))
                    }
                // Annotations parsed from XFDF are not added to document automatically.
                // We need to add them manually.
                for (annotation in annotations) {
                    pdfFragment?.addAnnotationToPage(annotation, false)
                }

                Toast
                    .makeText(
                        this@XfdfExampleActivity,
                        "Annotations successfully imported",
                        Toast.LENGTH_LONG,
                    ).show()
            } catch (e: Exception) {
                Toast
                    .makeText(
                        this@XfdfExampleActivity,
                        "Annotations import failed",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    companion object {
        private const val EXPORT_TO_XFDF_ITEM_ID = 1
        private const val IMPORT_FROM_XFDF_ITEM_ID = 2

        private const val PICK_EXPORT_FILE_RESULT = 1
        private const val PICK_IMPORT_FILE_RESULT = 2
    }
}
