/*
 *   Copyright © 2019-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;
import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import com.pspdfkit.annotations.configuration.AnnotationConfigurationRegistry;
import com.pspdfkit.annotations.configuration.LineAnnotationConfiguration;
import com.pspdfkit.catalog.R;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfFragment;
import com.pspdfkit.ui.special_mode.controller.AnnotationTool;
import com.pspdfkit.ui.special_mode.controller.AnnotationToolVariant;
import com.pspdfkit.ui.toolbar.AnnotationCreationToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbar;
import com.pspdfkit.ui.toolbar.ContextualToolbarMenuItem;
import com.pspdfkit.ui.toolbar.ToolbarCoordinatorLayout;
import com.pspdfkit.ui.toolbar.grouping.presets.MenuItem;
import com.pspdfkit.ui.toolbar.grouping.presets.PresetMenuItemGroupingRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This example shows how to manipulate annotation creation toolbar to add your own annotation tool
 * items. In this example we add three line annotation tools, each of them for drawing lines of
 * different colors.
 */
public class CustomAnnotationCreationToolbarActivity extends PdfActivity
        implements ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener,
                AnnotationCreationToolbar.ItemToAnnotationToolMapper {

    // Variants of the line tool that we will add.
    private static final String VARIANT_NAME_LINE_BLUE = "blue_line";
    private static final String VARIANT_NAME_LINE_RED = "red_line";
    private static final String VARIANT_NAME_LINE_YELLOW = "yellow_line";

    // Icons for custom toolbar items.
    @Nullable
    private Drawable blueLineIcon;

    @Nullable
    private Drawable redLineIcon;

    @Nullable
    private Drawable yellowLineIcon;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // First we set the listener for the toolbar lifecycle changes so we
        // can observe when the toolbar is being prepared, shown or dismissed
        // inside the activity's UI.
        setOnContextualToolbarLifecycleListener(this);

        blueLineIcon = AppCompatResources.getDrawable(this, com.pspdfkit.R.drawable.pspdf__ic_line);
        // The red line uses a customized colored drawable. So tinting must be disabled.
        redLineIcon = AppCompatResources.getDrawable(this, R.drawable.ic_line_red);
        yellowLineIcon = AppCompatResources.getDrawable(this, com.pspdfkit.R.drawable.pspdf__ic_line);
    }

    @Override
    public void onDocumentLoaded(@NonNull final PdfDocument document) {
        super.onDocumentLoaded(document);

        final PdfFragment pdfFragment = getPdfFragment();
        if (pdfFragment != null) {
            // Here we set editing defaults for the items we're gonna create.
            final AnnotationConfigurationRegistry annotationConfiguration = pdfFragment.getAnnotationConfiguration();

            // Blue line configuration.
            annotationConfiguration.put(
                    AnnotationTool.LINE,
                    AnnotationToolVariant.fromName(VARIANT_NAME_LINE_BLUE),
                    LineAnnotationConfiguration.builder(this) // Use preset configuration for creating lines.
                            .setDefaultColor(Color.BLUE) // Use blue as a default color.
                            .setAvailableColors(Collections.singletonList(Color.BLUE)) // Only allow blue to be set.
                            .setForceDefaults(true) // Always force these defaults.
                            .setCustomColorPickerEnabled(false)
                            .build());

            // Red line configuration.
            annotationConfiguration.put(
                    AnnotationTool.LINE,
                    AnnotationToolVariant.fromName(VARIANT_NAME_LINE_RED),
                    LineAnnotationConfiguration.builder(this) // Use preset configuration for creating lines.
                            .setDefaultColor(Color.RED) // Use red as a default color.
                            .setAvailableColors(Collections.singletonList(Color.RED)) // Only allow red to be set.
                            .setForceDefaults(true) // Always force these defaults.
                            .setCustomColorPickerEnabled(false)
                            .build());

            // Yellow line configuration.
            annotationConfiguration.put(
                    AnnotationTool.LINE,
                    AnnotationToolVariant.fromName(VARIANT_NAME_LINE_YELLOW),
                    LineAnnotationConfiguration.builder(this) // Use preset configuration for creating lines.
                            .setDefaultColor(Color.YELLOW) // Use yellow as a default color.
                            .setAvailableColors(Collections.singletonList(Color.YELLOW)) // Only allow yellow to be set.
                            .setForceDefaults(true) // Always force these defaults.
                            .setCustomColorPickerEnabled(false)
                            .build());
        }
    }

    @Override // ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener
    public void onPrepareContextualToolbar(@NonNull final ContextualToolbar toolbar) {
        if (toolbar instanceof AnnotationCreationToolbar annotationCreationToolbar) {
            // Register this class as a mapper (see implemented methods below).
            annotationCreationToolbar.setItemToAnnotationToolMapper(this);

            // Register grouping rule to tell toolbar how to group menu items.
            annotationCreationToolbar.setMenuItemGroupingRule(new CustomAnnotationCreationToolbarGroupingRule(this));

            final List<ContextualToolbarMenuItem> customMenuItems = new ArrayList<>();

            if (blueLineIcon != null) {
                final ContextualToolbarMenuItem blueLineItem = ContextualToolbarMenuItem.createSingleItem(
                        this,
                        R.id.line_blue,
                        blueLineIcon,
                        "Line: Blue",
                        Color.GRAY,
                        Color.BLUE,
                        ContextualToolbarMenuItem.Position.START,
                        true);
                customMenuItems.add(blueLineItem);
            }

            if (redLineIcon != null) {
                final ContextualToolbarMenuItem redLineItem = ContextualToolbarMenuItem.createSingleItem(
                        this,
                        R.id.line_red,
                        redLineIcon,
                        "Line: Red",
                        Color.WHITE,
                        Color.BLUE,
                        ContextualToolbarMenuItem.Position.START,
                        true);
                // Disabling tinting as the red line drawable
                // is a colored one.
                redLineItem.setTintingEnabled(false);
                customMenuItems.add(redLineItem);
            }

            if (yellowLineIcon != null) {
                final ContextualToolbarMenuItem yellowLineItem = ContextualToolbarMenuItem.createSingleItem(
                        this,
                        R.id.line_yellow,
                        yellowLineIcon,
                        "Line: Yellow",
                        Color.GRAY,
                        Color.BLUE,
                        ContextualToolbarMenuItem.Position.START,
                        true);
                customMenuItems.add(yellowLineItem);
            }

            // Before we pass custom items, we need to include our default ones as well.
            customMenuItems.addAll(annotationCreationToolbar.getMenuItems());
            annotationCreationToolbar.setMenuItems(customMenuItems);
        }
    }

    @Override // ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener
    public void onDisplayContextualToolbar(@NonNull final ContextualToolbar toolbar) {}

    @Override // ToolbarCoordinatorLayout.OnContextualToolbarLifecycleListener
    public void onRemoveContextualToolbar(@NonNull final ContextualToolbar toolbar) {}

    @NonNull
    @Override // AnnotationCreationToolbar.ItemToAnnotationToolMapper
    public SparseArray<Pair<AnnotationTool, AnnotationToolVariant>> getItemToAnnotationToolMapping() {
        // We need to add mappings to let toolbar know which item should handle which tool/variant.
        final SparseArray<Pair<AnnotationTool, AnnotationToolVariant>> itemToAnnotationToolMapping =
                new SparseArray<>();
        itemToAnnotationToolMapping.put(
                R.id.line_blue,
                new Pair<>(AnnotationTool.LINE, AnnotationToolVariant.fromName(VARIANT_NAME_LINE_BLUE)));
        itemToAnnotationToolMapping.put(
                R.id.line_red, new Pair<>(AnnotationTool.LINE, AnnotationToolVariant.fromName(VARIANT_NAME_LINE_RED)));
        itemToAnnotationToolMapping.put(
                R.id.line_yellow,
                new Pair<>(AnnotationTool.LINE, AnnotationToolVariant.fromName(VARIANT_NAME_LINE_YELLOW)));
        return itemToAnnotationToolMapping;
    }

    @Override // AnnotationCreationToolbar.ItemToAnnotationToolMapper
    public boolean isStyleIndicatorCircleEnabled(final int itemId) {
        // We want to show style indicators for all our custom items and not show it for all the
        // default ones.
        return getItemToAnnotationToolMapping().get(itemId) != null;
    }

    /**
     * Class that implements the {@link PresetMenuItemGroupingRule}, used to tell the toolbar how to
     * group menu items in the toolbar.
     *
     * @see com.pspdfkit.catalog.ui.CustomAnnotationCreationToolbarGroupingRule for more details.
     */
    private static class CustomAnnotationCreationToolbarGroupingRule extends PresetMenuItemGroupingRule {

        private final List<MenuItem> CUSTOM_GROUPING = new ArrayList<>(4);

        CustomAnnotationCreationToolbarGroupingRule(@NonNull final Context context) {
            super(context);

            // This adds our default markup items under out default markup group item.
            CUSTOM_GROUPING.add(
                    new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_markup, new int[] {
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_highlight,
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_squiggly,
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_strikeout,
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_underline,
                        com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_line
                    }));

            // This adds line items under our group item ('writing' in this case). You can also use
            // your own
            // but then you need to add them to the menu items.
            CUSTOM_GROUPING.add(new MenuItem(
                    com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_group_writing,
                    new int[] {R.id.line_blue, R.id.line_red, R.id.line_yellow}));

            // Some standalone item from the default framework implementation.
            CUSTOM_GROUPING.add(
                    new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_ink_highlighter));

            // To access the property inspector, the color picker item needs to be added as well.
            CUSTOM_GROUPING.add(new MenuItem(com.pspdfkit.R.id.pspdf__annotation_creation_toolbar_item_picker));
        }

        @NonNull
        @Override
        public List<MenuItem> getGroupPreset(final int capacity, final int itemsCount) {
            // Capacity shouldn't be less than 4. If that is the case, return empty list.
            if (capacity < ContextualToolbar.MIN_TOOLBAR_CAPACITY) return new ArrayList<>(capacity);

            // Return our custom groupings for all other capacities.
            return CUSTOM_GROUPING;
        }

        @Override
        public boolean areGeneratedGroupItemsSelectable() {
            return true;
        }
    }
}
