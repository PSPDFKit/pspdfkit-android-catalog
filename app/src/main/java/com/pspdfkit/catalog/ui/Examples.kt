/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pspdfkit.catalog.BuildConfig
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.ui.model.Action
import com.pspdfkit.catalog.ui.model.Dispatcher
import com.pspdfkit.catalog.ui.model.State
import com.pspdfkit.catalog.ui.model.getPdfActivityConfigurationBuilder
import com.pspdfkit.catalog.ui.model.searchQueryOrBlank
import com.pspdfkit.catalog.ui.theming.AlphaDefs
import com.pspdfkit.catalog.ui.theming.Animations
import com.pspdfkit.catalog.ui.theming.Dimens
import com.pspdfkit.catalog.utils.filterBySearchState
import com.pspdfkit.catalog.utils.firstCharacterUpperCase
import com.pspdfkit.catalog.utils.isDigitalSignatureExample

@Composable
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
fun Examples(state: State, dispatcher: Dispatcher) {
    val context = LocalContext.current

    var filteredExamplesInSections by remember { mutableStateOf(listOf<SdkExample.Section>()) }
    var dialogVisibility by remember { mutableStateOf(false) }
    var selectedClass by remember { mutableStateOf<SdkExample?>(null) }

    // I'm using a LaunchedEffect here to filter the list in a background thread
    LaunchedEffect(state.examples, state.searchState) {
        filteredExamplesInSections = state.examples.filterBySearchState(state.searchState, filteredExamplesInSections) { section, examples ->
            SdkExample.Section(section.name, section.iconId, examples)
        }
    }

    CustomAlertDialog(dialogVisibility, { dialogVisibility = false }) {
        selectedClass?.apply { digitalSignatureType = it }?.launchExample(context, state.getPdfActivityConfigurationBuilder(context))
    }

    ExpandableList(
        searchQuery = state.searchState.searchQueryOrBlank(),
        itemsInSections = filteredExamplesInSections,
        expandedSectionsState = state.expandedExampleSectionTitles,
        sectionKey = { it.name },
        topHeaderLayout = {
            Box(
                modifier = Modifier.zIndex(2f)
            ) {
                SpacerExamplesHeader()
            }
        },
        sectionHeaderLayout = { sectionIndex, sectionIsExpanded, firstVisibleHeaderIndex ->

            val section = filteredExamplesInSections.elementAtOrNull(sectionIndex) ?: return@ExpandableList

            // If the first visible index is higher than the current sectionIndex, it means it's the active and probably stickied header.
            // Stickied headers don't count as visible.
            val isCurrentHeader = firstVisibleHeaderIndex > sectionIndex

            val isPreviousSectionExpanded =
                state.expandedExampleSectionTitles.contains(filteredExamplesInSections.getOrNull(sectionIndex - 1)?.name)

            ExampleHeader(
                sectionTitle = section.name,
                iconId = section.iconId,
                isExpanded = sectionIsExpanded,
                isPreviousSectionExpanded = isPreviousSectionExpanded,
                isCurrentHeader = isCurrentHeader,
                onExpandButtonClicked = {
                    val action = Action.ToggleExampleSection(section.name)
                    dispatcher(action)
                }
            )
        },
        itemLayout = { example ->
            val psExample = example as SdkExample
            Box(
                modifier = Modifier.clickable {
                    psExample.isDigitalSignatureExample(context, {
                        selectedClass = psExample
                        dialogVisibility = true
                    }) {
                        psExample.launchExample(
                            context,
                            state.getPdfActivityConfigurationBuilder(context)
                        )
                    }
                }
            ) {
                ExampleListItem(
                    title = psExample.title,
                    description = psExample.description,
                    exampleLanguage = psExample.exampleLanguage
                )
            }
        }
    )
}

@Composable
fun ExampleHeader(
    sectionTitle: String,
    iconId: Int?,
    isExpanded: Boolean,
    isPreviousSectionExpanded: Boolean,
    isCurrentHeader: Boolean,
    onExpandButtonClicked: () -> Unit
) {
    if (iconId == null) {
        // Should never be the case for the Compose UI, but the viewer Catalog needs it to be nullable.
        throw NullPointerException("IconId is null. Check if icons are in order in the ExamplesHelper.")
    }

    // Animations for switching colors to purple and alpha to 100% when a section is expanded.
    val expandColorAnimation by Animations.sectionExpandingColor(expanded = isExpanded)
    val expandTextAlphaAnimation by Animations.sectionExpandingAlpha(expanded = isExpanded)
    val expandIconAlphaAnimation by Animations.sectionExpandingIconAlpha(expanded = isExpanded)

    // We want the header to have a some extra padding and a gray line spacer if it isn't the current header.
    val shouldHaveSpacerAbove = isPreviousSectionExpanded && !isCurrentHeader

    Surface(
        elevation = if (isExpanded) 2.dp else 0.dp,
        modifier = Modifier
            .height(if (shouldHaveSpacerAbove) Dimens.listHeaderHeight + 8.dp else Dimens.listHeaderHeight)
            .zIndex(if (isExpanded) 0f else 1f)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .clickable { onExpandButtonClicked() }
    ) {
        Column {
            if (shouldHaveSpacerAbove) {
                Row(
                    modifier = Modifier.padding(bottom = 7.dp)
                ) {
                    SpacerLine()
                }
            }

            Row(
                verticalAlignment = CenterVertically,
                modifier = Modifier
                    .padding(Dimens.examplesListHeaderHorizontalPadding)
                    .fillMaxHeight()
            ) {
                Icon(
                    modifier = Modifier
                        .defaultMinSize(Dimens.iconSize)
                        .alpha(expandIconAlphaAnimation),

                    painter = painterResource(id = iconId),
                    contentDescription = stringResource(R.string.section_icon_content_desc, sectionTitle),
                    tint = expandColorAnimation
                )

                Text(
                    modifier = Modifier
                        .padding(start = 32.dp)
                        .alpha(expandTextAlphaAnimation),

                    text = sectionTitle,
                    style = MaterialTheme.typography.h1,
                    color = expandColorAnimation
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    ExpandSectionButton(
                        sectionTitle,
                        Dimens.iconSize,
                        isExpanded,
                        onExpandButtonClicked
                    )
                }
            }
        }
    }
}

@Composable
fun ExampleListItem(
    title: String,
    description: String,
    exampleLanguage: SdkExample.ExampleLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // This is needed so the shadow under the header doesn't show when it's not stickied.
            .background(color = MaterialTheme.colors.background)
            .padding(Dimens.examplesListItemPadding),

        verticalAlignment = CenterVertically,
        horizontalArrangement = SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(weight = 0.75f)
        ) {
            Text(
                modifier = Modifier.alpha(AlphaDefs.title),
                text = title,
                style = MaterialTheme.typography.h2
            )
            Text(
                modifier = Modifier
                    .padding(end = 32.dp)
                    .alpha(AlphaDefs.half),
                text = description,
                style = MaterialTheme.typography.subtitle2
            )
        }

        // Badge to show if it was written in java or kotlin
        // This doesn't work in release mode. Just hide it if so.
        if (BuildConfig.DEBUG) {
            Surface(
                modifier = Modifier
                    .weight(weight = 0.25f, fill = false)
                    .align(CenterVertically)
                    .size(width = 45.dp, height = 18.dp),

                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colors.secondary
            ) {
                val languageName = exampleLanguage.name.firstCharacterUpperCase()

                Text(
                    modifier = Modifier.alpha(AlphaDefs.half),
                    text = languageName,
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
