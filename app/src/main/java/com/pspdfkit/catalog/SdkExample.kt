/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog

import android.content.Context
import androidx.annotation.StringRes
import com.pspdfkit.catalog.utils.FuzzlyMatchable
import com.pspdfkit.catalog.utils.GroupMatchable
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.signatures.DigitalSignatureType
import java.util.Locale

/**
 * Abstract example class which provides [.launchExample] as a generic way of launching a catalog app example.
 */
abstract class SdkExample(
    /** Short title of the example.  */
    val title: String,
    /** Full description of the example.  */
    val description: String
) : FuzzlyMatchable {

    /**
     * Convenience constructor. Examples can pass their `title` and `description` here.
     */
    constructor(context: Context, @StringRes title: Int, @StringRes description: Int) : this(context.getString(title), context.getString(description))

    /** Enum with all supported example languages.  */
    enum class ExampleLanguage {
        JAVA,
        KOTLIN
    }

    /** DigitalSignatureType to for changing signature type in digital signature examples.  */
    var digitalSignatureType: DigitalSignatureType = DigitalSignatureType.CADES

    /** A section is a named list of examples grouped together (e.g. "Multimedia examples").  */
    class Section(val name: String, val iconId: Int, examples: Collection<SdkExample>) : ArrayList<SdkExample>(examples), FuzzlyMatchable, GroupMatchable<SdkExample> {
        constructor(name: String, icon: Int, vararg examples: SdkExample) : this(name, icon, examples.toList())
        override val stringsToMatch by lazy { listOf(name.lowercase(Locale.getDefault())) }
        override val childMatchables: List<SdkExample> get() = this // this as the ArrayList we're inheriting from
    }

    override val stringsToMatch by lazy {
        listOf(
            title.lowercase(Locale.getDefault()),
            description.lowercase(Locale.getDefault()),
            exampleName.lowercase(Locale.getDefault())
        )
    }

    /**
     * Examples have to implement this method with their example code. The given `configuration` contains all default settings for the example, and can be further tweaked if
     * necessary.
     *
     * @param context       Context for launching examples.
     * @param configuration Default configuration as created by the preferences.
     */
    abstract fun launchExample(
        context: Context,
        configuration: PdfActivityConfiguration.Builder
    )

    private val exampleName: String
        /**
         * Returns the example simple class name as given in the source code.
         *
         * @return the example simple class name.
         */
        get() = javaClass.simpleName

    val exampleLanguage: ExampleLanguage
        /** Returns the language of this example.  */
        get() = if (isKotlin) ExampleLanguage.KOTLIN else ExampleLanguage.JAVA

    private val isKotlin: Boolean
        /** Returns `true` when this example is written in Kotlin.  */
        get() = this::class.java.isAnnotationPresent(Metadata::class.java)

    /**
     * Called when the owning activity gets destroyed. Example is supposed to clean any required
     * resources.
     */
    open fun onDestroy() {}

    companion object {
        // Documents used in our examples.
        const val QUICK_START_GUIDE: String = "Quickstart.pdf"
        const val ANNOTATIONS_EXAMPLE: String = "Annotations.pdf"

        // Image used in our examples.
        const val ANDROID_IMAGE_PNG: String = "images/android.png"
    }
}
