/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.graphics.Color;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import com.pspdfkit.annotations.FileAnnotation;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.document.files.EmbeddedFileSource;
import com.pspdfkit.document.providers.AssetDataProvider;
import com.pspdfkit.ui.PdfActivity;

/** This activity shows how to create file annotations programmatically. */
public class FileAnnotationCreationActivity extends PdfActivity {

    @UiThread
    @Override
    public void onDocumentLoaded(@NonNull final PdfDocument document) {
        final int pageIndex = 0;

        // Create file annotation with embedded pdf file.
        // File description and modification date is optional.
        final FileAnnotation pdfFileAnnotation = new FileAnnotation(
                pageIndex,
                new RectF(180, 692, 212, 660),
                new EmbeddedFileSource(new AssetDataProvider("Annotations.pdf"), "Annotations.pdf", null));
        pdfFileAnnotation.setIconName(FileAnnotation.GRAPH);
        pdfFileAnnotation.setColor(Color.GREEN);
        getPdfFragment().addAnnotationToPage(pdfFileAnnotation, false);

        // Create file annotation with embedded image file.
        final FileAnnotation imageFileAnnotation = new FileAnnotation(
                pageIndex,
                new RectF(244, 692, 276, 660),
                new EmbeddedFileSource(new AssetDataProvider("images/android.png"), "android.png", null));
        imageFileAnnotation.setIconName(FileAnnotation.PAPERCLIP);
        imageFileAnnotation.setColor(Color.BLUE);
        getPdfFragment().addAnnotationToPage(imageFileAnnotation, false);

        // Create file annotation with embedded video file.
        final FileAnnotation videoFileAnnotation = new FileAnnotation(
                pageIndex,
                new RectF(308, 692, 340, 660),
                new EmbeddedFileSource(
                        new AssetDataProvider("media/videos/small.mp4"),
                        "small.mp4",
                        "Example of an annotation with embedded video file."));
        videoFileAnnotation.setIconName(FileAnnotation.PUSH_PIN);
        videoFileAnnotation.setColor(Color.RED);
        getPdfFragment().addAnnotationToPage(videoFileAnnotation, false);

        // File source can also be directly specified as byte array with file's data.
        // In this case we create file annotation from UTF-8 encoded string.
        final FileAnnotation textFileAnnotation = new FileAnnotation(
                pageIndex,
                new RectF(372, 692, 404, 660),
                new EmbeddedFileSource(
                        "Plain text data".getBytes(),
                        "note.txt",
                        "Example of an annotation with embedded plain-text file."));
        textFileAnnotation.setIconName(FileAnnotation.TAG);
        textFileAnnotation.setColor(Color.YELLOW);
        getPdfFragment().addAnnotationToPage(textFileAnnotation, false);
    }
}
