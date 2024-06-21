/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.document.formatters.XfdfFormatter;
import com.pspdfkit.document.providers.ContentResolverDataProvider;
import com.pspdfkit.ui.PdfActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/** This activity shows how to import and export annotations in XFDF format. */
public class XfdfExampleActivity extends PdfActivity {

    private static final int EXPORT_TO_XFDF_ITEM_ID = 1;
    private static final int IMPORT_FROM_XFDF_ITEM_ID = 2;

    private static final int PICK_EXPORT_FILE_RESULT = 1;
    private static final int PICK_IMPORT_FILE_RESULT = 2;

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, EXPORT_TO_XFDF_ITEM_ID, 0, "Export to XFDF");
        menu.add(0, IMPORT_FROM_XFDF_ITEM_ID, 0, "Import from XFDF");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == EXPORT_TO_XFDF_ITEM_ID) {
            pickFileForXfdfExport();
            return true;
        } else if (item.getItemId() == IMPORT_FROM_XFDF_ITEM_ID) {
            pickFileForXfdfImport();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Fires up {@link Intent#ACTION_CREATE_DOCUMENT} to allows user to pick output file for XFDF
     * export.
     */
    private void pickFileForXfdfExport() {
        if (getDocument() == null) return;

        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // This sets the default name of the output file.
        intent.putExtra(Intent.EXTRA_TITLE, "annotations.xfdf");

        startActivityForResult(intent, PICK_EXPORT_FILE_RESULT);
    }

    /**
     * Fires up {@link Intent#ACTION_OPEN_DOCUMENT} to allows user to pick input file for XFDF
     * import.
     */
    private void pickFileForXfdfImport() {
        if (getDocument() == null) return;

        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, PICK_IMPORT_FILE_RESULT);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != AppCompatActivity.RESULT_OK || data == null) return;
        final Uri uri = data.getData();
        if (uri == null) return;

        switch (requestCode) {
            case PICK_EXPORT_FILE_RESULT:
                exportToXfdf(uri);
                break;
            case PICK_IMPORT_FILE_RESULT:
                importFromXfdf(uri);
                break;
        }
    }

    /**
     * Exports annotations from the first page to the XFDF file.
     *
     * @param uri Uri of the target file for XFDF export.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    private void exportToXfdf(@NonNull final Uri uri) {
        try {
            final OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null) return;

            final Completable writeXfdfCompletable = Completable.fromAction(() -> {
                final PdfDocument document = getDocument();
                final List<Annotation> allAnnotations = document.getAnnotationProvider()
                        .getAllAnnotationsOfTypeAsync(EnumSet.allOf(AnnotationType.class))
                        .toList()
                        .blockingGet();
                XfdfFormatter.writeXfdf(document, allAnnotations, Collections.emptyList(), outputStream);
            });
            writeXfdfCompletable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(outputStream::close)
                    .subscribe(
                            () -> Toast.makeText(
                                            XfdfExampleActivity.this,
                                            "Annotations successfully exported",
                                            Toast.LENGTH_LONG)
                                    .show(),
                            throwable -> Toast.makeText(
                                            XfdfExampleActivity.this, "Annotations export failed", Toast.LENGTH_LONG)
                                    .show());
        } catch (final FileNotFoundException ignored) {
        }
    }

    /**
     * Imports annotations from XFDF file and adds them to the document.
     *
     * @param uri Uri of the XFDF file from which to import annotations.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    private void importFromXfdf(@NonNull final Uri uri) {
        XfdfFormatter.parseXfdfAsync(getDocument(), new ContentResolverDataProvider(uri))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        annotations -> {
                            // Annotations parsed from XFDF are not added to document automatically.
                            // We need to add them manually.
                            for (final Annotation annotation : annotations) {
                                getPdfFragment().addAnnotationToPage(annotation, false);
                            }

                            Toast.makeText(
                                            XfdfExampleActivity.this,
                                            "Annotations successfully imported",
                                            Toast.LENGTH_LONG)
                                    .show();
                        },
                        throwable -> Toast.makeText(
                                        XfdfExampleActivity.this, "Annotations import failed", Toast.LENGTH_LONG)
                                .show());
    }
}
