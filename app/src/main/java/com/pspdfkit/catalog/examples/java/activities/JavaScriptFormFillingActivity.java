/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import com.pspdfkit.annotations.actions.JavaScriptAction;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.ui.PdfActivity;

/** This activity shows how to fill document forms via JavaScript */
public class JavaScriptFormFillingActivity extends PdfActivity {

    private static final int RESET_FORM_MENU_ITEM_ID = 1;

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, RESET_FORM_MENU_ITEM_ID, 0, "Reset form");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == RESET_FORM_MENU_ITEM_ID) {
            resetForm();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @UiThread
    @Override
    public void onDocumentLoaded(@NonNull final PdfDocument document) {
        super.onDocumentLoaded(document);
        fillForms();
    }

    /** Shows how to fill form fields via JavaScript. */
    private void fillForms() {
        // Form fields can be queried by their fully qualified name.
        // Select the MALE radio option (first option).
        executeJavaScript("var f = doc.getField('Sex'); f.checkThisBox(0, true);");

        // Set 'John' into last name text field.
        executeJavaScript("var f = doc.getField('Name_Last'); f.value = 'John';");

        // Set 'Appleseed' into first name text field.
        executeJavaScript("var f = doc.getField('Name_First'); f.value = 'Appleseed';");
    }

    /** Resets all form fields in the document to their default values. */
    private void resetForm() {
        // Calling `doc.resetForm()` without any parameters resets all form fields in the document.
        executeJavaScript("doc.resetForm()");
    }

    /**
     * Shows how to execute JavaScript scripts on the {@link com.pspdfkit.ui.PdfFragment} without
     * attaching them to annotations.
     */
    private void executeJavaScript(@NonNull String script) {
        // Create JavaScript action with required script.
        JavaScriptAction javaScriptAction = new JavaScriptAction(script);

        // Execute the action on PdfFragment.
        getPdfFragment().executeAction(javaScriptAction);
    }
}
