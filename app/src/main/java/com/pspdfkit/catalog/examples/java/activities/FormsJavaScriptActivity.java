/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import com.pspdfkit.annotations.actions.AnnotationTriggerEvent;
import com.pspdfkit.annotations.actions.JavaScriptAction;
import com.pspdfkit.catalog.examples.java.FormsJavaScriptExample;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.forms.CheckBoxFormConfiguration;
import com.pspdfkit.forms.PushButtonFormConfiguration;
import com.pspdfkit.forms.TextFormConfiguration;
import com.pspdfkit.ui.PdfActivity;
import java.io.IOException;
import java.io.InputStream;

/**
 * This activity shows how to create custom forms with JavaScript actions attached.
 *
 * <p><b>Note:</b> For more information about JavaScript API refer to our guides or to Adobe's <a
 * href="https://www.adobe.com/content/dam/acom/en/devnet/acrobat/pdfs/js_api_reference.pdf">JavaScript
 * for Acrobat API Reference</a>.
 *
 * @see FormsJavaScriptExample
 */
public class FormsJavaScriptActivity extends PdfActivity {

    @UiThread
    @Override
    public void onDocumentLoaded(@NonNull final PdfDocument document) {
        super.onDocumentLoaded(document);

        // Retrieve existing form elements and create form fields only when there are no forms.
        document.getFormProvider().getFormElementsAsync().subscribe((formElements, throwable) -> {
            if (!formElements.isEmpty()) return;
            createSimpleCalculationExample();
            createManualCalculationExample();
            createFormattingExample();

            createPushButtonWithImageImportAction();
            createCheckboxWithHideAction();
        });
    }

    /**
     * This example calculates sum of 2 fields via {@code AFSimple_Calculate} JavaScript function.
     * It also shows how to use formatting functions to validate keystrokes and format fields.
     */
    private void createSimpleCalculationExample() {
        final PdfDocument document = getDocument();
        if (document == null) return;

        // Create text field for the first operand.
        document.getFormProvider()
                .addFormElementToPage(
                        "sum-operand1",
                        new TextFormConfiguration.Builder(0, new RectF(30, 750, 200, 720))
                                .setAdditionalAction(
                                        // FORM_CHANGED additional action is executed for each
                                        // keystroke when editing form field.
                                        AnnotationTriggerEvent.FORM_CHANGED,
                                        // Accept only numbers with 2 decimal places, ',' as
                                        // thousands and '.' as decimal separators.
                                        new JavaScriptAction("AFNumber_Keystroke(2, 0, 0, 0, '', true);"))
                                .setAdditionalAction(
                                        // FIELD_FORMAT action is executed to format field contents
                                        // whenever form field's value changes.
                                        AnnotationTriggerEvent.FIELD_FORMAT,
                                        // Keystroke and format functions accepts the same
                                        // arguments.
                                        new JavaScriptAction("AFNumber_Format(2, 0, 0, 0, '', true);"))
                                .setText("3.0")
                                .build());

        // Create text field for the second operand.
        document.getFormProvider()
                .addFormElementToPage(
                        "sum-operand2",
                        new TextFormConfiguration.Builder(0, new RectF(30, 690, 200, 660))
                                .setAdditionalAction(
                                        AnnotationTriggerEvent.FORM_CHANGED,
                                        // Accept only numbers with 0 decimal places, and no
                                        // thousands separator.
                                        new JavaScriptAction("AFNumber_Keystroke(0, 1, 0, 0, '', true);"))
                                .setAdditionalAction(
                                        AnnotationTriggerEvent.FIELD_FORMAT,
                                        new JavaScriptAction("AFNumber_Format(0, 1, 0, 0, '', true);"))
                                .setText("4")
                                .build());

        // Create result field.
        document.getFormProvider()
                .addFormElementToPage(
                        "sum-result",
                        new TextFormConfiguration.Builder(0, new RectF(30, 630, 200, 600))
                                .setAdditionalAction(
                                        // FORM_CALCULATE additional action is executed whenever
                                        // value of any form field in the document changes.
                                        AnnotationTriggerEvent.FORM_CALCULATE,
                                        // Use AFSimple_Calculate function to perform simple
                                        // calculations for multiple source (operand) fields.
                                        // Other supported functions are: AVG (average), PRD
                                        // (product), MIN (minimum) and MAX (maximum)
                                        new JavaScriptAction(
                                                "AFSimple_Calculate('SUM', new Array('sum-operand1', 'sum-operand2'));"))
                                .setAdditionalAction(
                                        AnnotationTriggerEvent.FIELD_FORMAT,
                                        // In this example we'll format the field to have 2 decimal
                                        // places and use '.' as decimal separator.
                                        new JavaScriptAction("AFNumber_Format(2, 1, 0, 0, '', true);"))
                                // Make the result field read-only.
                                .setReadOnly(true)
                                .build());
    }

    /** Shows how to access form field values to calculate result. */
    private void createManualCalculationExample() {
        // Create text field for the first operand.
        final PdfDocument document = getDocument();
        if (document == null) return;

        document.getFormProvider()
                .addFormElementToPage(
                        "concat-operand1",
                        new TextFormConfiguration.Builder(1, new RectF(30, 750, 200, 720))
                                .setText("First")
                                .build());

        // Create text field for the second operand.
        document.getFormProvider()
                .addFormElementToPage(
                        "concat-operand2",
                        new TextFormConfiguration.Builder(1, new RectF(30, 690, 200, 660))
                                .setText("Second")
                                .build());

        // Create result field.
        document.getFormProvider()
                .addFormElementToPage(
                        "concat-result",
                        new TextFormConfiguration.Builder(1, new RectF(30, 630, 200, 600))
                                .setAdditionalAction(
                                        // FORM_CALCULATE additional action is executed whenever
                                        // value of any form field in the document changes.
                                        AnnotationTriggerEvent.FORM_CALCULATE,
                                        // Concatenate text in operand text fields and set it to the
                                        // result field.
                                        new JavaScriptAction(""
                                                + "var operand1 = doc.getField('concat-operand1');"
                                                + "var operand2 = doc.getField('concat-operand2');"
                                                + "var result = doc.getField('concat-result');"
                                                + "result.value = operand1.value + ' ' + operand2.value;"))
                                // Make the result field read-only.
                                .setReadOnly(true)
                                .build());
    }

    /**
     * Creates multiple text fields with different formatting and validation scripts to showcase
     * basic formatting options.
     */
    private void createFormattingExample() {
        final PdfDocument document = getDocument();
        if (document == null) return;

        // Create text field formatted as a number.
        // Uses AFNumber_*(nDec, sepStyle, negStyle, currStyle, strCurrency, bCurrencyPrepend)
        // function.
        document.getFormProvider()
                .addFormElementToPage(
                        "Number format",
                        new TextFormConfiguration.Builder(2, new RectF(30, 750, 200, 720))
                                .setAdditionalAction(
                                        // FORM_CHANGED additional action is executed for each
                                        // keystroke when editing form field.
                                        AnnotationTriggerEvent.FORM_CHANGED,
                                        // Accept only numbers with 2 decimal places, ',' as
                                        // thousands and '.' as decimal separators.
                                        new JavaScriptAction("AFNumber_Keystroke(2, 0, 0, 0, '', true);"))
                                .setAdditionalAction(
                                        // FIELD_FORMAT action is executed whenever form field's
                                        // value changes to format it's contents.
                                        AnnotationTriggerEvent.FIELD_FORMAT,
                                        // Keystroke and format functions accept the same arguments.
                                        new JavaScriptAction("AFNumber_Format(2, 0, 0, 0, '', true);"))
                                .setText("3.2293")
                                .build());

        // Create text field formatted as a currency.
        document.getFormProvider()
                .addFormElementToPage(
                        "Currency format",
                        new TextFormConfiguration.Builder(2, new RectF(30, 690, 200, 660))
                                .setAdditionalAction(
                                        AnnotationTriggerEvent.FORM_CHANGED,
                                        new JavaScriptAction("AFNumber_Keystroke(2, 3, 0, 0, '€', true);"))
                                .setAdditionalAction(
                                        AnnotationTriggerEvent.FIELD_FORMAT,
                                        new JavaScriptAction("AFNumber_Format(2, 3, 0, 0, '€', true);"))
                                .setText("33,99")
                                .build());

        // Create text field formatted as a date.
        document.getFormProvider()
                .addFormElementToPage(
                        "Date format",
                        new TextFormConfiguration.Builder(2, new RectF(30, 630, 200, 600))
                                .setAdditionalAction(
                                        AnnotationTriggerEvent.FORM_CHANGED,
                                        new JavaScriptAction("AFDate_KeystrokeEx('mm/dd/yyyy');"))
                                .setAdditionalAction(
                                        AnnotationTriggerEvent.FIELD_FORMAT,
                                        new JavaScriptAction("AFDate_FormatEx('mm/dd/yyyy');"))
                                .setText("02/07/2018")
                                .build());

        // Create text field formatted as a time.
        document.getFormProvider()
                .addFormElementToPage(
                        "Time format",
                        new TextFormConfiguration.Builder(2, new RectF(30, 570, 200, 540))
                                .setAdditionalAction(
                                        AnnotationTriggerEvent.FORM_CHANGED,
                                        new JavaScriptAction("AFTime_Keystroke('HH:MM::ss');"))
                                .setAdditionalAction(
                                        AnnotationTriggerEvent.FIELD_FORMAT,
                                        new JavaScriptAction("AFTime_Format('HH:MM::ss');"))
                                .setText("12:00:20")
                                .build());
    }

    /** Creates a push button form field that will execute importImageButton action when clicked. */
    private void createPushButtonWithImageImportAction() {
        final PdfDocument document = getDocument();
        if (document == null) return;

        // Create push button configuration builder.
        PushButtonFormConfiguration.Builder builder = new PushButtonFormConfiguration.Builder(
                        3,
                        new RectF(30, 750, 120, 660),
                        // Show default bitmap.
                        getBitmapFromAsset(getAssets(), "images/android.png"))
                .setAction(new JavaScriptAction("var f=this.getField('pushbuttonfield'); f.buttonImportIcon();"));

        document.getFormProvider().addFormElementToPage("pushbuttonfield", builder.build());
    }

    /**
     * Creates a checkbox form field that toggles visibility of push button created in {@link
     * #createPushButtonWithImageImportAction()} when checked.
     */
    private void createCheckboxWithHideAction() {
        final PdfDocument document = getDocument();
        if (document == null) return;

        CheckBoxFormConfiguration.Builder builder = new CheckBoxFormConfiguration.Builder(
                        3, new RectF(150, 690, 180, 660))
                .deselect()
                .setAdditionalAction(
                        // MOUSE_UP additional action is executed after clicking on the form
                        // field.
                        AnnotationTriggerEvent.MOUSE_UP,
                        // Toggle visibility of the push button when checked.
                        new JavaScriptAction(""
                                + "var pushButtonField = doc.getField('pushbuttonfield');"
                                + "if (pushButtonField.display == display.hidden) {"
                                + "   pushButtonField.display = display.visible;"
                                + "} else {"
                                + "   pushButtonField.display = display.hidden;"
                                + "}"));

        document.getFormProvider().addFormElementToPage("checkboxfield", builder.build());
    }

    /** Decodes bitmap from application's assets. */
    @NonNull
    private Bitmap getBitmapFromAsset(@NonNull AssetManager assets, @NonNull String path) {
        Bitmap bitmap;
        try (InputStream is = assets.open(path)) {
            bitmap = BitmapFactory.decodeStream(is);
        } catch (final IOException e) {
            throw new RuntimeException("Error while trying to load bitmap from assets: " + path, e);
        }
        return bitmap;
    }
}
