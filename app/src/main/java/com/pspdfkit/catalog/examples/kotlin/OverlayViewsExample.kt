/*
 *   Copyright © 2019-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.viewModels
import com.pspdfkit.annotations.Annotation
import com.pspdfkit.annotations.AnnotationType
import com.pspdfkit.annotations.FreeTextAnnotation
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.ui.overlay.OverlayLayoutParams
import com.pspdfkit.ui.overlay.OverlayViewProvider
import kotlin.getValue

class OverlayViewsExample(context: Context) :
    SdkExample(context.getString(R.string.overlayViewsExample), context.getString(R.string.overlayViewsExampleDescription)) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(WELCOME_DOC, title, context, true) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(
                    configuration
                        // We disable annotation editing to keep the example focused.
                        .editableAnnotationTypes(listOf(AnnotationType.NONE))
                        .build()
                )
                .activityClass(OverlayViewsActivity::class)
                .build()

            // Start the OverlayViewsActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}

// Suppress warnings about experimental classes.
class OverlayViewsActivity : PdfActivity() {

    private val viewModel: AnnotationCreationViewModel by viewModels()

    private lateinit var viewProvider: MyViewProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewProvider = MyViewProvider(this, requirePdfFragment(), savedInstanceState)
        // Add our view provider to the fragment.
        requirePdfFragment().addOverlayViewProvider(viewProvider)
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        viewModel.createObjects {
            // We add a simple annotation to explain to users what to do.
            val clickHereAnnotation = FreeTextAnnotation(
                0,
                RectF(50f, 900f, 350f, 700f),
                "Tap Anywhere on The Page"
            )
            clickHereAnnotation.textSize = 48f

            document.annotationProvider.addAnnotationToPage(clickHereAnnotation)
        }
    }

    override fun onPageClick(document: PdfDocument, pageIndex: Int, event: MotionEvent?, pagePosition: PointF?, clickedAnnotation: Annotation?): Boolean {
        if (pageIndex == 0) {
            // User tapped on first page, add our overlay views.
            viewProvider.areFirstPageViewsVisible = !viewProvider.areFirstPageViewsVisible
            return true
        }
        if (pageIndex == 1) {
            // User tapped on second page, add our overlay views.
            viewProvider.areSecondPageViewsVisible = !viewProvider.areSecondPageViewsVisible
            return true
        }

        return super.onPageClick(document, pageIndex, event, pagePosition, clickedAnnotation)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // In order to remember what the user entered we have to save it here.
        viewProvider.saveState(outState)
    }
}

/** A custom OverlayViewProvider that will display views on the first and second. */
@SuppressLint("SetTextI18n", "SetJavaScriptEnabled")
class MyViewProvider(
    context: Context,
    private val pdfFragment: PdfFragment,
    savedInstanceState: Bundle?
) : OverlayViewProvider() {

    companion object {
        private const val STATE_FIRST_PAGE_CLICKED = "MyOverlayProvider.FirstPageClicked"
        private const val STATE_SECOND_PAGE_CLICKED = "MyOverlayProvider.SecondPageClicked"

        private const val TEXT_SIZE = 24f
    }

    private val firstPageView: TextView
    private val secondPageView: WebView

    private val pageToViewTransformation = Matrix()

    /** This indicates whether our other form views are visible or not. */
    var areFirstPageViewsVisible = false
        set(value) {
            field = value
            notifyOverlayViewsChanged()
        }

    /** This indicates whether our other form views are visible or not. */
    var areSecondPageViewsVisible = false
        set(value) {
            field = value
            notifyOverlayViewsChanged()
        }

    init {
        // If you need a static set of views pre creating them is the most efficient way to go.
        firstPageView = TextView(context)
        firstPageView.text = "\uD83D\uDC4B I'm a custom overlay view!\nCheckout what happens when clicking on the second page."
        firstPageView.setBackgroundColor(Color.WHITE)
        // We put this view next to our freetext annotation.

        // The rect are the PDF coordinates on the page where our TextView should be.
        firstPageView.layoutParams = OverlayLayoutParams(
            RectF(350f, 900f, 768f, 700f),
            // We use SizingMode.SCALING here, this has the effect that the view will only be measured once and then a scale will be applied to it.
            // This means that the text size will automatically scale up as the page is zoomed.
            OverlayLayoutParams.SizingMode.SCALING
        )

        // You can embed any kind of view, even a WebView.
        secondPageView = WebView(context)
        secondPageView.settings.javaScriptEnabled = true
        // Load the Nutrient homepage.
        secondPageView.loadUrl("https://nutrient.io/")
        // We fill the entire page, the Webview will consume all scroll events so scrolling the page will only work in the margins.
        secondPageView.layoutParams = OverlayLayoutParams(
            RectF(0f, 1024f, 768f, 0f),
            // We use the SizingMode.LAYOUT here since we want the WebView to actually increase the available size if the page is zoomed in.
            OverlayLayoutParams.SizingMode.LAYOUT
        )

        if (savedInstanceState != null) {
            // If we have a saved state we restore it here.
            areFirstPageViewsVisible = savedInstanceState.getBoolean(STATE_FIRST_PAGE_CLICKED)
            areSecondPageViewsVisible = savedInstanceState.getBoolean(STATE_SECOND_PAGE_CLICKED)
        }
    }

    // We're only putting views on Page 13 containing the form elements.
    override fun getPagesWithViews(): Set<Int> = setOf(0, 1)

    // This method is called for all pages returned by getFilteredPages().
    // You simply return a list of all views for the page and they will be displayed.
    override fun getViewsForPage(context: Context, document: PdfDocument, pageIndex: Int): List<View> {
        val views = mutableListOf<View>()

        if (areFirstPageViewsVisible && pageIndex == 0) {
            // If the user clicked on the first page we add our text view to the list of views.
            // This way it will be overlaid on the document.

            // We update the text size so it stays consistent no matter the device size.
            val matrix = pdfFragment.viewProjection.getPageToViewTransformation(pageIndex, pageToViewTransformation)
            firstPageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, convertPdfPointsToPixel(TEXT_SIZE, pageIndex, matrix))
            views.add(firstPageView)
        }

        if (areSecondPageViewsVisible && pageIndex == 1) {
            views.add(secondPageView)
        }

        return views
    }

    override fun onViewsRecycled(pageIndex: Int, views: List<View>) {
        // This is called for every page with the views for the page that were recycled.
        // If you are pooling your views for efficient reuse this is good place to return them to the pool.
    }

    override fun onViewsShown(pageIndex: Int, views: List<View>) {
        // This is called for every page with the views for the page that are now visible because the hosting page is now visible.
    }

    override fun onViewsHidden(pageIndex: Int, views: List<View>) {
        // This is called for every page with the views for the page that are no longer visible because the hosting page is no longer visible.
    }

    fun saveState(outState: Bundle) {
        // In our example we simply store whatever the user entered.
        outState.putBoolean(STATE_FIRST_PAGE_CLICKED, areFirstPageViewsVisible)
        outState.putBoolean(STATE_SECOND_PAGE_CLICKED, areSecondPageViewsVisible)
    }

    /** This method converts the given pdf points to pixels for use in our text size. */
    private fun convertPdfPointsToPixel(pdfPoints: Float, pageIndex: Int, pageToViewTransformation: Matrix): Float {
        val f = FloatArray(9)
        pageToViewTransformation.getValues(f)

        // We multiply by the scale value stored in the matrix, but divide by the zoomscale so the result doesn't change as we zoom in.
        return (pdfPoints * f[Matrix.MSCALE_X]) / pdfFragment.getZoomScale(pageIndex)
    }
}
