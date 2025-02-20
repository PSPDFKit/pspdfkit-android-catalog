/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.sharing.ShareFeatures
import com.pspdfkit.document.formatters.DocumentJsonFormatter
import com.pspdfkit.document.providers.ContentResolverDataProvider
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * This example shows how annotation changes can be exported to the Instant Document JSON format,
 * and how they can be imported. You can launch the example and then modify, remove, or add
 * annotations to the document. When exporting to JSON, all changes to the document will be
 * included. You can always reapply the changes by tapping import, but the example works best if you
 * close the example and reopen it before importing (so the document's original state is restored).
 */
class DocumentJsonExample(context: Context) : SdkExample(context, R.string.documentJsonExampleTitle, R.string.documentJsonExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Disable auto-save so we get the original document when example is opened.
        configuration.autosaveEnabled(false)

        // Clean-up the primary toolbar for the sake of simplicity in this example.
        configuration
            .annotationListEnabled(false)
            .bookmarkListEnabled(false)
            .thumbnailGridEnabled(false)
            .settingsMenuEnabled(false)
            .documentEditorEnabled(false)
            .outlineEnabled(false)
            .printingEnabled(false)
            .setEnabledShareFeatures(ShareFeatures.none())
            .searchEnabled(false)

        // Load and show the a custom activity for importing and exporting document JSON.
        ExtractAssetTask.extract(ANNOTATIONS_EXAMPLE, title, context, false) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .activityClass(DocumentJsonExampleActivity::class)
                .configuration(configuration.build())
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * This activity allows editing of annotations and exporting and importing of changes
 * to an Instant Document JSON file on the external storage.
 */
// We're temporarily suppressing the warning for startActivityForResult being deprecated.
// Issue: https://github.com/PSPDFKit/PSPDFKit/issues/31881
@Suppress("DEPRECATION")
class DocumentJsonExampleActivity : PdfActivity() {

    private val disposables = CompositeDisposable()

    /** Adds import/export actions to the toolbar.  */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.document_json_example, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.import_json -> {
                pickFileForJsonImport()
                true
            }
            R.id.export_json -> {
                pickFileForJsonExport()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Fires up [Intent.ACTION_CREATE_DOCUMENT] to allow user to pick output file for Document JSON export.
     */
    private fun pickFileForJsonExport() {
        if (document == null) return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "application/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        // This sets the default name of the output file.
        intent.putExtra(Intent.EXTRA_TITLE, "document.json")
        startActivityForResult(intent, PICK_EXPORT_FILE_RESULT)
    }

    /**
     * Fires up [Intent.ACTION_OPEN_DOCUMENT] to allow user to pick input file for Document JSON import.
     */
    private fun pickFileForJsonImport() {
        if (document == null) return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "application/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, PICK_IMPORT_FILE_RESULT)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != AppCompatActivity.RESULT_OK) return
        val uri = data?.data ?: return

        when (requestCode) {
            PICK_EXPORT_FILE_RESULT -> exportDocumentJson(uri)
            PICK_IMPORT_FILE_RESULT -> importDocumentJson(uri)
        }
    }

    private fun exportDocumentJson(uri: Uri) {
        val document = document ?: return

        val outputStream = try {
            contentResolver.openOutputStream(uri)
        } catch (e: Throwable) {
            Toast.makeText(this, "Error while opening '$uri' for export. See logcat for more info.", Toast.LENGTH_LONG).show()
            Log.e(LOG_TAG, "Error while opening '$uri' for export", e)
            return
        } ?: run {
            Toast.makeText(this, "Error while opening '$uri' for export.", Toast.LENGTH_LONG).show()
            return
        }

        val exportDocumentDisposable = DocumentJsonFormatter.exportDocumentJsonAsync(document, outputStream)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    showToast("Export successful!.")
                },
                { throwable ->
                    showToast("Error while exporting document JSON. See logcat for more info.")
                    Log.e(LOG_TAG, "Error while exporting document JSON", throwable)
                }
            )
        disposables.add(exportDocumentDisposable)
    }

    private fun importDocumentJson(uri: Uri) {
        val document = document ?: return

        val importDocumentDisposable = DocumentJsonFormatter.importDocumentJsonAsync(document, ContentResolverDataProvider(uri))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    showToast("Import successful!")
                },
                { throwable ->
                    showToast("Error while importing document JSON. See logcat for more info.")
                    Log.e(LOG_TAG, "Error while importing document JSON", throwable)
                }
            )
        disposables.add(importDocumentDisposable)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    companion object {
        private const val PICK_EXPORT_FILE_RESULT = 1
        private const val PICK_IMPORT_FILE_RESULT = 2

        private const val LOG_TAG = "DocumentJsonExample"
    }
}
