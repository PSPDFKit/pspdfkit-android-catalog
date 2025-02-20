/*
 *   Copyright © 2016-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.screenreader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import com.pspdfkit.catalog.R;
import com.pspdfkit.datastructures.Range;
import com.pspdfkit.datastructures.TextBlock;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.drawable.PdfDrawable;
import com.pspdfkit.ui.drawable.PdfDrawableProvider;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The {@link ScreenReader} reads sentences on a document, and provides a {@link
 * PdfDrawableProvider} for synchronously highlighting on the fragment. It parses the document and
 * creates {@link Unit} instances, that can be read and highlighted synchronously.
 *
 * <p>Use {@link #readSentencesOnPage(PdfDocument, int)} to start reading the sentences of a single
 * page.
 */
public class ScreenReader {

    /**
     * The Android TTS module used for text synthesis. The class will try to initialize this in the
     * constructor, creating an error if TTS initialization failed.
     */
    @NonNull
    private final TextToSpeech textToSpeech;
    /**
     * Visual padding of highlighted units. This contains a pixel value, but is initialized using
     * DP.
     */
    private final int highlightPadding;
    /**
     * A list of units that are currently spoken. The screen reader will subsequently speak out
     * these units, and highlight them synchronously. Theoretically units can be words, sentences,
     * or even letters - for simplicity this example only uses sentences.
     */
    @Nullable
    private List<Unit> spokenUnits;
    /**
     * The drawable provider serving drawables of currently spoken units. This is a very basic
     * example of a drawable provider, that is backed by a list of ready-to-serve drawables (the
     * {@link #spokenUnits}). If a document provider needs to serve drawables for many hundred
     * pages, it may create drawables on demand rather than eagerly.
     */
    @NonNull
    private final PdfDrawableProvider drawableProvider = new PdfDrawableProvider() {
        @Nullable
        @Override
        public List<? extends PdfDrawable> getDrawablesForPage(
                @NonNull Context context, @NonNull PdfDocument document, @IntRange(from = 0) int pageIndex) {
            final List<Unit> availableDrawables = spokenUnits;
            if (availableDrawables == null) return null;
            final List<PdfDrawable> drawablesForPage = new ArrayList<>();
            for (Unit unit : availableDrawables) {
                if (unit.textBlock.pageIndex == pageIndex) {
                    drawablesForPage.add(unit);
                }
            }
            return drawablesForPage;
        }
    };
    /**
     * Since we're parsing text on a background thread using RxJava, this will keep track of such
     * operations. If {@link #stopReading()} is called before parsing has been finished, we can
     * cancel parsing operations using this.
     */
    @Nullable
    private Disposable parsingDisposable;
    /** Set to true as soon as TTS has been successfully initialized. */
    private boolean initialized = false;
    /**
     * This listener handles highlighting while a TTS is progressing. It is registered on the used
     * {@link TextToSpeech} object when starting to read, and unregistered when stopping (to prevent
     * leaks).
     */
    private UtteranceProgressListener textToSpeechProgressListener = new UtteranceProgressListener() {

        @Override
        public void onStart(String utteranceId) {
            setSpokenUnitHighlighted(utteranceId, true);
        }

        @Override
        public void onDone(String utteranceId) {
            setSpokenUnitHighlighted(utteranceId, false);
        }

        @Override
        public void onError(String utteranceId) {
            setSpokenUnitHighlighted(utteranceId, false);
        }

        private void setSpokenUnitHighlighted(@NonNull final String utteranceId, final boolean highlighted) {
            findUnitByUid(spokenUnits, utteranceId)
                    // Changing the drawable (and thus invalidating it) is only allowed from
                    // the main thread.
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(unit -> unit.setHighlighted(highlighted));
        }
    };

    /**
     * Creates the {@link ScreenReader} initializing the text-to-speech engine. Once initialization
     * is done the {@link OnInitListener#onInitializationSucceeded()} method of the provided
     * listener is called. If TTS could not be initialized (e.g. because of a missing TTS engine)
     * the {@link OnInitListener#onInitializationFailed()} is called instead.
     *
     * @param context The context is required for TTS initialization.
     * @param onInitListener {@link OnInitListener} that is called on a successful initialization or
     *     in case of a failure.
     */
    public ScreenReader(@NonNull final Context context, @NonNull final OnInitListener onInitListener) {
        this.highlightPadding =
                context.getResources().getDimensionPixelOffset(R.dimen.screenreaderexample_drawable_padding);
        this.textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.ERROR) {
                initialized = false;
                onInitListener.onInitializationFailed();
            } else {
                initialized = true;
                onInitListener.onInitializationSucceeded();
            }
        });
    }

    /**
     * Starts reading and highlighting sentences on the requested document page.
     *
     * @param document {@link PdfDocument} that should be read.
     * @param pageIndex Number of the document page that should be read.
     */
    public void readSentencesOnPage(@NonNull final PdfDocument document, @IntRange(from = 0) final int pageIndex) {
        stopReading();

        // Parse the document on a computational thread. Doing this on the main-thread would block
        // the UI.
        parsingDisposable = parseSentences(document, pageIndex)
                .subscribeOn(Schedulers.computation())
                .toList()
                .subscribe(this::readUnits);
    }

    /**
     * This will stop and currently running screen reading process and will remove all highlights.
     */
    public void stopReading() {
        if (!isReading()) return;

        if (parsingDisposable != null) {
            parsingDisposable.dispose();
            parsingDisposable = null;
        }

        textToSpeech.setOnUtteranceProgressListener(null);
        textToSpeech.stop();
        spokenUnits = null;
        drawableProvider.notifyDrawablesChanged();
    }

    /**
     * Returns the drawableProvider that highlights text while it is read. Register it on the
     * fragment using {@link PdfFragment#addDrawableProvider(PdfDrawableProvider)}
     *
     * @return A {@link PdfDrawableProvider} that highlights the text, while it is read.
     */
    @NonNull
    public PdfDrawableProvider getDrawableProvider() {
        return drawableProvider;
    }

    /**
     * Returns whether this screen reader has been successfully initialized or not.
     *
     * @return {@code true} if initialized and ready to use, otherwise {@code false}.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Releases all resources. After calling this, the screen reader is no longer usable and should
     * be disposed.
     */
    public void shutdown() {
        initialized = false;
        textToSpeech.shutdown();
        spokenUnits = null;
    }

    /**
     * Returns whether a something is currently read and highlighted or not.
     *
     * @return {@code true} if something is currently read, otherwise {@code false}.
     */
    private boolean isReading() {
        return spokenUnits != null;
    }

    private void readUnits(@NonNull final List<Unit> units) {
        // The progress of currently spoken units is followed by this listener, which will update
        // highlights accordingly.
        this.textToSpeech.setOnUtteranceProgressListener(textToSpeechProgressListener);

        spokenUnits = units;

        // We notify the drawable provider that its backing data has changed.
        drawableProvider.notifyDrawablesChanged();

        // Enqueue all sentences for TTS synthesis. Once they are read the listener registered above
        // will do the actual screen highlighting.
        for (final Unit sentence : units) {
            textToSpeech.speak(sentence.textBlock.text, TextToSpeech.QUEUE_ADD, null, sentence.uid);
        }
    }

    /**
     * Internal helper for parsing sentences on a document page. Since parsing can take time, this
     * uses an RxJava observable which allows putting the work into a computational thread.
     *
     * @param document The document providing the text to parse.
     * @param pageIndex The page number of the document page to parse.
     * @return A {@link Observable} emitting {@link Unit} objects, each containing a sentence of the
     *     parsed page.
     */
    @NonNull
    private Flowable<Unit> parseSentences(
            @NonNull final PdfDocument document, @IntRange(from = 0) final int pageIndex) {
        return Flowable.create(
                emitter -> {
                    // This example uses a BreakIterator to speak and highlight whole sentences.
                    // It requires the document locale, to correctly find sentences.
                    final BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
                    iterator.setText(document.getPageText(pageIndex));

                    // Split the text into sentences and store each sentence as a readable Unit.
                    int start = iterator.first();
                    for (int end = iterator.next();
                            end != BreakIterator.DONE && !emitter.isCancelled();
                            start = end, end = iterator.next()) {
                        emitter.onNext(new Unit(
                                TextBlock.create(document, pageIndex, new Range(start, end - start)),
                                highlightPadding));
                    }

                    if (!emitter.isCancelled()) {
                        emitter.onComplete();
                    }
                },
                BackpressureStrategy.BUFFER);
    }

    /**
     * Helper for finding a unit in a list. This uses RxJava to allow the search operation and
     * result handling on different threads.
     */
    @NonNull
    private Observable<Unit> findUnitByUid(@Nullable final List<Unit> units, @NonNull final String uid) {
        return Observable.defer(() -> {
            if (units != null) {
                for (Unit unit : units) {
                    if (uid.equals(unit.uid)) return Observable.just(unit);
                }
            }

            return Observable.empty();
        });
    }

    /**
     * Initialization listener, this is used to teardown the example on devices without proper TTS
     * support.
     */
    public interface OnInitListener {
        /** Called by the {@link ScreenReader} as soon as the TTS engine was initialized. */
        void onInitializationSucceeded();

        /**
         * Called by the {@link ScreenReader} if there was an error while initializing the TTS
         * engine.
         */
        void onInitializationFailed();
    }

    /**
     * A screen reader unit is a word or a sentence that can be read via TTS and at the same time
     * get highlighted. Units are created on-demand (e.g. when asking to read a whole page). This
     * class extends the {@link PdfDrawable} which allows drawing content on top of a displayed
     * page.
     */
    private static class Unit extends PdfDrawable {

        @NonNull
        private final Paint paint = new Paint();

        @NonNull
        private final List<RectF> screenRects;

        /**
         * This is the spoken and highlighted text block on the page. It contains the text, as well
         * as the PDF coordinates. Inside {@link #updatePdfToViewTransformation(Matrix)} these
         * coordinates are converted to screen coordinates for drawing.
         */
        @NonNull
        private final TextBlock textBlock;

        /**
         * The uid is used to uniquely identify the spoken unit inside the TTS system. It will be
         * returned by the {@link UtteranceProgressListener} while speaking is performed, and allows
         * to fetch this unit and update its visual representation.
         */
        @NonNull
        private final String uid;

        private final int highlightPadding;
        private int alpha;

        private Unit(@NonNull TextBlock textBlock, final int highlightPadding) {
            this.textBlock = textBlock;
            this.highlightPadding = highlightPadding;
            this.uid = Integer.toHexString(textBlock.hashCode());

            final List<RectF> screenRects = new ArrayList<>(textBlock.pageRects.size());
            for (int i = 0; i < textBlock.pageRects.size(); i++) screenRects.add(new RectF());
            this.screenRects = Collections.unmodifiableList(screenRects);

            // The drawable will paint a light yellow rect via multiplication on top of the
            // highlighted text.
            paint.setColor(Color.parseColor("#FDF4B9"));
            paint.setStyle(Paint.Style.FILL);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

            alpha = 0;
            paint.setAlpha(0);
        }

        /**
         * This method enables or disables visual highlighting of this unit.
         *
         * @param highlighted {@code true} to enable visual highlighting, or {@code false} to
         *     disable it.
         */
        @UiThread
        private void setHighlighted(final boolean highlighted) {
            setAlpha(highlighted ? 255 : 0);
        }

        /**
         * Every time this method is called, PSPDFKit provides a fresh transformation matrix that
         * holds the current PDF-to-view transformation. Using the matrix the drawable can convert
         * PDF coordinates to view/screen coordinates. This example uses the matrix to calculate
         * screen coordinates of {@link TextBlock} instances that should be highlighted.
         */
        @Override
        public void updatePdfToViewTransformation(@NonNull Matrix matrix) {
            super.updatePdfToViewTransformation(matrix);
            for (int i = 0; i < textBlock.pageRects.size(); i++) {
                final RectF rect = screenRects.get(i);

                // This transforms the PDF coordinates of the text block (inside
                // TextBlock#pageRects) to
                // screen coordinates and stores them into another RectF.
                matrix.mapRect(rect, textBlock.pageRects.get(i));

                // We slightly inflate the highlighted rectangle above the text, just because it
                // looks better.
                rect.inset(-highlightPadding, -highlightPadding);

                // The drawable defines its boundaries (the area where it will draw).
                // We're rounding "outside" so that content of the drawable is not accidentally
                // clipped.
                final int l = (int) rect.left;
                final int t = (int) rect.top;
                final int r = (int) Math.ceil(rect.right);
                final int b = (int) Math.ceil(rect.bottom);
                final Rect bounds = getBounds();
                if (i == 0) {
                    bounds.set(l, t, r, b);
                } else {
                    bounds.union(l, t, r, b);
                }
                setBounds(bounds);
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (alpha == 0) return;

            // This method is called on the UI thread, and should not do any long-running
            // calculations.
            // Since the drawable has pre-calculated the screen coordinates, we can simply draw them
            // without additional computation.
            for (RectF rect : screenRects) {
                canvas.drawRect(rect, paint);
            }
        }

        @UiThread
        @Override
        public void setAlpha(int alpha) {
            this.alpha = alpha;
            paint.setAlpha(alpha);

            // If the visual representation of a drawable changed, it can call this method to ensure
            // it's updated on-screen.
            // You can also use this self-invalidation for animating a drawable.
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            // The highlighted text lets the background shine through (it's semi-transparent). If
            // your own drawable is instead opaque,
            // and covers it's complete bounds when drawing, return PixelFormat.OPAQUE instead.
            return PixelFormat.TRANSLUCENT;
        }
    }
}
