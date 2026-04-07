/*
 *   Copyright © 2017-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import static com.pspdfkit.catalog.tasks.ExtractAssetTask.extract;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/** Shows how to toggle the form highlight color. */
public class CustomFormHighlightColorExample extends SdkExample {
    public CustomFormHighlightColorExample(@NonNull final Context context) {
        super(
                context,
                R.string.customFormHighlightColorExampleTitle,
                R.string.customFormHighlightColorExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        configuration
                // Turn off saving, so we have the clean original document every time the example is
                // launched.
                .autosaveEnabled(false)
                .formEditingEnabled(true)
                .build();

        // Extract the document from the assets.
        extract("Form_example.pdf", getTitle(), context, documentFile -> {
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(CustomFormHighlightColorActivity.class)
                    .build();
            context.startActivity(intent);
        });
    }

    public static class CustomFormHighlightColorActivity extends PdfActivity {

        private static final int TOGGLE_FORM_HIGHLIGHT_COLOR_MENU_ITEM_ID = 1;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public boolean onCreateOptionsMenu(@NonNull Menu menu) {
            super.onCreateOptionsMenu(menu);
            MenuItem menuItem = menu.add(0, TOGGLE_FORM_HIGHLIGHT_COLOR_MENU_ITEM_ID, 0, "Toggle highlight color");
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            final Drawable tintedDrawable =
                    DrawableCompat.wrap(ContextCompat.getDrawable(this, com.pspdfkit.R.drawable.pspdf__ic_highlight));
            DrawableCompat.setTint(tintedDrawable, Color.WHITE);
            menuItem.setIcon(tintedDrawable);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == TOGGLE_FORM_HIGHLIGHT_COLOR_MENU_ITEM_ID) {
                toggleFormHighlightColor();
                return true;
            } else {
                return super.onOptionsItemSelected(item);
            }
        }

        /**
         * Toggles form highlight color between {@link Color#TRANSPARENT} and highlight color set in the
         * theme.
         */
        private void toggleFormHighlightColor() {
            if (getConfiguration().getTheme() == PdfActivityConfiguration.NO_THEME) {
                setConfiguration(new PdfActivityConfiguration.Builder(getConfiguration())
                        .theme(R.style.PSPDFCatalog_Theme_FormSelectionNoHighlight)
                        .build());
            } else {
                setConfiguration(new PdfActivityConfiguration.Builder(getConfiguration())
                        .theme(PdfActivityConfiguration.NO_THEME)
                        .build());
            }
        }
    }
}
