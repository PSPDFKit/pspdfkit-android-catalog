/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui.model

import androidx.datastore.preferences.core.Preferences
import java.util.ArrayList
import java.util.Collections

sealed class Preference<T>(
    val title: String,
    val key: Preferences.Key<T>,
    val description: String = ""
)

class RadioPreference(
    title: String,
    key: Preferences.Key<String>,
    val possibleValuesResource: Int,
    val isInline: Boolean = false
) : Preference<String>(title, key)

class CheckboxPreference(
    title: String,
    key: Preferences.Key<Boolean>
) : Preference<Boolean>(title, key)

class ButtonPreference(
    title: String,
    key: Preferences.Key<String>,
    description: String = ""
) : Preference<String>(title, key, description)

class IntegerPreference(
    title: String,
    key: Preferences.Key<Int>,
    description: String = ""
) : Preference<Int>(title, key, description)

class PreferencesSection(
    val title: String,
    vararg preferences: Preference<*>
) : ArrayList<Preference<*>>() {
    init {
        Collections.addAll(this, *preferences)
    }
}
