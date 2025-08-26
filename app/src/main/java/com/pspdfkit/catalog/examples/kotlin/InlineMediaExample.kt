/*
 *   Copyright © 2020-2025 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import com.pspdfkit.catalog.R

/**
 * This example showcases the inline multimedia extension of Nutrient. The multimedia-capable document
 * as well as the media files themselves are located within the asset folder.
 */
class InlineMediaExample(context: Context) : AssetExample(context, R.string.inlineMultimediaExampleTitle, R.string.inlineMultimediaExampleDescription) {
    override val assetPath: String
        get() = "media/multimedia_android_v2.pdf"
}
