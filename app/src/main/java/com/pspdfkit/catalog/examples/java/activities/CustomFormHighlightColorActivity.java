/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.pspdfkit.catalog.R;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivity;

public class CustomFormHighlightColorActivity extends PdfActivity {

    private static final int TOGGLE_FORM_HIGHLIGHT_COLOR_MENU_ITEM_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
