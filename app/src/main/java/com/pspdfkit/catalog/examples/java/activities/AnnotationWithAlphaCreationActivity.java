/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import com.pspdfkit.annotations.Annotation;
import com.pspdfkit.annotations.FreeTextAnnotation;
import com.pspdfkit.annotations.HighlightAnnotation;
import com.pspdfkit.annotations.InkAnnotation;
import com.pspdfkit.annotations.LineAnnotation;
import com.pspdfkit.annotations.NoteAnnotation;
import com.pspdfkit.annotations.PolygonAnnotation;
import com.pspdfkit.annotations.PolylineAnnotation;
import com.pspdfkit.annotations.SquareAnnotation;
import com.pspdfkit.annotations.StampAnnotation;
import com.pspdfkit.catalog.examples.java.AnnotationWithAlphaCreationExample;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.utils.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This activity shows how to create various semi-transparent annotations programmatically. Also
 * have a look at the {@link AnnotationWithAlphaCreationExample} class.
 */
public class AnnotationWithAlphaCreationActivity extends PdfActivity {

    private static final int ADD_STAMP_ITEM_ID = 1234;
    private final float ALPHA = 0.4f;

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_STAMP_ITEM_ID, 0, "Add Stamp");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == ADD_STAMP_ITEM_ID) {
            createStamp();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @UiThread
    @Override
    public void onDocumentLoaded(@NonNull final PdfDocument document) {
        final int pageIndex = 0;
        createNoteAnnotation(pageIndex);
        createHighlightAnnotation(pageIndex, "PSPDFKit", Color.YELLOW);
        createHighlightAnnotation(pageIndex, "QuickStart", Color.GREEN);
        createFreeTextAnnotation(pageIndex);
        createInkAnnotation(pageIndex);
        createLineAnnotation(pageIndex);
        createPolyline(pageIndex);
        createPolygon(pageIndex);
        createSquareAnnotation(pageIndex);
    }

    private void createStamp() {
        final PdfDocument document = getDocument();
        if (document == null) return;
        final int pageIndex = getPageIndex();
        if (pageIndex < 0) return;

        final Size pageSize = document.getPageSize(pageIndex);
        float halfWidth = pageSize.width / 2;
        float halfHeight = pageSize.height / 2;
        final RectF rect = new RectF(halfWidth - 100, halfHeight + 100, halfWidth + 100, halfHeight - 100);

        final StampAnnotation stamp = new StampAnnotation(pageIndex, rect, "STAMP_SUBJECT");
        final int color = Color.rgb(255, 0, 0);
        stamp.setColor(color);
        stamp.setFillColor(Color.rgb(255, 255, 255));
        stamp.setAlpha(ALPHA);

        addAnnotationToPage(stamp);
    }

    private void createNoteAnnotation(@IntRange(from = 0) final int pageIndex) {
        final RectF pageRect = new RectF(180, 692, 212, 660);
        final String contents = "This is note annotation was created from code.";
        final String icon = NoteAnnotation.CROSS;
        final int color = Color.GREEN;

        // Create the annotation, and set its properties.
        final NoteAnnotation noteAnnotation = new NoteAnnotation(pageIndex, pageRect, contents, icon);
        noteAnnotation.setColor(color);
        noteAnnotation.setAlpha(ALPHA);

        addAnnotationToPage(noteAnnotation);
    }

    private void createHighlightAnnotation(
            @IntRange(from = 0) final int pageIndex, @NonNull final String highlightedText, @ColorInt final int color) {
        PdfDocument document = getDocument();
        // Find the provided text on the current page.
        final int textPosition = document.getPageText(pageIndex).indexOf(highlightedText);

        if (textPosition >= 0) {
            // To create a text highlight, extract the rects of the text to highlight and pass them
            // to the annotation constructor.
            final List<RectF> textRects =
                    document.getPageTextRects(pageIndex, textPosition, highlightedText.length(), true);
            final HighlightAnnotation highlightAnnotation = new HighlightAnnotation(pageIndex, textRects);

            highlightAnnotation.setColor(color);
            highlightAnnotation.setAlpha(ALPHA);

            addAnnotationToPage(highlightAnnotation);
        } else {
            Toast.makeText(this, "Can't find the text to highlight.", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void createInkAnnotation(@IntRange(from = 0) final int pageIndex) {
        final InkAnnotation inkAnnotation = new InkAnnotation(pageIndex);
        inkAnnotation.setColor(Color.rgb(255, 165, 0)); // Orange
        inkAnnotation.setAlpha(ALPHA);
        inkAnnotation.setLineWidth(10);

        // Create a line from a list of points.
        final List<PointF> line = new ArrayList<>();
        for (int x = 120; x < 720; x += 60) {
            final int y = ((x % 120) == 0) ? 400 : 350;
            line.add(new PointF(x, y));
        }

        // Ink annotations can hold multiple lines. This example only uses a single line.
        inkAnnotation.setLines(Collections.singletonList(line));

        addAnnotationToPage(inkAnnotation);
    }

    private void createLineAnnotation(@IntRange(from = 0) final int pageIndex) {
        final LineAnnotation lineAnnotation = new LineAnnotation(pageIndex, new PointF(400, 100), new PointF(500, 100));

        lineAnnotation.setColor(Color.CYAN);
        lineAnnotation.setAlpha(ALPHA);
        lineAnnotation.setLineWidth(20);

        addAnnotationToPage(lineAnnotation);
    }

    private void createPolyline(@IntRange(from = 0) final int pageIndex) {

        final PolylineAnnotation polylineAnnotation = new PolylineAnnotation(
                pageIndex, Arrays.asList(new PointF(400, 200), new PointF(500, 200), new PointF(500, 150)));

        polylineAnnotation.setAlpha(ALPHA);
        polylineAnnotation.setColor(Color.RED);
        polylineAnnotation.setLineWidth(20);

        addAnnotationToPage(polylineAnnotation);
    }

    private void createPolygon(@IntRange(from = 0) final int pageIndex) {

        final PolygonAnnotation polygonAnnotation = new PolygonAnnotation(
                pageIndex, Arrays.asList(new PointF(600, 600), new PointF(600, 500), new PointF(500, 550)));

        polygonAnnotation.setAlpha(ALPHA);
        polygonAnnotation.setColor(Color.RED);
        polygonAnnotation.setFillColor(Color.BLUE);
        polygonAnnotation.setLineWidth(20);

        addAnnotationToPage(polygonAnnotation);
    }

    private void createSquareAnnotation(@IntRange(from = 0) final int pageIndex) {
        final SquareAnnotation squareAnnotation = new SquareAnnotation(pageIndex, new RectF(0, 100, 100, 0));

        squareAnnotation.setColor(Color.RED);
        squareAnnotation.setFillColor(Color.RED);
        squareAnnotation.setAlpha(ALPHA);

        addAnnotationToPage(squareAnnotation);
    }

    private void createFreeTextAnnotation(@IntRange(from = 0) final int pageIndex) {
        final String contents = "Add text to pages using FreeTextAnnotations";
        final RectF pageRect = new RectF(100f, 980f, 320f, 930f);

        final FreeTextAnnotation freeTextAnnotation = new FreeTextAnnotation(pageIndex, pageRect, contents);
        freeTextAnnotation.setColor(Color.BLUE);
        freeTextAnnotation.setAlpha(ALPHA);
        freeTextAnnotation.setTextSize(20f);

        addAnnotationToPage(freeTextAnnotation);
    }

    /** Adds annotation to page asynchronously, refreshing rendering in all UI components. */
    private void addAnnotationToPage(@NonNull Annotation annotation) {
        getPdfFragment().addAnnotationToPage(annotation, false);
    }
}
