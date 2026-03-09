/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.listeners.OnVisibilityChangedListener
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import com.pspdfkit.ui.search.PdfSearchViewInline

/**
 * Simple example that demonstrates how to add a navigation icon (back button) to the toolbar.
 * The back button appears in the top-left of the toolbar and closes the activity when tapped.
 * The back button is hidden when the inline search view is visible to avoid duplicate back buttons.
 */
class BackButtonExample(context: Context) :
    SdkExample(
        context,
        R.string.backButtonExampleTitle,
        R.string.backButtonExampleDescription,
    ) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(BackButtonExampleActivity::class.java)
                .build()
            context.startActivity(intent)
        }
    }
}

/**
 * Activity displaying the PDF with a navigation icon (back button) in the toolbar.
 * Tapping the back button closes the activity.
 */
class BackButtonExampleActivity :
    PdfActivity(),
    OnVisibilityChangedListener {

    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the main toolbar and add a navigation icon (back arrow).
        toolbar = findViewById(com.pspdfkit.R.id.pspdf__toolbar_main)
        setupNavigationButton()

        // Listen for search view visibility changes to hide/show the navigation button.
        pspdfKitViews.addOnVisibilityChangedListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        pspdfKitViews.removeOnVisibilityChangedListener(this)
    }

    /**
     * Sets up the navigation button (back arrow) on the toolbar.
     */
    private fun setupNavigationButton() {
        toolbar?.apply {
            setNavigationIcon(com.pspdfkit.R.drawable.pspdf__ic_arrow_back)
            setNavigationOnClickListener { finish() }
        }
    }

    /**
     * Hides the navigation button when the inline search view is shown.
     */
    private fun handleInlineSearch(view: View, isVisible: Boolean) {
        if (view is PdfSearchViewInline) {
            toolbar?.navigationIcon =
                if (isVisible) null else AppCompatResources.getDrawable(this, com.pspdfkit.R.drawable.pspdf__ic_arrow_back)
        }
    }

    override fun onShow(view: View) {
        handleInlineSearch(view, isVisible = true)
    }

    override fun onHide(view: View) {
        handleInlineSearch(view, isVisible = false)
    }
}
