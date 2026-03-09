package com.pspdfkit.catalog.examples.java

import android.content.Context
import androidx.annotation.IntRange
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.drawable.PdfDrawable
import com.pspdfkit.ui.drawable.PdfDrawableProvider

// Fixme: Convert ScreenReaderExample to Kotlin and use it directly.
internal class ScreenReaderDrawableProvider(private val screenReader: ScreenReaderExample.ScreenReader) : PdfDrawableProvider() {
    override suspend fun getDrawablesForPage(
        context: Context,
        document: PdfDocument,
        @IntRange(from = 0) pageIndex: Int,
    ): List<PdfDrawable>? = screenReader.getDrawablesForPage(pageIndex)
}
