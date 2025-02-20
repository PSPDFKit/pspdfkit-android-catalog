/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.pspdfkit.annotations.actions.UriAction
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.processor.NewPage
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.forms.CheckBoxFormConfiguration
import com.pspdfkit.forms.ComboBoxFormConfiguration
import com.pspdfkit.forms.FormOption
import com.pspdfkit.forms.ListBoxFormConfiguration
import com.pspdfkit.forms.PushButtonFormConfiguration
import com.pspdfkit.forms.RadioButtonFormConfiguration
import com.pspdfkit.forms.SignatureFormConfiguration
import com.pspdfkit.forms.TextFormConfiguration
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Kotlin example. Showcases how to create forms programmatically.
 */
class FormCreationExample(context: Context) :
    SdkExample(context, R.string.formCreationExampleTitle, R.string.formCreationExampleDescription) {

    private var documentProcessingDisposable: Disposable? = null

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Turn off saving, so we have the clean original document every time the example is launched.
        configuration.autosaveEnabled(false)

        // Enable form editing UI.
        configuration.enableFormEditing()

        // Create an A4 page document from scratch.
        val newPage = NewPage.emptyPage(NewPage.PAGE_SIZE_A4).build()
        val task = PdfProcessorTask.newPage(newPage)

        val outputFile: File
        try {
            outputFile = File(getCatalogCacheDirectory(context), "Blank.pdf").canonicalFile
        } catch (exception: IOException) {
            throw IllegalStateException("Couldn't create Blank.pdf file.", exception)
        }

        documentProcessingDisposable = PdfProcessor.processDocumentAsync(task, outputFile)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    val intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(outputFile))
                        .configuration(configuration.build())
                        .activityClass(FormCreationActivity::class)
                        .build()

                    context.startActivity(intent)
                },
                { throwable ->
                    Log.e(TAG, "Error while trying to create PDF document.", throwable)
                }
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        documentProcessingDisposable?.dispose()
        documentProcessingDisposable = null
    }

    @Throws(IOException::class)
    private fun getCatalogCacheDirectory(context: Context): File {
        val directory = File(context.cacheDir, PSPDFKIT_DIRECTORY_NAME)
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw IOException("Failed to create Catalog cache directory.")
            }
        }
        return directory
    }

    companion object {
        private const val TAG = "FormCreationExample"
        private const val PSPDFKIT_DIRECTORY_NAME = "catalog-pspdfkit"
    }
}

class FormCreationActivity : PdfActivity() {

    private var getFormElementsDisposable: Disposable? = null

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        // Retrieve existing form elements and create form fields only when there are no forms.
        getFormElementsDisposable = document.formProvider.formElementsAsync.subscribe { formElements ->
            if (formElements.isNotEmpty()) return@subscribe
            createForms()
        }
    }

    /**
     * Creates several forms and attach them to the document.
     */
    private fun createForms() {
        createTextFormField()
        createCheckBoxFormField()
        createRadioButtonFormField()
        createPushButtonFormField()
        createSignatureFormField()
        createComboBoxFormField()
        createListBoxFormField()
    }

    /**
     * Creates and attaches to the document a text form field.
     */
    private fun createTextFormField() {
        val rectF = RectF(30f, 750f, 200f, 720f)
        val textFormConfiguration = TextFormConfiguration.Builder(0, rectF)
            .setText("Example text")
            .build()
        document?.formProvider?.addFormElementToPage("textfield-1", textFormConfiguration)
    }

    /**
     * Creates and attaches to the document a checkbox form field with two checkbox form elements.
     */
    private fun createCheckBoxFormField() {
        val rectFCheckBoxFormConfiguration1 = RectF(30f, 650f, 60f, 620f)
        val checkBoxFormConfiguration1 = CheckBoxFormConfiguration.Builder(0, rectFCheckBoxFormConfiguration1)
            .select()
            .build()

        val rectFCheckBoxFormConfiguration2 = RectF(30f, 600f, 60f, 570f)
        val checkBoxFormConfiguration2 = CheckBoxFormConfiguration.Builder(0, rectFCheckBoxFormConfiguration2)
            .deselect()
            .build()

        val checkBoxFormConfigurationList = listOf(checkBoxFormConfiguration1, checkBoxFormConfiguration2)
        document?.formProvider?.addFormElementsToPage("checkboxfield-1", checkBoxFormConfigurationList)
    }

    /**
     * Creates and attaches to the document a radio button form field with two radio button form elements.
     */
    private fun createRadioButtonFormField() {
        val rectFRadioButtonFormConfiguration1 = RectF(30f, 500f, 60f, 470f)
        val radioButtonFormConfiguration1 = RadioButtonFormConfiguration.Builder(0, rectFRadioButtonFormConfiguration1)
            .select()
            .build()

        val rectFRadioButtonFormConfiguration2 = RectF(30f, 450f, 60f, 420f)
        val radioButtonFormConfiguration2 = RadioButtonFormConfiguration.Builder(0, rectFRadioButtonFormConfiguration2)
            .deselect()
            .build()

        val radioButtonFormConfigurationList = listOf(radioButtonFormConfiguration1, radioButtonFormConfiguration2)
        document?.formProvider?.addFormElementsToPage("radiobuttonfield-1", radioButtonFormConfigurationList)
    }

    /**
     * Creates and attaches to the document a push button form field.
     */
    private fun createPushButtonFormField() {
        val rectFPushButtonFormConfiguration = RectF(30f, 350f, 120f, 260f)

        val bitmapFromAsset = getBitmapFromAsset(assets, "images/android.png") ?: return

        val pushButtonFormConfiguration = PushButtonFormConfiguration.Builder(
            0,
            rectFPushButtonFormConfiguration,
            bitmapFromAsset
        )
            .setAction(UriAction("https://developer.android.com/index.html"))
            .build()

        document?.formProvider?.addFormElementToPage("pushbuttonfield-1", pushButtonFormConfiguration)
    }

    /**
     * Creates and attaches to the document a signature form field.
     */
    private fun createSignatureFormField() {
        val rectFSignatureFormConfiguration = RectF(30f, 190f, 200f, 160f)

        val signatureFormConfiguration = SignatureFormConfiguration.Builder(0, rectFSignatureFormConfiguration)
            .build()

        document?.formProvider?.addFormElementToPage("signaturefield-1", signatureFormConfiguration)
    }

    /**
     * Creates and attaches to the document a combo box form field.
     */
    private fun createComboBoxFormField() {
        val rectFComboBoxFormConfiguration = RectF(350f, 650f, 520f, 620f)
        val comboBoxFormConfiguration = ComboBoxFormConfiguration.Builder(0, rectFComboBoxFormConfiguration)
            .setFormOptions(
                listOf(
                    FormOption("L1", "42"),
                    FormOption("L2", "43")
                )
            )
            .setCustomText("Custom text")
            .build()

        document?.formProvider?.addFormElementToPage("comboboxfield-1", comboBoxFormConfiguration)
    }

    /**
     * Creates and attaches to the document a list box form field.
     */
    private fun createListBoxFormField() {
        val rectFListBoxFormConfiguration = RectF(350f, 500f, 520f, 420f)
        val listBoxFormConfiguration = ListBoxFormConfiguration.Builder(0, rectFListBoxFormConfiguration)
            .setFormOptions(
                listOf(
                    FormOption("L1", "42"),
                    FormOption("L2", "43"),
                    FormOption("L3", "44"),
                    FormOption("L4", "45")
                )
            )
            .setMultiSelectionEnabled(true)
            .setSelectedIndexes(listOf(1, 2))
            .build()

        document?.formProvider?.addFormElementToPage("listboxfield-1", listBoxFormConfiguration)
    }

    @Suppress("SameParameterValue")
    private fun getBitmapFromAsset(assets: AssetManager, path: String): Bitmap? {
        var inputStream: InputStream? = null
        var bitmap: Bitmap?
        try {
            inputStream = assets.open(path)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            bitmap = null
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (ignored: IOException) {
                }
            }
        }
        return bitmap
    }

    override fun onDestroy() {
        super.onDestroy()
        getFormElementsDisposable?.dispose()
    }
}
