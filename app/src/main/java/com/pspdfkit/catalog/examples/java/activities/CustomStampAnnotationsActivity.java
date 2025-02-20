/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.AnnotationProvider;
import com.pspdfkit.annotations.AnnotationType;
import com.pspdfkit.annotations.appearance.AppearanceStreamGenerator;
import com.pspdfkit.annotations.appearance.AssetAppearanceStreamGenerator;
import com.pspdfkit.annotations.configuration.StampAnnotationConfiguration;
import com.pspdfkit.annotations.stamps.CustomStampAppearanceStreamGenerator;
import com.pspdfkit.annotations.stamps.PredefinedStampType;
import com.pspdfkit.annotations.stamps.StampPickerItem;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows how to implement custom set of annotations for the `StampPickerDialog`.
 */
public class CustomStampAnnotationsActivity extends PdfActivity {

    @NonNull
    private final String CUSTOM_AP_STREAM_SUBJECT = "CustomApStream";

    @NonNull
    private final CustomStampAppearanceStreamGenerator customStampAppearanceStreamGenerator =
            new CustomStampAppearanceStreamGenerator();

    /**
     * Appearance stream generator instance set via {@link
     * Annotation#setAppearanceStreamGenerator(AppearanceStreamGenerator)} is not retained after
     * reloading the document. Custom appearance stream generated for stamps created via stamp
     * picker item created in {@link #createCustomApStreamStampItem} will be retained in he reloaded
     * document (and in other PDF viewers), however its appearance will be regenerated with default
     * appearance stream after the annotation is modified. This has to be called on the main thread.
     *
     * <p>We register an instance of {@link CustomStampAppearanceStreamGenerator} on {@link
     * AnnotationProvider} to regenerate proper appearance for modified stamps based on their custom
     * subject.
     *
     * @param document Loaded document instance.
     */
    @UiThread
    @Override
    public void onDocumentLoaded(@NonNull PdfDocument document) {
        super.onDocumentLoaded(document);
        document.getAnnotationProvider().addAppearanceStreamGenerator(customStampAppearanceStreamGenerator);
        if (getPdfFragment() != null) {
            configureStampAnnotationsDefaults();
        }
    }

    /**
     * Shows how to configure annotation defaults and control which properties are going to be
     * displayed in annotation inspector.
     */
    private void configureStampAnnotationsDefaults() {
        // Create list of stamps that are going to be added to the picker.
        final List<StampPickerItem> items = new ArrayList<>();

        // Adding built in stamps.
        items.add(StampPickerItem.fromPredefinedType(this, PredefinedStampType.ACCEPTED)
                .build());
        items.add(StampPickerItem.fromPredefinedType(this, PredefinedStampType.REJECTED)
                .build());

        // Adding custom subject stamps.
        items.add(StampPickerItem.fromTitle(this, "Great!").build());
        items.add(StampPickerItem.fromTitle(this, "Stamp!").build());

        // Adding custom subject stamps with date-time subtext.
        items.add(StampPickerItem.fromTitle(this, "Like")
                .withDateTimeSubtitle(true, true)
                .build());

        // Adding custom bitmap based stamps.
        StampPickerItem bitmapStampItem = createBitmapStampItem();
        if (bitmapStampItem != null) {
            items.add(bitmapStampItem);
        }

        // Adding custom AP stream based stamps.
        items.add(createCustomApStreamStampItem());

        // Defaults can be configured through PdfFragment for each annotation type.
        getPdfFragment()
                .getAnnotationConfiguration()
                .put(
                        AnnotationType.STAMP,
                        StampAnnotationConfiguration.builder(this)
                                // Here you return list of stamp picker items that are going to be
                                // available in the stamp picker.
                                .setAvailableStampPickerItems(items)
                                .build());
    }

    /** This example shows how to create stamp picker item from bitmap image. */
    @Nullable
    private StampPickerItem createBitmapStampItem() {
        try {
            final Bitmap bitmap = BitmapFactory.decodeStream(getAssets().open("media/images/exampleimage.jpg"));
            return StampPickerItem.fromBitmap(bitmap)
                    .withSize(StampPickerItem.DEFAULT_STAMP_ANNOTATION_PDF_WIDTH)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This example shows how to create stamp picker item from PDF file containing transparent
     * vector image.
     */
    @NonNull
    private StampPickerItem createCustomApStreamStampItem() {
        // We supply our logo image from assets.
        AssetAppearanceStreamGenerator appearanceStreamGenerator =
                new AssetAppearanceStreamGenerator("images/PSPDFKit_Logo.pdf");

        // Create picker item from custom subject with appearance stream generator set.
        StampPickerItem stampPickerItem = StampPickerItem.fromTitle(this, CUSTOM_AP_STREAM_SUBJECT)
                .withSize(StampPickerItem.DEFAULT_STAMP_ANNOTATION_PDF_WIDTH)
                .withAppearanceStreamGenerator(appearanceStreamGenerator)
                .build();

        // We want to generate the same AP stream for all stamps with CUSTOM_AP_STREAM_SUBJECT even
        // after the document is reloaded.
        // customStampAppearanceStreamGenerator will be registered as appearance
        // stream generator for document annotations once the document is loaded.
        // see onDocumentLoaded below.
        customStampAppearanceStreamGenerator.addAppearanceStreamGenerator(
                CUSTOM_AP_STREAM_SUBJECT, appearanceStreamGenerator);

        return stampPickerItem;
    }
}
