/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.graphics.PointF;
import android.os.Bundle;
import android.view.MotionEvent;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.catalog.R;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.scrollbar.VerticalScrollBar;

/** This activity sets up the {@link VerticalScrollBar} for using it with the loaded document. */
public class VerticalScrollbarExampleActivity extends PdfActivity {

    private VerticalScrollBar verticalScrollBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        verticalScrollBar.setOnPageChangeListener((verticalScrollBar, pageIndex) -> fragment.setPageIndex(pageIndex));
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
