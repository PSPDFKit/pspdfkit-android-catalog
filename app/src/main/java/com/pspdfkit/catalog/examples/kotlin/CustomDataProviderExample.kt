/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RawRes
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.providers.DataProvider
import com.pspdfkit.document.providers.InputStreamDataProvider
import com.pspdfkit.ui.PdfActivity
import com.pspdfkit.ui.PdfActivityIntentBuilder
import java.io.IOException
import java.io.InputStream

/**
 * This example shows how to create a custom data provider that reads a document from the `raw` resources
 * of the app. Furthermore, it implements [Parcelable] to allow using the data provider with  [PdfActivity].
 */
class CustomDataProviderExample(context: Context) : SdkExample(context, R.string.customDataProviderExampleTitle, R.string.customDataProviderExampleDescription) {

    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        // Create an instance of the custom data provider. See the implementation details below.
        val dataProvider: DataProvider = RawResourceDataProvider(R.raw.guide)

        // Start the activity using our custom data provider.
        val intent = PdfActivityIntentBuilder.fromDataProvider(context, dataProvider)
            .configuration(configuration.build())
            .build()
        context.startActivity(intent)
    }
}

/**
 * Custom data provider for loading a PDF document from the app's raw resources. Since
 * [Resources.openRawResource] returns an `InputStream`, this provider derives from
 * `InputStreamDataProvider` which handles loading data from a stream object.
 *
 * @param resId The id of the PDF document inside the resources (stored within the `res/raw` folder of the application).
 */
class RawResourceDataProvider(@RawRes private val resId: Int) : InputStreamDataProvider(), Parcelable {

    /**
     * The size of the raw resource. This will be cached after the first call to [.getSize].
     */
    private var size = DataProvider.FILE_SIZE_UNKNOWN.toLong()

    /**
     * We return the InputStream for the referenced raw resource. Since InputStreamDataProvider may call this
     * method multiple times we have to make sure that it always returns a fresh input stream object.
     */
    @Throws(IOException::class)
    override fun openInputStream(): InputStream {
        return getContext().resources.openRawResource(resId)
    }

    /**
     * This method returns the size of our resource. Android only gives us an [InputStream] for
     * accessing the resources. Thus we have to reopen the input stream if current stream
     * position is not at the start.
     */
    override fun getSize(): Long {
        // If the file size is already known, return it immediately.
        if (size != DataProvider.FILE_SIZE_UNKNOWN.toLong()) {
            return size
        }

        val inputStreamSize = try {
            // Since we can only get size of the available data in the input stream we need to
            // reopen it here if the stream position is not 0.
            if (inputStreamPosition != 0L) {
                reopenInputStream()
            }
            openInputStream().available()
        } catch (e: Exception) {
            DataProvider.FILE_SIZE_UNKNOWN
        }

        size = inputStreamSize.toLong()
        return size
    }

    override fun getUid(): String {
        return getContext().resources.getResourceName(resId)
    }

    override fun getTitle(): String {
        // If you know the file or document name upfront, you can return it here. Otherwise return null,
        // which will instruct PSPDFKit to use the title stored within the document (if any).
        return "PSPDFKit Quickstart Guide"
    }

    // The code below is standard Android parcelation code. If you don't know how to implement the Parcelable
    // interface start looking at {@link http://developer.android.com/reference/android/os/Parcelable.html}.

    /**
     * Default parcelable implementation. The object is always parceled the same way. Thus, we return 0.
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * We simply write the id of the PDF resource to the parcel.
     */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(resId)
    }

    /**
     * Constructor required for unparcelation, takes a `input` Parcel and reads the raw resource id from it.
     */
    internal constructor(input: Parcel) : this(input.readInt())

    companion object CREATOR : Parcelable.Creator<RawResourceDataProvider> {
        override fun createFromParcel(parcel: Parcel): RawResourceDataProvider {
            return RawResourceDataProvider(parcel)
        }

        override fun newArray(size: Int): Array<RawResourceDataProvider?> {
            return arrayOfNulls(size)
        }
    }
}
