/*
 *   Copyright © 2016-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui;

import android.content.Context;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.ui.toolbar.ContextualToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.ui.toolbar.grouping.presets.MenuItem;
import com.pspdfkit.ui.toolbar.grouping.presets.PresetMenuItemGroupingRule;
import java.util.ArrayList;
import java.util.List;

public class CustomAnnotationCreationToolbarGroupingRule extends PresetMenuItemGroupingRule {

    /** Annotation toolbar grouping with 4 elements. */
    private static final List<MenuItem> FOUR_ITEMS_GROUPING = new ArrayList<>(4);
    /** Annotation toolbar grouping with 7 elements. */
    private static final List<MenuItem> SEVEN_ELEMENTS_GROUPING = new ArrayList<>(7);

    static {
        // Make sure our custom item is included.
        FOUR_ITEMS_GROUPING.add(new MenuItem(R.id.pspdf_menu_custom));
        FOUR_ITEMS_GROUPING.add(
                new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_markup, new int[] {
                    com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_highlight,
                    com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_squiggly,
                    com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_strikeout,
                    com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_underline
                }));
        FOUR_ITEMS_GROUPING.add(new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_picker));
        FOUR_ITEMS_GROUPING.add(new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_image));
    }

    static {
        // Make sure our custom item is included.
        SEVEN_ELEMENTS_GROUPING.add(new MenuItem(R.id.pspdf_menu_custom));
        SEVEN_ELEMENTS_GROUPING.add(new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_highlight));
        SEVEN_ELEMENTS_GROUPING.add(new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_squiggly));
        SEVEN_ELEMENTS_GROUPING.add(new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_strikeout));
        SEVEN_ELEMENTS_GROUPING.add(new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_underline));
        SEVEN_ELEMENTS_GROUPING.add(new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_picker));
        SEVEN_ELEMENTS_GROUPING.add(new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_image));
    }

    public CustomAnnotationCreationToolbarGroupingRule(@NonNull Context context) {
        super(context);
    }

    @Override
    @NonNull
    public List<ContextualToolbarMenuItem> groupMenuItems(
            @NonNull List<ContextualToolbarMenuItem> flatItems, @IntRange(from = 4) int capacity) {
        // Let's say we want to have all items at the beginning(start) of the toolbar, and only our
        // item and image at the end.
        for (ContextualToolbarMenuItem item : flatItems) {
            if (item.getId() == com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_image
                    || item.getId() == R.id.pspdf_menu_custom) {
                item.setPosition(ContextualToolbarMenuItem.Position.END);
            } else {
                item.setPosition(ContextualToolbarMenuItem.Position.START);
            }
        }

        return super.groupMenuItems(flatItems, capacity);
    }

    @NonNull
    @Override
    public List<MenuItem> getGroupPreset(
            @IntRange(from = ContextualToolbar.MIN_TOOLBAR_CAPACITY) int capacity, int itemsCount) {
        // Capacity shouldn't be less than 4. If that is the case, return empty list.
        if (capacity < ContextualToolbar.MIN_TOOLBAR_CAPACITY) return new ArrayList<>(capacity);

        final List<MenuItem> presetGrouping;
        if (capacity <= 7) {
            presetGrouping = FOUR_ITEMS_GROUPING;
        } else {
            presetGrouping = SEVEN_ELEMENTS_GROUPING;
        }

        return presetGrouping;
    }

    @Override
    public boolean areGeneratedGroupItemsSelectable() {
        return true;
    }
}
