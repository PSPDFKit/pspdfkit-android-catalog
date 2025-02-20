/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder

/**
 * This example shows how to change [PdfActivityConfiguration] dynamically when [PdfActivity] is displayed.
 */
class RuntimeConfigurationExample(context: Context) : SdkExample(context, R.string.runtimeConfigurationChangeExampleTitle, R.string.runtimeConfigurationChangeExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, title, context) { documentFile ->
            // Launch the custom example activity using the document and configuration.
            val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                .configuration(configuration.build())
                .activityClass(RuntimeConfigurationActivity::class)
                .build()

            // Start the DynamicConfigurationActivity for the extracted document.
            context.startActivity(intent)
        }
    }
}

class RuntimeConfigurationActivity : PdfActivity() {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        // We are adding menu items as specific examples of how to change activity configuration from within the UI.
        menuInflater.inflate(R.menu.runtime_configuration_example_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Create the new configuration based on the clicked menu item.
        val newConfiguration = when (item.itemId) {
            R.id.toggle_night_mode -> {
                // This example toggles activity display between day and night modes.
                // We'll base our state on the invert colors property of the configuration.
                val isNightMode = configuration.configuration.isInvertColors
                val themeId = if (!isNightMode) R.style.PSPDFCatalog_Theme_Dark else R.style.PSPDFCatalog_Theme

                // Provide theme resource id when constructing the configuration builder.
                PdfActivityConfiguration.Builder(configuration)
                    // Invert document colors in night mode.
                    .invertColors(!isNightMode)
                    .theme(themeId)
                    .build()
            }
            R.id.toggle_scroll_direction -> {
                // This example toggles between horizontal and vertical page scroll direction.
                PdfActivityConfiguration.Builder(configuration)
                    .scrollDirection(
                        if (configuration.configuration.scrollDirection == PageScrollDirection.HORIZONTAL) PageScrollDirection.VERTICAL else PageScrollDirection.HORIZONTAL
                    )
                    .build()
            }
            R.id.toggle_scroll_mode -> {
                // This example toggles between paginated and continuous scroll mode.
                PdfActivityConfiguration.Builder(configuration)
                    .scrollMode(if (configuration.configuration.scrollMode == PageScrollMode.PER_PAGE) PageScrollMode.CONTINUOUS else PageScrollMode.PER_PAGE)
                    .build()
            }
            else -> null
        } ?: return super.onOptionsItemSelected(item)

        // Set configuration on the activity. This will recreate the
        // activity similar to changing the screen orientation or language.
        this.configuration = newConfiguration

        // Return true to consume the action.
        return true
    }
}
