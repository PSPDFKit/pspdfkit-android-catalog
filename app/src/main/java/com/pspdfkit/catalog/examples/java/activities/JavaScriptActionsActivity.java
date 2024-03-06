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
import com.pspdfkit.annotations.actions.JavaScriptAction;
import com.pspdfkit.catalog.examples.java.JavaScriptActionsExample;
import com.pspdfkit.ui.PdfActivity;

/**
 * This activity shows how to create and execute JavaScript actions programmatically.
 *
 * @see JavaScriptActionsExample
 */
public class JavaScriptActionsActivity extends PdfActivity {

    private static final int SHOW_ALERT = 1;
    private static final int GO_TO_NEXT_PAGE = 2;
    private static final int SHOW_FORM_FIELDS = 3;
    private static final int MAIL_TO = 4;

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, SHOW_ALERT, 0, "Show Alert");
        menu.add(0, GO_TO_NEXT_PAGE, 0, "Go To Next Page");
        menu.add(0, SHOW_FORM_FIELDS, 0, "Show Form Fields");
        menu.add(0, MAIL_TO, 0, "Mail to");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == SHOW_ALERT) {
            executeShowAlertAction();
            return true;
        } else if (item.getItemId() == GO_TO_NEXT_PAGE) {
            executeGoToNextPageAction();
            return true;
        } else if (item.getItemId() == SHOW_FORM_FIELDS) {
            executeShowFormFieldsAction();
            return true;
        } else if (item.getItemId() == MAIL_TO) {
            executeMailDocumentAction();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /** Shows simple alert dialog through JavaScript. */
    private void executeShowAlertAction() {
        // Show simple alert dialog with message.
        executeJavaScript("app.alert('This is a JavaScript alert!')");
    }

    /** Changes to next page via JavaScript. */
    private void executeGoToNextPageAction() {
        // Switch to next page. `doc.pageNum` returns currently displayed page number.
        executeJavaScript("doc.pageNum = doc.pageNum + 1");
    }

    /** Prints list of form field names via JavaScript. */
    private void executeShowFormFieldsAction() {
        executeJavaScript("var message = 'There are ' + this.numFields + ' form fields in this document.\\n';"
                + "for (var i = 0; i < this.numFields; i++) {"
                + "   message += 'Field[' + i + '] = ' + this.getNthFieldName(i) + '\\n';"
                + "};"
                + "app.alert(message);");
    }

    /** Opens default mail client with document as attachment via JavaScript. */
    private void executeMailDocumentAction() {
        executeJavaScript("doc.mailDoc(true, "
                + "\"john@pspdfkit.com\", "
                + "\"cc@pspdfkit.com\", "
                + "\"bcc@pspdfkit.com\", "
                + "\"subject\", "
                + "\"This is a message\");");
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
