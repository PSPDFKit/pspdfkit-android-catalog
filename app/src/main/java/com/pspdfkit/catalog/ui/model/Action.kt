/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui.model

import androidx.datastore.preferences.core.Preferences

sealed class Action {

    // Examples
    data class ToggleExampleSection(val sectionTitle: String) : Action()

    // Preferences
    data class PreferenceChanged<T : Any>(val key: Preferences.Key<out T>, val value: T) : Action()
    data class TogglePreferenceSection(val sectionTitle: String) : Action()
    data class PreferenceButtonTapped(val key: Preferences.Key<String>) : Action()

    // Topbar
    object SettingsButtonTapped : Action()
    object SearchButtonTapped : Action()
    object CancelSearchButtonTapped : Action()
    object UpButtonTapped : Action()
    data class SearchQueryChanged(val searchQuery: String) : Action()
}

typealias Dispatcher = (Action) -> Unit
