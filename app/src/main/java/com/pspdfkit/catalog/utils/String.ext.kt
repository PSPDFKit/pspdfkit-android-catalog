/*
 *   Copyright © 2023-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils

import java.util.Locale

/**
 *  Checks if this string contains all characters in [stringToMatch], in order.
 **/
fun String.fuzzlyMatches(stringToMatch: String): Boolean {
    if (length < stringToMatch.length) {
        return false
    }

    var currentIndex = 0
    val stringToMatchLowercase = stringToMatch.lowercase(Locale.getDefault())
    for (charToMatch in stringToMatchLowercase) {
        var charMatched = false
        while (!charMatched && currentIndex <= lastIndex) {
            val currChar = this[currentIndex].lowercaseChar()
            if (currChar == charToMatch) {
                charMatched = true
            }

            currentIndex++
        }

        if (!charMatched) {
            return false
        }
    }

    return true
}
