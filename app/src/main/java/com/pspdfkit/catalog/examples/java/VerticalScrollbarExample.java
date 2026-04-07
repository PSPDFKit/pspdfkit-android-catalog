/*
 *   Copyright © 2017-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.activity.TabBarHidingMode;
import com.pspdfkit.configuration.activity.ThumbnailBarMode;
import com.pspdfkit.configuration.page.PageScrollDirection;
import com.pspdfkit.configuration.page.PageScrollMode;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.scrollbar.VerticalScrollBar;

/**
 * This example shows how to use the {@link VerticalScrollBar} widget inside a {@link PdfActivity}.
 */
public class VerticalScrollbarExample extends SdkExample {

    public VerticalScrollbarExample(@NonNull final Context context) {
        super(context, R.string.verticalScrollbarExampleTitle, R.string.verticalScrollbarExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        configuration
                // The example uses its own scrollbar implementation.
                .scrollbarsEnabled(false)
                // We use a custom layout that removes the thumbnail bar, and instead places the
                // vertical scrollbar widget.
                .layout(R.layout.custom_scrollbar_activity)
                .setThumbnailBarMode(ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE)
                .setTabBarHidingMode(TabBarHidingMode.HIDE)
                .documentTitleOverlayEnabled(false)
                .navigationButtonsEnabled(false)
                // Although the widget also works for horizontal scrolling, we switch to vertical
                // continuous scroll mode in this example.
                .scrollDirection(PageScrollDirection.VERTICAL)
                .scrollMode(PageScrollMode.CONTINUOUS)
                // The custom layout has no content editor. In order to prevent the activity from accessing
                // it we have to deactivate it in the configuration.
                .contentEditingEnabled(false)
                .setMeasurementToolsEnabled(false);

        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(WELCOME_DOC, getTitle(), context, documentFile -> {
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(VerticalScrollbarExampleActivity.class)
                    .build();

            // Start the VerticalScrollbarExampleActivity for the extracted document.
            context.startActivity(intent);
        });
    }

    /** This activity sets up the {@link VerticalScrollBar} for using it with the loaded document. */
    public static class VerticalScrollbarExampleActivity extends PdfActivity {

        private VerticalScrollBar verticalScrollBar;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // The scrollbar widget has already been inflated from the layout file defined via the
            // configuration.
            verticalScrollBar = findViewById(R.id.customScrollbar);

            // The scrollbar also works for horizontally scrolled documents. We need to manually tell it
            // the used scroll direction.
            verticalScrollBar.setScrollDirection(
                    getConfiguration().getConfiguration().getScrollDirection());

            // Register the scrollbar widget to receive all scroll events of the PdfFragment.
            final PdfFragment fragment = getPdfFragment();
            fragment.addDocumentScrollListener(verticalScrollBar);

            // The scrollbar widget emits page change events (whenever the user drags the scroll
            // indicator).
            // Use those events to scroll the document inside the fragment.
            verticalScrollBar.setOnPageChangeListener(
                    (verticalScrollBar, pageIndex) -> fragment.setPageIndex(pageIndex));
        }

        @UiThread
        @Override
        public void onDocumentLoaded(@NonNull PdfDocument document) {
            // Prepare the scrollbar widget: You need to set the controlled document as well as the
            // configured scroll direction.
            verticalScrollBar.setDocument(document);
        }

        @Override
        public boolean onPageClick(
                @NonNull PdfDocument document,
                @IntRange(from = 0) int pageIndex,
                @Nullable MotionEvent event,
                @Nullable PointF pagePosition,
                @Nullable Annotation clickedAnnotation) {
            // If the page was tapped in a blank area, reveal the indicator.
            if (clickedAnnotation == null) {
                verticalScrollBar.awakenScrollBar();
            }

            // Do not consume the event.
            return false;
        }

        @Override
        public boolean onDocumentClick() {
            // If the document background (where no page is) is tapped, reveal the indicator.
            verticalScrollBar.awakenScrollBar();

            // Do not consume the event.
            return false;
        }
    }
}
