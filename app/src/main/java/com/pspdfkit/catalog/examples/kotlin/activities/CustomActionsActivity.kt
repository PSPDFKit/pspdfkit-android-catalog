/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.pspdfkit.catalog.R
import com.pspdfkit.ui.PdfActivity
import java.util.ArrayList

/**
 * This subclass of [PdfActivity] adds a set of custom actions.
 */
class CustomActionsActivity : PdfActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access any previously added intent extras as with normal activities.
        val receivedString = intent.getStringExtra(STRING_SAMPLE_ARG)
        Toast.makeText(this, receivedString, Toast.LENGTH_SHORT).show()
    }

    /**
     * Override this method to get the list od menu item ids as they're gonna be ordered by default
     * so you can add your own menu item ids that you can later edit in [.onCreateOptionsMenu]
     * or [.onPrepareOptionsMenu].
     */
    override fun onGenerateMenuItemIds(menuItems: MutableList<Int>): List<Int> {
        // For example let's say we want to add custom menu items after the outline button.
        // First, we get an index of outline button (all default button ids can be retrieved
        // via MENU_OPTION_* variables defined in the PdfActivity.
        val indexOfOutlineButton = menuItems.indexOf(PdfActivity.MENU_OPTION_OUTLINE)

        // Generate our custom item ids.
        val customItems = ArrayList<Int>()
        customItems.add(R.id.custom_action1)
        customItems.add(R.id.custom_action2)
        customItems.add(R.id.custom_action3)

        // Add items after the outline button.
        menuItems.addAll(indexOfOutlineButton + 1, customItems)

        // Return new menu items order.
        return menuItems
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // This will populate menu with items ordered as specified in onGenerateMenuItemIds().
        super.onCreateOptionsMenu(menu)

        // Edit first button.
        val menuItem1 = menu.findItem(R.id.custom_action1)
        menuItem1.title = "Menu Item 1"
        menuItem1.setIcon(R.drawable.ic_arrow_left)

        // Edit second button.
        val menuItem2 = menu.findItem(R.id.custom_action2)
        menuItem2.title = "Menu Item 2"
        menuItem2.setIcon(R.drawable.ic_arrow_right)

        // Edit third button.
        val menuItem3 = menu.findItem(R.id.custom_action3)
        menuItem3.title = "Menu Item 3"
        menuItem3.setIcon(R.drawable.ic_collaborate)

        // Let's say we want to tint icons same as the default ones). We can read the color
        // from the theme, or specify the same color we have in theme. Reading from theme is a bit
        // more complex but a better way to do it, so here's how to:
        val a = theme.obtainStyledAttributes(
            null,
            com.pspdfkit.R.styleable.pspdf__ActionBarIcons,
            com.pspdfkit.R.attr.pspdf__actionBarIconsStyle,
            com.pspdfkit.R.style.PSPDFKit_ActionBarIcons
        )
        val mainToolbarIconsColor = a.getColor(com.pspdfkit.R.styleable.pspdf__ActionBarIcons_pspdf__iconsColor, ContextCompat.getColor(this, R.color.white))
        a.recycle()

        // Tinting all custom menu drawables (you can do it the easier way if you iterate over your ids).
        val icon1 = menuItem1.icon
        icon1?.let { DrawableCompat.setTint(it, mainToolbarIconsColor) }
        menuItem1.icon = icon1

        val icon2 = menuItem2.icon
        icon2?.let { DrawableCompat.setTint(it, mainToolbarIconsColor) }
        menuItem2.icon = icon2

        val icon3 = menuItem3.icon
        icon3?.let { DrawableCompat.setTint(it, mainToolbarIconsColor) }
        menuItem3.icon = icon3

        // All our menu items are marked as SHOW_AS_ALWAYS. If you want to just show the first 4
        // items for example and send others to the overflow, you can simply do:
        for (i in 0 until menu.size()) {
            menu.getItem(i).setShowAsAction(if (i < 4) MenuItem.SHOW_AS_ACTION_IF_ROOM else MenuItem.SHOW_AS_ACTION_NEVER)
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Here, you can edit your items when the menu is being invalidated.
        // To invalidate menu, call supportInvalidateOptionsMenu();
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * Override onOptionsItemSelected(MenuItem) to handle click events for your custom menu items.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val handled = when (item.itemId) {
            R.id.custom_action1 -> {
                Toast.makeText(this, "Selected Action 1", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.custom_action2 -> {
                Toast.makeText(this, "Selected Action 2", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.custom_action3 -> {
                Toast.makeText(this, "Selected Action 3", Toast.LENGTH_SHORT).show()
                true
            }

            else -> {
                false
            }
        }

        // Return true if you have handled the current event. If your code has not handled the event,
        // pass it on to the superclass. This is important or standard PSPDFKit actions won't work.
        return handled || super.onOptionsItemSelected(item)
    }

    companion object {
        const val STRING_SAMPLE_ARG = "some_string_extra"
    }
}
