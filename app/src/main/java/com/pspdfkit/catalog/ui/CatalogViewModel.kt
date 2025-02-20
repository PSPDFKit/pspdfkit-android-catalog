/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.pspdfkit.PSPDFKit
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.ui.model.Action
import com.pspdfkit.catalog.ui.model.Page
import com.pspdfkit.catalog.ui.model.PreferenceKeys
import com.pspdfkit.catalog.ui.model.SearchState
import com.pspdfkit.catalog.ui.model.State
import com.pspdfkit.catalog.utils.getSectionsWithExamples
import com.pspdfkit.catalog.utils.preferenceSections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Centralized ViewModel for the whole Catalog application.
 * The State within it serves as our single source of truth for anything state related.
 * This ViewModel should be the only way to access and mutate that state.
 */
class CatalogViewModel(
    application: Application,
    private val dataStore: DataStore<Preferences>
) : AndroidViewModel(application) {

    // Note that the MutableStateFlow is private. We only expose an immutable version for Composables to observe.
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val examples = getSectionsWithExamples(application.applicationContext)
            val preferencesSections = preferenceSections(application.applicationContext)
            val storedPreferences = dataStore.data.first()
            val preferences = state.value.preferences.map { (preferenceKey, defaultValue) ->
                val entry = storedPreferences[preferenceKey] ?: defaultValue
                preferenceKey to entry
            }.toMap()

            mutableState.mutate {
                copy(
                    preferences = preferences,
                    examples = examples,
                    preferenceSections = preferencesSections
                )
            }
        }
    }

    fun dispatch(action: Action) {
        when (action) {
            Action.CancelSearchButtonTapped -> onCancelSearchButtonTapped()
            is Action.SearchQueryChanged -> onSearchQueryChanged(action.searchQuery)
            is Action.PreferenceButtonTapped -> onPreferenceButtonTapped(action.key)
            is Action.PreferenceChanged<*> -> viewModelScope.launch {
                @Suppress("UNCHECKED_CAST")
                val key = action.key as Preferences.Key<Any>
                val value = action.value
                dataStore.edit { preferences -> preferences[action.key] = value }
                mutableState.mutate {
                    copy(
                        preferences = preferences.toMutableMap().apply { this[key] = value }
                    )
                }
            }
            Action.SearchButtonTapped -> onSearchButtonTapped()
            Action.SettingsButtonTapped -> onSettingsButtonTapped()
            Action.UpButtonTapped -> onUpButtonTapped()
            is Action.ToggleExampleSection -> toggleExampleSection(action.sectionTitle)
            is Action.TogglePreferenceSection -> togglePreferenceSection(action.sectionTitle)
        }
    }

    private fun togglePreferenceSection(sectionTitle: String) {
        mutableState.mutate {
            if (expandedPreferenceSectionTitles.contains(sectionTitle)) {
                copy(expandedPreferenceSectionTitles = expandedPreferenceSectionTitles - sectionTitle)
            } else {
                copy(expandedPreferenceSectionTitles = expandedPreferenceSectionTitles + sectionTitle)
            }
        }
    }

    private fun toggleExampleSection(sectionTitle: String) {
        mutableState.mutate {
            if (expandedExampleSectionTitles.contains(sectionTitle)) {
                copy(expandedExampleSectionTitles = expandedExampleSectionTitles - sectionTitle)
            } else {
                copy(expandedExampleSectionTitles = expandedExampleSectionTitles + sectionTitle)
            }
        }
    }

    fun backPressed(): Boolean {
        val callSystemBack = with(state.value) { currentPage == Page.ExampleList && searchState == SearchState.Hidden }
        mutableState.mutate {
            if (searchState != SearchState.Hidden) {
                copy(searchState = SearchState.Hidden)
            } else {
                copy(currentPage = Page.ExampleList)
            }
        }

        return callSystemBack
    }

    private fun onPreferenceButtonTapped(key: Preferences.Key<String>) {
        val context = getApplication<Application>()

        // We currently only have two preference buttons that execute specific actions.
        when (key) {
            PreferenceKeys.ClearAppData -> {
                (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            }

            PreferenceKeys.ClearCache -> {
                PSPDFKit.clearCaches()
                Toast
                    .makeText(context, context.getString(R.string.toast_cache_cleared), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun onSettingsButtonTapped() {
        mutableState.mutate {
            copy(currentPage = Page.Settings)
        }
    }

    private fun onSearchButtonTapped() {
        mutableState.mutate {
            copy(searchState = SearchState.Visible(searchQuery = ""))
        }
    }

    private fun onCancelSearchButtonTapped() {
        mutableState.mutate {
            copy(searchState = SearchState.Hidden)
        }
    }

    private fun onUpButtonTapped() {
        mutableState.mutate {
            copy(currentPage = Page.ExampleList)
        }
    }

    private fun onSearchQueryChanged(searchQuery: String) {
        mutableState.mutate {
            val oldQuery = (searchState as? SearchState.Visible)?.searchQuery ?: ""
            copy(
                searchState = SearchState.Visible(searchQuery, oldQuery)
            )
        }
    }

    class Factory(private val application: Application, private val dataStore: DataStore<Preferences>) :
        ViewModelProvider.AndroidViewModelFactory(application) {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return CatalogViewModel(application, dataStore) as T
        }
    }

    private fun <T> MutableStateFlow<T>.mutate(mutateFunction: T.() -> T) {
        value = value.mutateFunction()
    }
}
