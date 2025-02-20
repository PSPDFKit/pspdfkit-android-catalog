/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.examples.java.activities.VerticalScrollbarExampleActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.activity.TabBarHidingMode;
import com.pspdfkit.configuration.activity.ThumbnailBarMode;
import com.pspdfkit.configuration.page.PageScrollDirection;
import com.pspdfkit.configuration.page.PageScrollMode;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.scrollbar.VerticalScrollBar;

/**
 * This example shows how to use the {@link VerticalScrollBar} widget inside a {@link PdfActivity}.
 */
public class VerticalScrollbarExample extends SdkExample {

    public VerticalScrollbarExample(@NonNull final Context context) {
        super(
                context.getString(R.string.verticalScrollbarExampleTitle),
                context.getString(R.string.verticalScrollbarExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        configuration
                // The example uses its own scrollbar implementation.
                .scrollbarsEnabled(false)
                // We use a custom layout that removes the thumbnail bar, and instead places the
                // vertical scrollbar widget.
                .layout(R.layout.custom_scrollbar_activity)
                .setThumbnailBarMode(ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE)
                .setTabBarHidingMode(TabBarHidingMode.HIDE)
                .documentTitleOverlayEnabled(false)
                .navigationButtonsEnabled(false)
                // Although the widget also works for horizontal scrolling, we switch to vertical
                // continuous scroll mode in this example.
                .scrollDirection(PageScrollDirection.VERTICAL)
                .scrollMode(PageScrollMode.CONTINUOUS)
                // The custom layout has no content editor. In order to prevent the activity from accessing
                // it we have to deactivate it in the configuration.
                .contentEditingEnabled(false)
                .setMeasurementToolsEnabled(false);

        // We use a custom utility class to extract the example document from the assets.
        ExtractAssetTask.extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(VerticalScrollbarExampleActivity.class)
                    .build();

            // Start the VerticalScrollbarExampleActivity for the extracted document.
            context.startActivity(intent);
        });
    }
}
