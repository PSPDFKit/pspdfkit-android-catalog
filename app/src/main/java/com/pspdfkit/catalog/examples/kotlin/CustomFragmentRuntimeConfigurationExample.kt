/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.ui.PdfFragment
import com.pspdfkit.utils.getSupportParcelableExtra
import java.io.File

/**
 * Shows how to dynamically change [PdfFragment] configuration at runtime when used with custom activity.
 */
class CustomFragmentRuntimeConfigurationExample(context: Context) : SdkExample(context, R.string.runtimeConfigurationFragmentExampleTitle, R.string.runtimeConfigurationFragmentExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile: File? ->
            val intent = Intent(context, CustomFragmentRuntimeConfigurationActivity::class.java)
            intent.putExtra(CustomFragmentRuntimeConfigurationActivity.EXTRA_URI, Uri.fromFile(documentFile))
            context.startActivity(intent)
        }
    }
}

/**
 * This activity shows how to change [PdfFragment] configuration at runtime when used with custom activity.
 */
class CustomFragmentRuntimeConfigurationActivity : AppCompatActivity() {

    private lateinit var configuration: PdfConfiguration
    private lateinit var fragment: PdfFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the Uri provided when launching the activity.
        val documentUri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)
            ?: throw IllegalStateException("Extras bundle was missing document URI")

        // Extract the existing fragment. The fragment only exist if it has been created previously (like if the activity is recreated).
        // If no fragment was found, create a new one providing it with the configuration and document Uri.
        var fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as PdfFragment?
        if (fragment == null) {
            // Create a new configuration and fragment.
            configuration = PdfConfiguration.Builder().build()
            fragment = PdfFragment.newInstance(documentUri, configuration)
        } else {
            // Use existing configuration.
            configuration = fragment.configuration
        }

        // Set theme according to invert colors property in the configuration.
        setTheme(getNightModeTheme(configuration.isInvertColors))

        // Set activity layout. Must be called after setTheme for configured theme to work properly.
        setContentView(R.layout.activity_custom_fragment_runtime_configuration)

        // Bind example configuration change actions to buttons in layout.
        findViewById<View>(R.id.toggle_scroll_direction).setOnClickListener { toggleScrollDirection() }
        findViewById<View>(R.id.toggle_night_mode_button).setOnClickListener { toggleNightMode() }

        // Add the fragment to the activity and register all needed listeners.
        setFragment(fragment)
    }

    private fun setFragment(fragment: PdfFragment) {
        // Replace previous fragment.
        this.fragment = fragment

        // Replace old fragment with a new one in Activity's fragment manager.
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * When changing fragment configuration we need to replace fragment with a new instance with updated configuration.
     */
    private fun toggleScrollDirection() {
        val scrollDirection = fragment.configuration.scrollDirection

        // Copy existing configuration to a new configuration builder.
        val newConfig = fragment.configuration.copy(
            scrollDirection = if (scrollDirection == PageScrollDirection.HORIZONTAL) PageScrollDirection.VERTICAL else PageScrollDirection.HORIZONTAL
        )

        // Create a new fragment based on the current fragment and the new configuration.
        // This copies the state (loaded document, scrolled page, selected annotations etc.) of the
        // current fragment while applying the new configuration.
        val newFragment = PdfFragment.newInstance(fragment, newConfig)

        // Replace the current fragment with it.
        setFragment(newFragment)
    }

    /**
     * Here we show how to implement the night mode. We replace existing fragment with
     * configuration that inverts rendering colors and set dark theme on the activity.
     */
    private fun toggleNightMode() {
        // In this example, we will use invert colors property to control night mode to make things simple.
        val isNightModeActive = fragment.configuration.isInvertColors

        // Copy existing configuration to a new configuration builder.
        val newConfig = fragment.configuration.copy(
            // Toggle invert colors property.
            isInvertColors = !isNightModeActive
        )

        // Create a new fragment based on the current fragment and the new configuration.
        // This copies the state (loaded document, scrolled page, selected annotations etc.) of the
        // current fragment while applying the new configuration.
        val newFragment = PdfFragment.newInstance(fragment, newConfig)
        setFragment(newFragment)

        // Activity theme must be applied before setContentView. Thus we need to restart the activity.
        // When activity is restarted, we set the theme according to invertColors configuration property (see onCreate above).
        recreate()
    }

    companion object {
        const val EXTRA_URI = "CustomFragmentRuntimeConfigurationActivity.EXTRA_URI"

        private fun getNightModeTheme(isNightMode: Boolean): Int {
            return if (isNightMode) R.style.PSPDFCatalog_Theme_Dark else R.style.PSPDFCatalog_Theme
        }
    }
}
