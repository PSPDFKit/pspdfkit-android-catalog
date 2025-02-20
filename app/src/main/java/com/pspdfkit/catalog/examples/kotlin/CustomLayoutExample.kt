/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.UiThread
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.catalog.utils.Utils
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.TabBarHidingMode
import com.pspdfkit.configuration.activity.ThumbnailBarMode
import com.pspdfkit.configuration.search.SearchType
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.listeners.OnVisibilityChangedListener
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.PdfThumbnailGrid

/**
 * This example shows how to use a custom [PdfActivity] with a custom layout. In detail:
 *
 * - It subclasses the [PdfActivity] and uses a custom layout resource.
 * - It removes the thumbnail bar and adds two navigation buttons to the layouts ("Next" and "Previous").
 * - It puts the thumbnail grid into the right navigation drawer.
 */
class CustomLayoutExample(context: Context) : SdkExample(context, R.string.customLayoutExampleTitle, R.string.customLayoutExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Define the custom layout of our activity inside the configuration.
        configuration.layout(R.layout.custom_pdf_activity)

        // The custom layout is missing some UI elements, in order to prevent the activity from
        // accessing them we have to deactivate them in the configuration.
        configuration.apply {
            // The custom layout has no thumbnail bar.
            setThumbnailBarMode(ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE)
            // The custom layout has no document editor.
            documentEditorEnabled(false)
            // The custom layout has no document title overlay.
            documentTitleOverlayEnabled(false)
            // The custom layout has no navigation buttons.
            navigationButtonsEnabled(false)
            // This example shows the thumbnail grid in a custom drawer layout.
            thumbnailGridEnabled(true)
            // Disable forms editing.
            formEditingEnabled(false)
            // The custom layout has no content editor.
            contentEditingEnabled(false)
            // Disable measurements
            setMeasurementToolsEnabled(false)
        }

        // Hide tab bar as it's not used by the custom layout.
        configuration.setTabBarHidingMode(TabBarHidingMode.HIDE)

        // We keep things simple, and use inline search and deactivate immersive mode for this example.
        configuration.apply {
            setSearchType(SearchType.INLINE)
            useImmersiveMode(false)
        }

        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // To start the `CustomLayoutActivity` create a launch intent using the builder.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(CustomLayoutActivity::class)
                .build()
            context.startActivity(intent)
        }
    }
}

class CustomLayoutActivity : PdfActivity() {

    /**
     * Total number of pages in the current document.
     */
    private var documentPageCount = 0

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var thumbnailGridView: PdfThumbnailGrid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get all required views for customization.
        drawerLayout = findViewById(R.id.drawerLayout)

        // Dynamically set the correct width of the thumbnail grid drawer.
        val thumbnailGridDrawer = findViewById<View>(R.id.thumbnailGridDrawer)
        Utils.setProperNavigationDrawerWidth(thumbnailGridDrawer)

        thumbnailGridView = findViewById(R.id.pspdf__activity_thumbnail_grid)

        // Register the thumbnail grid with the fragment, so it is notified of page changes.
        requirePdfFragment().addDocumentListener(thumbnailGridView)

        // Toggle drawer when thumbnail grid visibility changes.
        thumbnailGridView.addOnVisibilityChangedListener(object : OnVisibilityChangedListener {
            override fun onShow(view: View) {
                drawerLayout.openDrawer(DRAWER_GRAVITY)
            }

            override fun onHide(view: View) {
                drawerLayout.closeDrawer(DRAWER_GRAVITY)
            }
        })

        // Ensure action bar and grid are visible when drawer is opened.
        drawerLayout.addDrawerListener(object : SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerClosed(drawerView: View) {
                thumbnailGridView.hide()
            }

            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    thumbnailGridView.show()
                }
            }
        })

        // Go to the tapped page, and close the thumbnail drawer after selecting a page.
        thumbnailGridView.setOnPageClickListener { _, pageIndex ->
            setPageIndex(pageIndex)
            toggleThumbnailGrid()
        }

        // Flip to the next page when clicking on the next page button.
        findViewById<View>(R.id.nextPageButton).setOnClickListener {
            val currentPage = pageIndex
            if (currentPage < documentPageCount - 1) pageIndex = currentPage + 1
        }
        // Flip to the previous page when clicking on the previous page button.
        findViewById<View>(R.id.previousPageButton).setOnClickListener {
            val currentPage = pageIndex
            if (currentPage > 0) pageIndex = currentPage - 1
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_OPTION_THUMBNAIL_GRID -> {
                toggleThumbnailGrid()
                hidePSPDFViews()

                // Consume the event, preventing the default behavior.
                return true
            }
            MENU_OPTION_OUTLINE, MENU_OPTION_SEARCH -> {
                hideThumbnailGrid()
                // Don't consume the event here since we want to fallback to default action handling.
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        hideThumbnailGrid()
    }

    /**
     * Called as soon as the PDF document has been loaded.
     */
    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        // Retrieve the total number of pages in the document.
        documentPageCount = document.pageCount
    }

    private fun hidePSPDFViews() {
        pspdfKitViews.outlineView?.hide()
        pspdfKitViews.searchView?.hide()
    }

    private fun hideThumbnailGrid() {
        if (drawerLayout.isDrawerVisible(DRAWER_GRAVITY)) {
            drawerLayout.closeDrawer(DRAWER_GRAVITY)
        }
    }

    private fun toggleThumbnailGrid() {
        if (drawerLayout.isDrawerOpen(DRAWER_GRAVITY)) {
            thumbnailGridView.hide()
            drawerLayout.closeDrawer(DRAWER_GRAVITY)
        } else {
            thumbnailGridView.show()
            drawerLayout.openDrawer(DRAWER_GRAVITY)
        }
    }

    companion object {
        /** Gravity of the thumbnail grid drawer. */
        private const val DRAWER_GRAVITY = GravityCompat.END
    }
}
