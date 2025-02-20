/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils

import com.pspdfkit.catalog.ui.model.SearchState
import com.pspdfkit.catalog.ui.model.previousSearchQueryOrBlank
import com.pspdfkit.catalog.ui.model.searchQueryOrBlank

// Matchable interface used to make the examples and the preferences of the catalog searchable.
interface Matchable {
    val stringsToMatch: List<String>
    fun matches(stringToMatch: String): Boolean
}

interface GroupMatchable<T : Matchable> : Matchable {
    val childMatchables: List<T>
    fun getMatchingChildren(stringToMatch: String): List<T> = childMatchables.filter { it.matches(stringToMatch) }
}

interface FuzzlyMatchable : Matchable {
    override fun matches(stringToMatch: String): Boolean = stringsToMatch.any { it.fuzzlyMatches(stringToMatch) }
}

/**
 * Filters a list of [GroupMatchable]s by the query string of a [SearchState].
 * @param searchState The current search state.
 * @param previousResult Provide here the result of the previous search to optimize filtering.
 * @param createFilteredGroupMatchable A function that creates a new group matchable with the filtered children.
 */
fun <T : Matchable, G : GroupMatchable<T>> List<G>.filterBySearchState(searchState: SearchState, previousResult: List<G>, createFilteredGroupMatchable: (parent: G, filteredChildren: List<T>) -> G): List<G> {
    val stringToMatch = searchState.searchQueryOrBlank()
    return if (stringToMatch.isBlank()) {
        this
    } else {
        val previousQueryString = searchState.previousSearchQueryOrBlank()

        // if we just extend the previous search query, we can filter the previous result
        val groupsToSearch = if (stringToMatch.startsWith(previousQueryString)) {
            previousResult
        } else {
            this
        }

        groupsToSearch.mapNotNull { group ->

            // If the group name matches, everything inside of it should match
            if (group.matches(stringToMatch)) return@mapNotNull group

            // otherwise filter the group's children
            val matchingChildren = group.childMatchables.filter { child -> child.matches(stringToMatch) }

            // If there are no examples in a section, we remove said section
            if (matchingChildren.isEmpty()) {
                null
            } else {
                createFilteredGroupMatchable(group, matchingChildren)
            }
        }
    }
}
