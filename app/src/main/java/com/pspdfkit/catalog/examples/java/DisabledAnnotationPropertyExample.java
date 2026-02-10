/*
 *   Copyright © 2019-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.annotations.configuration.AnnotationConfiguration;
import com.pspdfkit.annotations.configuration.AnnotationConfigurationRegistry;
import com.pspdfkit.annotations.configuration.AnnotationProperty;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import java.util.Arrays;
import java.util.List;

/**
 * Showcases how to change {@link AnnotationConfiguration} in order to disable annotation note
 * option in the annotation editing toolbar.
 */
public class DisabledAnnotationPropertyExample extends SdkExample {
    public DisabledAnnotationPropertyExample(@NonNull final Context context) {
        super(
                context,
                R.string.disabledAnnotationPropertyExampleTitle,
                R.string.disabledAnnotationPropertyExampleDescription);
    }

    @Override
    public void launchExample(@NonNull Context context, @NonNull PdfActivityConfiguration.Builder configuration) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(WELCOME_DOC, getTitle(), context, documentFile -> {
            // To start the CustomAnnotationDefaultsManagerActivity we create a launch
            // intent using the builder.
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(DisabledAnnotationPropertyActivity.class)
                    .build();

            context.startActivity(intent);
        });
    }

    /**
     * Shows how to use {@link AnnotationConfiguration} to disable annotation note option from the
     * annotation editing toolbar by removing the {@link AnnotationProperty#ANNOTATION_NOTE} from the
     * supported properties.
     */
    public static class DisabledAnnotationPropertyActivity extends PdfActivity {

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
}
