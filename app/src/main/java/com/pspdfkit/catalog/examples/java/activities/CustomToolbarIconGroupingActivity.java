/*
 *   Copyright © 2016-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.ui.CustomAnnotationCreationToolbarGroupingRule;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.toolbar.AnnotationCreationToolbar;
import com.pspdfkit.ui.toolbar.AnnotationEditingToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.ui.toolbar.DocumentEditingToolbar;
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout;
import java.util.List;

/**
 * This examples shows how to add items to the toolbar and apply a custom grouping rule. See {@link
 * CustomAnnotationCreationToolbarGroupingRule} for the applied grouping.
 */
public class CustomToolbarIconGroupingActivity extends PdfActivity
        implements ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOnContextualToolbarLifecycleListener(this);
    }

    @Override
    public void onPrepareContextualToolbar(@NonNull ContextualToolbar toolbar) {
        if (toolbar instanceof AnnotationCreationToolbar) {
            toolbar.setDraggable(false);
            toolbar.setUseBackButtonForCloseWhenHorizontal(true);
            toolbar.setMenuItemGroupingRule(new CustomAnnotationCreationToolbarGroupingRule(this));

            // Get the existing menu items so we can add our item later.
            final List<ContextualToolbarMenuItem> menuItems = ((AnnotationCreationToolbar) toolbar).getMenuItems();

            // Create our custom menu item.
            final ContextualToolbarMenuItem customItem = ContextualToolbarMenuItem.createSingleItem(
                    this,
                    R.id.pspdf_menu_custom,
                    ContextCompat.getDrawable(this, R.drawable.ic_bookmark_outline),
                    "Bookmark",
                    Color.WHITE,
                    Color.WHITE,
                    ContextualToolbarMenuItem.Position.START,
                    false);

            // Tell the toolbar about our new item.
            menuItems.add(customItem);
            toolbar.setMenuItems(menuItems);

            // Add a listener so we can handle clicking on our item.
            toolbar.setOnMenuItemClickListener((toolbar1, menuItem) -> {
                if (menuItem.getId() == R.id.pspdf_menu_custom) {
                    Toast.makeText(CustomToolbarIconGroupingActivity.this, "Custom Action clicked", Toast.LENGTH_SHORT)
                            .show();

                    // You can even hide menu items. This shows how to toggle image menu
                    // item by clicking on the custom button.
                    ContextualToolbarMenuItem imageItem =
                            toolbar1.findItemById(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_image);
                    if (imageItem != null) {
                        toolbar1.setMenuItemVisibility(
                                com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_image,
                                imageItem.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                    }
                    return true;
                }
                return false;
            });
        } else if (toolbar instanceof AnnotationEditingToolbar) {
            // This shows how to hide annotation note button for all annotations.
            toolbar.setMenuItemVisibility(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_note, View.GONE);
        } else if (toolbar instanceof DocumentEditingToolbar) {
            // This shows how to hide close button in document editor.
            toolbar.getCloseButton().setVisibility(View.GONE);
        }
    }

    @Override
    public void onDisplayContextualToolbar(@NonNull ContextualToolbar toolbar) {}

    @Override
    public void onRemoveContextualToolbar(@NonNull ContextualToolbar toolbar) {}
}
