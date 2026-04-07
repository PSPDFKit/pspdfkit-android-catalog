/*
 *   Copyright © 2017-2026 PSPDFKit GmbH. All rights reserved.
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
 *
 * Examples must use the constructor with Context and @StringRes parameters to ensure
 * proper resource ID extraction for documentation generation.
 */
abstract class SdkExample(
    context: Context,
    /** String resource ID for the short title of the example. */
    @StringRes titleRes: Int,
    /** String resource ID for the full description of the example. */
    @StringRes descriptionRes: Int,
) : FuzzlyMatchable {
    /** Short title of the example. */
    val title: String = context.getString(titleRes)

    /** Full description of the example. */
    val description: String = context.getString(descriptionRes)

    /** Enum with all supported example languages.  */
    enum class ExampleLanguage {
        JAVA,
        KOTLIN,
    }

    enum class LaunchRequirement {
        DIGITAL_SIGNATURE_TYPE,
        AI_ASSISTANT_SERVER,
    }

    /** DigitalSignatureType to for changing signature type in digital signature examples.  */
    var digitalSignatureType: DigitalSignatureType = DigitalSignatureType.CADES

    /**
     * Extra setup the catalog should perform before launching this example.
     */
    open val launchRequirements: Set<LaunchRequirement> = emptySet()

    /** A section is a named list of examples grouped together (e.g. "Multimedia examples").  */
    class Section(val name: String, val iconId: Int, examples: Collection<SdkExample>) :
        ArrayList<SdkExample>(examples),
        FuzzlyMatchable,
        GroupMatchable<SdkExample> {
        constructor(name: String, icon: Int, vararg examples: SdkExample) : this(name, icon, examples.toList())

        override val stringsToMatch by lazy { listOf(name.lowercase(Locale.getDefault())) }
        override val childMatchables: List<SdkExample> get() = this // this as the ArrayList we're inheriting from
    }

    override val stringsToMatch by lazy {
        listOf(
            title.lowercase(Locale.getDefault()),
            exampleName.lowercase(Locale.getDefault()),
        )
    }

    /**
     * Examples have to implement this method with their example code. The given `configuration` contains all default settings for the example, and can be further tweaked if
     * necessary.
     *
     * @param context       Context for launching examples.
     * @param configuration Default configuration as created by the preferences.
     */
    abstract fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder)

    /**
     * Returns the example simple class name as given in the source code.
     */
    private val exampleName: String
        get() = javaClass.simpleName

    /** Returns the language of this example. */
    val exampleLanguage: ExampleLanguage
        get() = if (isKotlin) ExampleLanguage.KOTLIN else ExampleLanguage.JAVA

    /** Returns `true` when this example is written in Kotlin. */
    private val isKotlin: Boolean
        get() = this::class.java.isAnnotationPresent(Metadata::class.java)

    /**
     * Called when the owning activity gets destroyed. Example is supposed to clean any required
     * resources.
     */
    open fun onDestroy() {}

    companion object {
        const val TAG = "Example"

        // Documents used in our examples.
        const val WELCOME_DOC: String = "Nutrient welcome.pdf"
        const val ANNOTATIONS_EXAMPLE: String = "Annotations.pdf"

        // Image used in our examples.
        const val ANDROID_IMAGE_PNG: String = "images/android.png"
    }
}
