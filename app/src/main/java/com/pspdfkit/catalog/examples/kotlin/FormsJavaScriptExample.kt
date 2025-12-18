/*
 *   Copyright © 2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.annotation.UiThread
import com.pspdfkit.annotations.actions.AnnotationTriggerEvent
import com.pspdfkit.annotations.actions.JavaScriptAction
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.processor.NewPage
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.forms.CheckBoxFormConfiguration
import com.pspdfkit.forms.PushButtonFormConfiguration
import com.pspdfkit.forms.TextFormConfiguration
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.IOException
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach

/** This example showcases forms JavaScript functionality. */
class FormsJavaScriptExample(context: Context) : SdkExample(
    context.getString(R.string.javaScriptFormsExampleTitle),
    context.getString(R.string.javaScriptFormsExampleDescription)
) {

    @SuppressLint("SetJavaScriptEnabled", "CheckResult")
    override fun launchExample(
        context: Context,
        configuration: PdfActivityConfiguration.Builder
    ) {
        configuration
            // JavaScript is enabled by default. It can be disabled in configuration.
            .setJavaScriptEnabled(true)
            // Turn off saving, so we have the clean original document every time the example is launched.
            .autosaveEnabled(false)

        // Create an empty document from scratch with multiple pages.
        val blankPage = NewPage.emptyPage(NewPage.PAGE_SIZE_A4).build()
        val task = PdfProcessorTask.empty()

        // Create multiple blank pages - we'll create example form fields on these pages.
        repeat(4) { index ->
            task.addNewPage(blankPage, index)
        }

        val outputFile = try {
            File(getCatalogCacheDirectory(context), "FormsJavaScriptExample.pdf").canonicalFile
        } catch (exception: IOException) {
            throw IllegalStateException("Couldn't create FormsJavaScriptExample.pdf file.", exception)
        }

        PdfProcessor.processDocumentAsync(task, outputFile)
            .ignoreElements()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(outputFile))
                        .configuration(configuration.build())
                        .activityClass(FormsJavaScriptActivity::class.java)
                        .build()

                    context.startActivity(intent)
                },
                { throwable ->
                    Log.e(TAG, "Error while trying to create PDF document.", throwable)
                }
            )
    }

    companion object {
        private const val PSPDFKIT_DIRECTORY_NAME = "catalog-pspdfkit"

        @Throws(IOException::class)
        private fun getCatalogCacheDirectory(ctx: Context): File {
            val dir = File(ctx.cacheDir, PSPDFKIT_DIRECTORY_NAME)
            if (!dir.exists() && !dir.mkdirs()) {
                throw IOException("Failed to create Catalog cache directory.")
            }
            return dir
        }
    }
}

/**
 * This activity shows how to create custom forms with JavaScript actions attached.
 *
 * Note: For more information about JavaScript API refer to our guides or Adobe's
 * JavaScript for Acrobat API Reference.
 */
class FormsJavaScriptActivity : PdfActivity() {

    @SuppressLint("CheckResult")
    @UiThread
    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        document.formProvider.formElementsAsync.subscribe { formElements, _ ->
            if (!formElements.isNullOrEmpty()) return@subscribe
            createSimpleCalculationExample()
            createManualCalculationExample()
            createFormattingExample()
            createPushButtonWithImageImportAction()
            createCheckboxWithHideAction()
        }
    }

    private fun addTextField(
        name: String,
        pageIndex: Int,
        rect: RectF,
        text: String,
        actions: Map<AnnotationTriggerEvent, String> = emptyMap(),
        readOnly: Boolean = false
    ) {
        document?.formProvider?.addFormElementToPage(
            name,
            TextFormConfiguration.Builder(pageIndex, rect)
                .apply {
                    setText(text)
                    actions.forEach { (event, script) ->
                        setAdditionalAction(event, JavaScriptAction(script))
                    }
                    if (readOnly) setReadOnly(true)
                }
                .build()
        )
    }

    private fun createSimpleCalculationExample() {
        addTextField(
            "sum-operand1",
            0,
            RectF(30f, 750f, 200f, 720f),
            "3.0",
            mapOf(
                AnnotationTriggerEvent.FORM_CHANGED to "AFNumber_Keystroke(2,0,0,0,'',true);",
                AnnotationTriggerEvent.FIELD_FORMAT to "AFNumber_Format(2,0,0,0,'',true);"
            )
        )

        addTextField(
            "sum-operand2",
            0,
            RectF(30f, 690f, 200f, 660f),
            "4",
            mapOf(
                AnnotationTriggerEvent.FORM_CHANGED to "AFNumber_Keystroke(0,1,0,0,'',true);",
                AnnotationTriggerEvent.FIELD_FORMAT to "AFNumber_Format(0,1,0,0,'',true);"
            )
        )

        addTextField(
            "sum-result",
            0,
            RectF(30f, 630f, 200f, 600f),
            "",
            mapOf(
                AnnotationTriggerEvent.FORM_CALCULATE to
                    "AFSimple_Calculate('SUM', new Array('sum-operand1','sum-operand2'));",
                AnnotationTriggerEvent.FIELD_FORMAT to "AFNumber_Format(2,1,0,0,'',true);"
            ),
            readOnly = true
        )
    }

    private fun createManualCalculationExample() {
        addTextField("concat-operand1", 1, RectF(30f, 750f, 200f, 720f), "First")
        addTextField("concat-operand2", 1, RectF(30f, 690f, 200f, 660f), "Second")
        addTextField(
            "concat-result",
            1,
            RectF(30f, 630f, 200f, 600f),
            "",
            mapOf(
                AnnotationTriggerEvent.FORM_CALCULATE to """
                    var operand1 = doc.getField('concat-operand1');
                    var operand2 = doc.getField('concat-operand2');
                    var result = doc.getField('concat-result');
                    result.value = operand1.value + ' ' + operand2.value;
                """.trimIndent()
            ),
            readOnly = true
        )
    }

    private fun createFormattingExample() {
        addTextField(
            "Number format",
            2,
            RectF(30f, 750f, 200f, 720f),
            "3.2293",
            mapOf(
                AnnotationTriggerEvent.FORM_CHANGED to "AFNumber_Keystroke(2,0,0,0,'',true);",
                AnnotationTriggerEvent.FIELD_FORMAT to "AFNumber_Format(2,0,0,0,'',true);"
            )
        )

        addTextField(
            "Currency format",
            2,
            RectF(30f, 690f, 200f, 660f),
            "33,99",
            mapOf(
                AnnotationTriggerEvent.FORM_CHANGED to "AFNumber_Keystroke(2,3,0,0,'€',true);",
                AnnotationTriggerEvent.FIELD_FORMAT to "AFNumber_Format(2,3,0,0,'€',true);"
            )
        )

        addTextField(
            "Date format",
            2,
            RectF(30f, 630f, 200f, 600f),
            "02/07/2018",
            mapOf(
                AnnotationTriggerEvent.FORM_CHANGED to "AFDate_KeystrokeEx('mm/dd/yyyy');",
                AnnotationTriggerEvent.FIELD_FORMAT to "AFDate_FormatEx('mm/dd/yyyy');"
            )
        )

        addTextField(
            "Time format",
            2,
            RectF(30f, 570f, 200f, 540f),
            "12:00:20",
            mapOf(
                AnnotationTriggerEvent.FORM_CHANGED to "AFTime_Keystroke('HH:MM::ss');",
                AnnotationTriggerEvent.FIELD_FORMAT to "AFTime_Format('HH:MM::ss');"
            )
        )
    }

    private fun createPushButtonWithImageImportAction() {
        document?.formProvider?.addFormElementToPage(
            "pushbuttonfield",
            PushButtonFormConfiguration.Builder(
                3,
                RectF(30f, 750f, 120f, 660f),
                getBitmapFromAsset(assets, "images/android.png")
            )
                .setAction(JavaScriptAction("var f=this.getField('pushbuttonfield'); f.buttonImportIcon();"))
                .build()
        )
    }

    private fun createCheckboxWithHideAction() {
        document?.formProvider?.addFormElementToPage(
            "checkboxfield",
            CheckBoxFormConfiguration.Builder(3, RectF(150f, 690f, 180f, 660f))
                .deselect()
                .setAdditionalAction(
                    AnnotationTriggerEvent.MOUSE_UP,
                    JavaScriptAction(
                        """
                        var pushButtonField = doc.getField('pushbuttonfield');
                        pushButtonField.display = (pushButtonField.display == display.hidden) ? display.visible : display.hidden;
                        """.trimIndent()
                    )
                )
                .build()
        )
    }

    private fun getBitmapFromAsset(assets: AssetManager, path: String): Bitmap =
        assets.open(path).use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalStateException("Failed to decode bitmap from assets: $path")
}
