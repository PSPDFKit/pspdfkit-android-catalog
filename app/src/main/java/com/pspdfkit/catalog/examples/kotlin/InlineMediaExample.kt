/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import com.pspdfkit.catalog.R
import com.pspdfkit.configuration.activity.PdfActivityConfiguration

/**
 * This example showcases the inline multimedia extension of PSPDFKit. The multimedia-capable document
 * as well as the media files themselves are located within the asset folder.
 */
class InlineMediaExample(context: Context) : AssetExample(context, R.string.inlineMultimediaExampleTitle, R.string.inlineMultimediaExampleDescription) {

    override val assetPath: String
        get() = "media/multimedia_android_v2.pdf"

    override fun prepareConfiguration(configuration: PdfActivityConfiguration.Builder) {
        // Make sure that video playback is enabled in configuration.
        //
        // By default, video playback is disabled unless the device is deemed secure. Meaning the
        // device is running at least Android Marshmallow (API 23+) with security patch dating
        // Feb 1st, 2016 or newer. This security patch fixes a critical security vulnerability that
        // could enable remote code execution on an affected device through multiple methods such
        // as email, web browsing, and MMS when processing media files.
        configuration.videoPlaybackEnabled(true)
    }
}
