/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * A list that takes items organized in sections and lays them out inside a LazyColumn.
 * This is a custom implementation of a list of expandable sections (headers).
 * Note that this Composable is light on logic, and your header Composable should include the expanding logic.
 *
 * @param itemsInSections A sequence of items organized inside a sequence of headers (sections).
 * @param expandedSectionsState A list of which sections are expanded.
 * @param modifier The Composable modifier.
 * @param topHeaderLayout A Composable displayed as a header to the whole list. Is not sticky by default.
 * @param sectionHeaderLayout A Composable used for section headers.
 * @param itemLayout A Composable used for each item.
 * @param sectionFooterLayout A Composable displayed as a footer at the end of each section.
 * @param bottomFooterLayout A Composable displayed as a footer to the whole list.
 * @param areSectionHeadersSticky If section headers should stick to the top of the screen or be scrollable.
 * @param searchQuery String used to filter this list.
 */
@Composable
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun <Item, Section : List<Item>, Key> ExpandableList(
    itemsInSections: List<Section>,
    expandedSectionsState: Set<Key>,
    sectionKey: (Section) -> Key,
    modifier: Modifier = Modifier,
    topHeaderLayout: @Composable (() -> Unit)? = null,
    sectionHeaderLayout: @Composable ((Int, Boolean, Int) -> Unit)? = null,
    itemLayout: @Composable ((Any?) -> Unit)? = null,
    sectionFooterLayout: @Composable (() -> Unit)? = null,
    bottomFooterLayout: @Composable (() -> Unit)? = null,
    areSectionHeadersSticky: Boolean = true,
    searchQuery: String
) {
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            keyboardController?.hide()
        }
    }

    // To know which headers are currently active and stickied, we remember a vector where each index holds the current number of
    // children for each section. This is needed because all of the indexes update when a header expands and adds its children to the list.
    // Using a Vector as it's not guaranteed that the list of sections will be properly initialized by the first time this is Composed.
    val openSectionsWithChildrenAmount = remember { MutableVector<Int>() }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {
        item {
            topHeaderLayout?.invoke()
        }

        itemsInSections.forEachIndexed { sectionIndex, section ->

            val sectionIsOpen = searchQuery.isNotBlank() || expandedSectionsState.contains(sectionKey(section))

            if (areSectionHeadersSticky) {
                stickyHeader {
                    // As it might not have been initialized at first, we ensure enough capacity here.
                    // So whenever the headers are recomposed, we update their open/closed status.
                    openSectionsWithChildrenAmount.ensureCapacity(itemsInSections.size)

                    if (sectionIsOpen) {
                        openSectionsWithChildrenAmount[sectionIndex] = section.size
                    } else {
                        openSectionsWithChildrenAmount[sectionIndex] = 0
                    }

                    var currentHeaderIndex = lazyListState.layoutInfo.visibleItemsInfo[0].index - 1

                    for (i in 0 until sectionIndex) {
                        currentHeaderIndex -= openSectionsWithChildrenAmount[i]
                    }

                    sectionHeaderLayout?.invoke(
                        sectionIndex,
                        sectionIsOpen,
                        currentHeaderIndex
                    )
                }
            } else {
                item {
                    sectionHeaderLayout?.invoke(
                        sectionIndex,
                        sectionIsOpen,
                        // This info is only relevant if the headers are sticky.
                        -1
                    )
                }
            }

            if (sectionIsOpen) {
                items(section) { item ->
                    itemLayout?.invoke(item)
                }

                item {
                    sectionFooterLayout?.invoke()
                }
            }
        }

        item {
            bottomFooterLayout?.invoke()

            // We always want 16.dp at the end to make the last item easier to click on.
            SpacerTransparentFooter()
        }
    }
}
