/*
 *   Copyright © 2014-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.examples.java.activities.DocumentSwitcherActivity;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.activity.TabBarHidingMode;
import com.pspdfkit.document.providers.AssetDataProvider;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/**
 * This example extends the default `PdfActivity` and adds a side navigation drawer that allows to
 * switch between documents without leaving the activity.
 */
public class DocumentSwitcherExample extends SdkExample {

    public DocumentSwitcherExample(Context context) {
        super(
                context.getString(R.string.documentSwitcherExampleTitle),
                context.getString(R.string.documentSwitcherExampleDescription));
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Use a custom activity and a custom PdfActivity subclass. To keep this example simple, we
        // turn off immersive mode.
        configuration
                .useImmersiveMode(false)
                .hideUserInterfaceWhenCreatingAnnotations(false)
                // Disable tab bar - example displays list of documents in a drawer instead.
                .setTabBarHidingMode(TabBarHidingMode.HIDE)
                // The custom layout has no content editor. In order to prevent the activity from accessing
                // it we have to deactivate it in the configuration.
                .disableContentEditing()
                .layout(R.layout.activity_document_switcher);

        // Launch the custom example activity using the (read-only) document from assets and
        // configuration.
        final Intent intent = PdfActivityIntentBuilder.fromDataProvider(
                        context, new AssetDataProvider(QUICK_START_GUIDE))
                .configuration(configuration.build())
                .activityClass(DocumentSwitcherActivity.class)
                .build();
        context.startActivity(intent);
    }
}
