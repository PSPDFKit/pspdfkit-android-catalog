/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.tasks.ExtractAssetTask
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.listeners.OnToolbarMenuChangedListener
import com.pspdfkit.ui.PdfUiFragment
import com.pspdfkit.ui.PdfUiFragmentBuilder
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * This example shows how to use [PdfUiFragment] inside an [AppCompatActivity] (not
 * [com.pspdfkit.ui.PdfActivity]) and customize the main toolbar by adding custom menu options
 * using [OnToolbarMenuChangedListener].
 */
class CustomMainToolbarExample(context: Context) : SdkExample(
    context,
    R.string.customMainToolbarExampleTitle,
    R.string.customMainToolbarExampleDescription
) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        ExtractAssetTask.extract(WELCOME_DOC, title, context) { documentFile ->
            val intent = Intent(context, CustomMainToolbarActivity::class.java)
            intent.putExtra(CustomMainToolbarActivity.EXTRA_URI, Uri.fromFile(documentFile))
            intent.putExtra(CustomMainToolbarActivity.EXTRA_CONFIGURATION, configuration.build())
            context.startActivity(intent)
        }
    }
}

/**
 * Activity that hosts a [PdfUiFragment] with custom main toolbar options.
 * Implements [OnToolbarMenuChangedListener] which is auto-discovered by the fragment in onAttach.
 * Extends [AppCompatActivity] instead of [com.pspdfkit.ui.PdfActivity] to demonstrate standalone
 * fragment usage.
 */
class CustomMainToolbarActivity :
    AppCompatActivity(),
    OnToolbarMenuChangedListener {

    private var pdfUiFragment: PdfUiFragment? = null
    private var optionsExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_main_toolbar)

        if (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) == null) {
            var configuration: PdfActivityConfiguration? =
                intent.getSupportParcelableExtra(EXTRA_CONFIGURATION, PdfActivityConfiguration::class.java)
            if (configuration == null) {
                configuration = PdfActivityConfiguration.Builder(this).build()
            }

            val uri = intent.getSupportParcelableExtra(EXTRA_URI, Uri::class.java)

            pdfUiFragment = PdfUiFragmentBuilder.fromUri(this, uri)
                .configuration(configuration)
                .build()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, pdfUiFragment!!, FRAGMENT_TAG)
                .commit()
        } else {
            pdfUiFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as PdfUiFragment
        }
    }

    override fun onCreateToolbarMenu(menu: Menu) {
        if (optionsExpanded) {
            menu.add(0, R.id.custom_action_hide, 0, "Hide")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.add(0, R.id.custom_action1, 0, "Option 1")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.add(0, R.id.custom_action2, 0, "Option 2")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            menu.add(0, R.id.custom_action3, 0, "Option 3")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        } else {
            menu.add(0, R.id.custom_action_show, 0, "Show")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
    }
    override fun onPrepareToolbarMenu(menu: Menu) {
        // No-op: menu item state is set during creation.
    }

    override fun onToolbarMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.custom_action_show -> {
                optionsExpanded = true
                pdfUiFragment?.invalidateMenu()
                true
            }
            R.id.custom_action_hide -> {
                optionsExpanded = false
                pdfUiFragment?.invalidateMenu()
                true
            }
            R.id.custom_action1 -> {
                Toast.makeText(this, "Option 1 selected", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.custom_action2 -> {
                Toast.makeText(this, "Option 2 selected", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.custom_action3 -> {
                Toast.makeText(this, "Option 3 selected", Toast.LENGTH_SHORT).show()
                true
            }
            else -> false
        }
    }

    companion object {
        const val EXTRA_URI = "CustomMainToolbarActivity.DocumentUri"
        const val EXTRA_CONFIGURATION = "CustomMainToolbarActivity.PdfConfiguration"
        private const val FRAGMENT_TAG = "customPdfUiFragment"
    }
}
