/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.processor.NewPage
import com.pspdfkit.document.processor.PageImage
import com.pspdfkit.document.processor.PdfProcessor
import com.pspdfkit.document.processor.PdfProcessorTask
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.utils.Size
import com.pspdfkit.utils.getSupportParcelableExtra

class PdfFromImageExample(context: Context) : SdkExample(context, R.string.pdfFromImageExampleTitle, R.string.pdfFromImageExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        val intent = Intent(context, PdfFromImageActivity::class.java)
        intent.putExtra(PdfFromImageActivity.EXTRA_CONFIGURATION, configuration.build())
        context.startActivity(intent)
    }
}

class PdfFromImageActivity : Activity() {

    private lateinit var configuration: PdfActivityConfiguration

    private var waitingForResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract the configuration for displaying the viewer activity.
        configuration = intent.getSupportParcelableExtra(EXTRA_CONFIGURATION, PdfActivityConfiguration::class.java)
            ?: throw ExceptionInInitializerError(
                PdfFromImageActivity::class.java.simpleName +
                    " was started without a PdfActivityConfiguration."
            )

        // Prevent the example from requesting multiple documents at the same time.
        if (!waitingForResult) {
            waitingForResult = true
            startActivityForResult(getImagePickerIntent(), REQUEST_IMAGE)
        }
    }

    private fun getImagePickerIntent(): Intent? {
        // Creates an intent that will open a file picker with the filter set to only open images.
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        return if (intent.resolveActivity(packageManager) == null) null else Intent.createChooser(intent, "")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Once the image was picked we are done with this activity, close it.
        finish()

        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && data?.data != null) {
            // Grab the path to the selected image.
            val imageUri = data.data ?: return

            // Create a PdfProcessorTask to create the new PDF.
            val task = createPdfFromImageTask(imageUri)

            // Obtain a path to put the resulting file.
            // For simplicity we always put it in our application directory.
            val outputPath = filesDir.resolve("image.pdf")

            // Process the document.
            PdfProcessor.processDocument(task, outputPath)

            // And finally show it.
            PdfActivity.showDocument(this, Uri.fromFile(outputPath), configuration)
        }
    }

    /**
     * This creates a [PdfProcessorTask] which will create a single page document using the supplied image as the page background.
     */
    private fun createPdfFromImageTask(imageUri: Uri): PdfProcessorTask {
        // First obtain the size of the image.
        val options = BitmapFactory.Options().apply {
            // By setting this we won't actually load the image but only figure out the size.
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri), null, options)
        val imageHeight: Int = options.outHeight
        val imageWidth: Int = options.outWidth

        // We take A4 as a baseline, and alter the page's aspect ratio based on the given bitmap.
        val pageSize: Size = if (imageWidth <= imageHeight) {
            Size(NewPage.PAGE_SIZE_A4.width, imageHeight * (NewPage.PAGE_SIZE_A4.width / imageWidth))
        } else {
            Size(NewPage.PAGE_SIZE_A4.height, imageHeight * NewPage.PAGE_SIZE_A4.height / imageWidth)
        }

        // Now that we know the desired size we can create a PdfProcessorTask which will create a document containing a single page.
        return PdfProcessorTask.newPage(
            NewPage.emptyPage(pageSize)
                // We initialize our new page using the passed in image URI and calculated page size.
                .withPageItem(PageImage(this, imageUri, RectF(0f, pageSize.height, pageSize.width, 0f)))
                .build()
        )
    }

    companion object {
        internal const val EXTRA_CONFIGURATION = "PSPDFKit.PdfFromImageActivity.configuration"

        private const val REQUEST_IMAGE = 1
    }
}
