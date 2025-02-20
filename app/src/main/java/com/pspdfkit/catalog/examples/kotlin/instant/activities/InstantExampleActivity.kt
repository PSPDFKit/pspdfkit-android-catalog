/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.instant.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.examples.kotlin.instant.api.InstantExampleDocumentDescriptor
import com.pspdfkit.document.sharing.DocumentSharingIntentHelper
import com.pspdfkit.instant.ui.InstantPdfActivity
import com.pspdfkit.ui.actionmenu.ActionMenu
import com.pspdfkit.ui.actionmenu.ActionMenuItem
import com.pspdfkit.ui.actionmenu.ActionMenuListener
import com.pspdfkit.ui.actionmenu.FixedActionMenuItem
import com.pspdfkit.ui.actionmenu.SharingMenu
import com.pspdfkit.utils.getSupportParcelableExtra

/**
 * Extends [InstantPdfActivity] with the ability to share the Instant document with other users.
 */
class InstantExampleActivity : InstantPdfActivity(), ActionMenuListener {
    /** Descriptor for the displayed document.  */
    private lateinit var documentDescriptor: InstantExampleDocumentDescriptor

    /** Menu with collaborate sharing actions.  */
    private lateinit var collaborateMenu: SharingMenu

    /** Main toolbar icons color.  */
    private var mainToolbarIconsColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val documentDescriptor: InstantExampleDocumentDescriptor? = intent.getSupportParcelableExtra(
            DOCUMENT_DESCRIPTOR,
            InstantExampleDocumentDescriptor::class.java
        )
        checkNotNull(documentDescriptor) { "InstantExampleActivity was not initialized with proper arguments: Missing document descriptor extra!" }
        this.documentDescriptor = documentDescriptor

        val a = theme.obtainStyledAttributes(null, com.pspdfkit.R.styleable.pspdf__ActionBarIcons, com.pspdfkit.R.attr.pspdf__actionBarIconsStyle, com.pspdfkit.R.style.PSPDFKit_ActionBarIcons)
        mainToolbarIconsColor = a.getColor(com.pspdfkit.R.styleable.pspdf__ActionBarIcons_pspdf__iconsColor, ContextCompat.getColor(this, R.color.white))
        a.recycle()

        initCollaborateMenu()
    }

    private fun initCollaborateMenu() {
        collaborateMenu = SharingMenu(this, null)
        collaborateMenu.setTitle(getString(R.string.instant_collaborate))

        collaborateMenu.addMenuItem(FixedActionMenuItem(this, R.id.open_in_browser, R.drawable.ic_open_in_browser, R.string.instant_open_in_browser))
        collaborateMenu.addMenuItem(FixedActionMenuItem(this, R.id.share_document_link, com.pspdfkit.R.drawable.pspdf__ic_open_in, R.string.instant_share_document_link))

        collaborateMenu.addActionMenuListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        val collaborateMenuItem = menu.add(0, R.id.instant_collaborate, 0, getString(R.string.instant_collaborate))
        collaborateMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        val collaborateMenuItem = menu.findItem(R.id.instant_collaborate)
        collaborateMenuItem.isEnabled = document != null

        collaborateMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_collaborate)?.apply {
            DrawableCompat.setTint(this, mainToolbarIconsColor)
            alpha = if (document != null) 255 else 128
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.instant_collaborate) {
            collaborateMenu.show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareActionMenu(actionMenu: ActionMenu): Boolean {
        return true
    }

    override fun onDisplayActionMenu(actionMenu: ActionMenu) {}

    override fun onRemoveActionMenu(actionMenu: ActionMenu) {}

    override fun onActionMenuItemLongClicked(actionMenu: ActionMenu, menuItem: ActionMenuItem): Boolean {
        return false
    }

    override fun onActionMenuItemClicked(actionMenu: ActionMenu, menuItem: ActionMenuItem): Boolean {
        if (menuItem.itemId == R.id.share_document_link) {
            // Sharing link to the Instant document.
            showShareTextMenu(R.string.instant_share_document_link, documentDescriptor.webUrl)
            return true
        } else if (menuItem.itemId == R.id.open_in_browser) {
            // Opens Instant document link in the web browser.
            showOpenInBrowserMenu()
            return true
        }
        return false
    }

    private fun showOpenInBrowserMenu() {
        val shareIntent = Intent(Intent.ACTION_VIEW, Uri.parse(documentDescriptor.webUrl))
        val sharingMenu = SharingMenu(
            this,
            SharingMenu.SharingMenuListener { shareTarget ->
                shareIntent.setPackage(shareTarget.packageName)
                startActivity(shareIntent)
            }
        )
        sharingMenu.setTitle(R.string.instant_open_in_browser)
        sharingMenu.setShareIntents(listOf(shareIntent))

        collaborateMenu.dismiss()
        sharingMenu.show()
    }

    private fun showShareTextMenu(@StringRes titleRes: Int, textToShare: String) {
        val shareIntent = DocumentSharingIntentHelper.getShareTextIntent(textToShare)
        val sharingMenu = SharingMenu(
            this,
            SharingMenu.SharingMenuListener { shareTarget ->
                shareIntent.setPackage(shareTarget.packageName)
                startActivity(shareIntent)
            }
        )
        sharingMenu.setTitle(titleRes)
        sharingMenu.setShareIntents(listOf(shareIntent))

        collaborateMenu.dismiss()
        sharingMenu.show()
    }

    companion object {
        /** Name of the extra holding [InstantExampleDocumentDescriptor] of the displayed document.  */
        const val DOCUMENT_DESCRIPTOR = "InstantExampleActivity.DocumentDescriptor"
    }
}
