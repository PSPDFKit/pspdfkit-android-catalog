/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.examples.java.activities.FormsJavaScriptActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.processor.NewPage;
import com.pspdfkit.document.processor.PdfProcessor;
import com.pspdfkit.document.processor.PdfProcessorTask;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;

/** This example showcases forms JavaScript functionality. */
public class FormsJavaScriptExample extends SdkExample {

    private static final String LOG_TAG = "FormsJavaScriptExample";
    private static final String PSPDFKIT_DIRECTORY_NAME = "catalog-pspdfkit";

    public FormsJavaScriptExample(@NonNull Context context) {
        super(
                context.getString(R.string.javaScriptFormsExampleTitle),
                context.getString(R.string.javaScriptFormsExampleDescription));
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        configuration
                // JavaScript is enabled by default. It can be disabled in configuration.
                .setJavaScriptEnabled(true)
                // Turn off saving, so we have the clean original document every time the example is
                // launched.
                .autosaveEnabled(false);

        // Create an empty document from scratch with multiple pages.
        NewPage blankPage = NewPage.emptyPage(NewPage.PAGE_SIZE_A4).build();
        final PdfProcessorTask task = PdfProcessorTask.empty();

        // Create multiple blank pages - we'll create example form fields on these pages.
        for (int i = 0; i < 4; ++i) {
            task.addNewPage(blankPage, i);
        }

        final File outputFile;
        try {
            outputFile = new File(getCatalogCacheDirectory(context), "FormsJavaScriptExample.pdf").getCanonicalFile();
        } catch (IOException exception) {
            throw new IllegalStateException("Couldn't create FormsJavaScriptExample.pdf file.", exception);
        }

        PdfProcessor.processDocumentAsync(task, outputFile)
                .ignoreElements()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(outputFile))
                                    .configuration(configuration.build())
                                    .activityClass(FormsJavaScriptActivity.class)
                                    .build();

                            context.startActivity(intent);
                        },
                        throwable -> Log.e(LOG_TAG, "Error while trying to create PDF document.", throwable));
    }

    @NonNull
    private static File getCatalogCacheDirectory(@NonNull Context ctx) throws IOException {
        File dir = new File(ctx.getCacheDir(), PSPDFKIT_DIRECTORY_NAME);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Failed to create Catalog cache directory.");
            }
        }
        return dir;
    }
}
