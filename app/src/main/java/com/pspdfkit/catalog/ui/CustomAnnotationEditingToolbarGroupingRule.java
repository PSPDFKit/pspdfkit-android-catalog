/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.examples.java.activities.AnnotationFlagsActivity;
import com.pspdfkit.catalog.examples.java.activities.AnnotationOverlayActivity;
import com.pspdfkit.ui.toolbar.grouping.presets.AnnotationEditingToolbarGroupingRule;
import com.pspdfkit.ui.toolbar.grouping.presets.MenuItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Grouping rule for annotation editing toolbar with our custom menu item. See {@link
 * AnnotationFlagsActivity} or {@link AnnotationOverlayActivity} for usage example.
 */
public class CustomAnnotationEditingToolbarGroupingRule extends AnnotationEditingToolbarGroupingRule {

    public CustomAnnotationEditingToolbarGroupingRule(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public List<MenuItem> getGroupPreset(int capacity, int itemsCount) {
        // Copy preset from default grouping rule while making room for additional custom item.
        List<com.pspdfkit.ui.toolbar.grouping.presets.MenuItem> groupPreset =
                new ArrayList<>(super.getGroupPreset(capacity - 1, itemsCount - 1));

        // Add our custom item to the grouping preset.
        groupPreset.add(new com.pspdfkit.ui.toolbar.grouping.presets.MenuItem(R.id.pspdf_menu_custom));

        return groupPreset;
    }
}
