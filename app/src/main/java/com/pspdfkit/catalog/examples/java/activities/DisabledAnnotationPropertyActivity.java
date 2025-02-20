/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.annotations.configuration.AnnotationConfiguration;
import com.pspdfkit.annotations.configuration.AnnotationConfigurationRegistry;
import com.pspdfkit.annotations.configuration.AnnotationProperty;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;
import java.util.Arrays;
import java.util.List;

/**
 * Shows how to use {@link AnnotationConfiguration} to disable annotation note option from the
 * annotation editing toolbar by removing the {@link AnnotationProperty#ANNOTATION_NOTE} from the
 * supported properties.
 */
public class DisabledAnnotationPropertyActivity extends PdfActivity {

    @UiThread
    @Override
    public void onDocumentLoaded(@NonNull PdfDocument document) {
        super.onDocumentLoaded(document);

        //noinspection ConstantConditions - When a document is successfully loaded fragment won't be
        // null.
        AnnotationConfigurationRegistry annotationConfigurationRegistry =
                getPdfFragment().getAnnotationConfiguration();

        List<AnnotationType> annotationTypesWithNotes = Arrays.asList(
                AnnotationType.INK,
                AnnotationType.LINE,
                AnnotationType.POLYLINE,
                AnnotationType.SQUARE,
                AnnotationType.CIRCLE,
                AnnotationType.POLYGON,
                AnnotationType.FREETEXT,
                AnnotationType.UNDERLINE,
                AnnotationType.SQUIGGLY,
                AnnotationType.STRIKEOUT,
                AnnotationType.HIGHLIGHT,
                AnnotationType.STAMP,
                AnnotationType.FILE,
                AnnotationType.REDACT);

        // Disable ANNOTATION_NOTE property for every annotation type that supports notes.
        for (AnnotationType annotationType : annotationTypesWithNotes) {
            annotationConfigurationRegistry.put(
                    annotationType,
                    AnnotationConfiguration.builder(this, annotationType)
                            .disableProperty(AnnotationProperty.ANNOTATION_NOTE)
                            .build());
        }
    }
}
