/*
 *   Copyright © 2016-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import com.pspdfkit.catalog.examples.java.screenreader.ScreenReader;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;

/** Example activity using a {@link ScreenReader} to read and highlight text on a page. */
public class ScreenReaderExampleActivity extends PdfActivity {

    /** The screen reader encapsulates text-to-speech synthesis and screen highlighting. */
    private ScreenReader screenReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Creating the screen reader will require a callback for when initialization failed.
        // In case of a failure (usually because of a missing TTS engine) the example will be
        // stopped.
        screenReader = new ScreenReader(this, new ScreenReader.OnInitListener() {

            @Override
            public void onInitializationSucceeded() {
                // If the document is ready upon initialization, start reading it.
                if (getDocument() != null) {
                    screenReader.readSentencesOnPage(getDocument(), getPageIndex());
                }
            }

            @Override
            public void onInitializationFailed() {
                new AlertDialog.Builder(ScreenReaderExampleActivity.this)
                        .setTitle("Error")
                        .setMessage(
                                "Could not initiate text-to-speech engine. This may happen if your device does not have a local TTS "
                                        + "engine installed and is not connected to the internet.")
                        .setCancelable(false)
                        .setNeutralButton("Leave example", (dialog, which) -> finish())
                        .show();
            }
        });

        // The screen reader provides a drawable provider that has to be registered on the fragment.
        // It will serve drawables for highlighting text on the document while it is read out loud.
        getPdfFragment().addDrawableProvider(screenReader.getDrawableProvider());
    }

    @Override
    protected void onStop() {
        super.onStop();
        // When the activity goes to the background we stop any running TTS process and clean up
        // drawable providers.
        screenReader.stopReading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getPdfFragment().removeDrawableProvider(screenReader.getDrawableProvider());
        screenReader.shutdown();
        screenReader = null;
    }

    @UiThread
    @Override
    public void onDocumentLoaded(@NonNull PdfDocument document) {
        // If the screen reader is initialized upon loading the document start reading on the
        // current page.
        if (screenReader.isInitialized()) {
            screenReader.readSentencesOnPage(document, getPageIndex());
        }
    }

    /** Every time the page is changed we start reading text on that page. */
    @Override
    public void onPageChanged(@NonNull PdfDocument document, int pageIndex) {
        screenReader.readSentencesOnPage(document, pageIndex);
    }
}
