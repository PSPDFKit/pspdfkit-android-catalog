/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.ui.model

import android.content.Context
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHostState
import androidx.datastore.preferences.core.Preferences
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.activity.ThumbnailBarMode
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.configuration.annotations.AnnotationReplyFeatures
import com.pspdfkit.configuration.page.PageFitMode
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.page.PageScrollMode
import com.pspdfkit.configuration.sharing.ShareFeatures
import com.pspdfkit.configuration.theming.ThemeMode
import java.util.Locale

sealed class SearchState {
    object Hidden : SearchState()
    class Visible(searchQuery: String, previousQuery: String = "") : SearchState() {
        val searchQuery = searchQuery.lowercase(Locale.getDefault())
        val previousQuery = previousQuery.lowercase(Locale.getDefault())
    }
}

fun SearchState.searchQueryOrBlank() = when (this) {
    SearchState.Hidden -> ""
    is SearchState.Visible -> searchQuery
}

fun SearchState.previousSearchQueryOrBlank() = when (this) {
    SearchState.Hidden -> ""
    is SearchState.Visible -> previousQuery
}

data class State(
    val currentPage: Page = Page.ExampleList,
    val examples: List<SdkExample.Section> = emptyList(),
    val expandedExampleSectionTitles: Set<String> = emptySet(),
    val preferenceSections: List<PreferencesSection> = emptyList(),
    val expandedPreferenceSectionTitles: Set<String> = emptySet(),
    val searchState: SearchState = SearchState.Hidden,
    val scaffoldState: ScaffoldState = ScaffoldState(
        drawerState = DrawerState(initialValue = DrawerValue.Closed),
        snackbarHostState = SnackbarHostState()
    ),
    val showedExampleLanguageHint: Boolean = false,
    val preferences: Map<Preferences.Key<*>, Any> = mapOf(
        Pair(PreferenceKeys.PageScrollDirection, PageScrollDirection.HORIZONTAL.name),
        Pair(PreferenceKeys.PageLayoutMode, PageLayoutMode.AUTO.name),
        Pair(PreferenceKeys.PageScrollContinuous, false),
        Pair(PreferenceKeys.FitPageToWidth, true),
        Pair(PreferenceKeys.FirstPageAsSingle, false),
        Pair(PreferenceKeys.ShowGapBetweenPages, false),
        Pair(PreferenceKeys.ImmersiveMode, true),
        Pair(PreferenceKeys.SystemUserInterfaceMode, UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_AUTOMATIC.name),
        Pair(PreferenceKeys.HideUiWhenCreatingAnnotations, true),
        Pair(PreferenceKeys.ShowSearchAction, true),
        Pair(PreferenceKeys.InlineSearch, true),
        Pair(PreferenceKeys.ThumbnailBarMode, ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING.name),
        Pair(PreferenceKeys.ShowThumbnailGridAction, true),
        Pair(PreferenceKeys.EnableDocumentOutline, true),
        Pair(PreferenceKeys.ShowAnnotationListAction, true),
        Pair(PreferenceKeys.ShowPageNumberOverlay, true),
        Pair(PreferenceKeys.ShowPageLabels, true),
        Pair(PreferenceKeys.InvertColors, false),
        Pair(PreferenceKeys.Grayscale, false),
        Pair(PreferenceKeys.StartPage, 0),
        Pair(PreferenceKeys.RestoreLastViewedPage, false),
        Pair(PreferenceKeys.EnableAnnotationEditing, true),
        Pair(PreferenceKeys.EnableAnnotationRotation, true),
        Pair(PreferenceKeys.AnnotationReplies, AnnotationReplyFeatures.ENABLED.name),
        Pair(PreferenceKeys.EnableTextSelection, true),
        Pair(PreferenceKeys.EnableFormEditing, true),
        Pair(PreferenceKeys.ShowShareAction, true),
        Pair(PreferenceKeys.ShowPrintAction, true),
        Pair(PreferenceKeys.ThemeMode, ThemeMode.DEFAULT.name),
        Pair(PreferenceKeys.EnableVolumeButtonsNavigation, false),
        Pair(PreferenceKeys.MultiThreadedRendering, true),
        Pair(PreferenceKeys.LeakCanaryEnabled, true)
    )
)

fun State.getPdfActivityConfigurationBuilder(context: Context): PdfActivityConfiguration.Builder {
    val scrollMode = if (scrollContinuously) PageScrollMode.CONTINUOUS else PageScrollMode.PER_PAGE
    val fitPageToWidth = if (fitPageToWidth) PageFitMode.FIT_TO_WIDTH else PageFitMode.FIT_TO_SCREEN
    val searchType = if (inlineSearch) PdfActivityConfiguration.SEARCH_INLINE else PdfActivityConfiguration.SEARCH_MODULAR

    val configuration = PdfActivityConfiguration.Builder(context)
        .scrollDirection(PageScrollDirection.valueOf(scrollDirection))
        .scrollMode(scrollMode)
        .fitMode(fitPageToWidth)
        .layoutMode(PageLayoutMode.valueOf(pageLayout))
        .theme(R.style.PSPDFCatalog_Theme)
        .themeDark(R.style.PSPDFCatalog_Theme_Dark)
        .themeMode(ThemeMode.valueOf(themeMode))
        .firstPageAlwaysSingle(displayFirstPageAsSingle)
        .showGapBetweenPages(showGapsBetweenPages)
        .restoreLastViewedPage(restoreLastViewedPage)
        .setUserInterfaceViewMode(UserInterfaceViewMode.valueOf(userInterfaceViewMode))
        .hideUserInterfaceWhenCreatingAnnotations(hideUiWhenCreatingAnnotations)
        .setSearchType(searchType)
        .setThumbnailBarMode(ThumbnailBarMode.valueOf(thumbnailBarMode))
        .annotationReplyFeatures(AnnotationReplyFeatures.valueOf(annotationReplies))
        .page(startPage)
        .useImmersiveMode(enableImmersiveMode)
        .setMultithreadedRenderingEnabled(enableMultithreadingRendering)

    if (showSearchAction) {
        configuration.enableSearch()
    } else {
        configuration.disableSearch()
    }

    if (showThumbnailGrid) {
        configuration.showThumbnailGrid()
    } else {
        configuration.hideThumbnailGrid()
    }

    if (enableDocumentOutline) {
        configuration.enableOutline()
    } else {
        configuration.disableOutline()
    }

    if (enableAnnotationList) {
        configuration.enableAnnotationList()
    } else {
        configuration.disableAnnotationList()
    }

    if (showPageNumberOverlay) {
        configuration.showPageNumberOverlay()
    } else {
        configuration.hidePageNumberOverlay()
    }

    if (showPageLabels) {
        configuration.showPageLabels()
    } else {
        configuration.hidePageLabels()
    }

    if (enableAnnotationEditing) {
        configuration.enableAnnotationEditing()
    } else {
        configuration.disableAnnotationEditing()
    }

    if (enableAnnotationRotation) {
        configuration.enableAnnotationRotation()
    } else {
        configuration.disableAnnotationRotation()
    }

    if (enableFormEditing) {
        configuration.enableFormEditing()
    } else {
        configuration.disableFormEditing()
    }

    if (showShareAction) {
        configuration.setEnabledShareFeatures(ShareFeatures.all())
    } else {
        configuration.setEnabledShareFeatures(ShareFeatures.none())
    }

    if (showPrintAction) {
        configuration.enablePrinting()
    } else {
        configuration.disablePrinting()
    }

    configuration.textSelectionEnabled(enableTextSelection)
    configuration.setVolumeButtonsNavigationEnabled(enableVolumeButtonNavigation)
    configuration.toGrayscale(grayscale)
    configuration.invertColors(invertPageColors || themeMode == ThemeMode.NIGHT.name)

    return configuration
}

val State.scrollDirection: String
    get() =
        preferences[PreferenceKeys.PageScrollDirection] as String

val State.pageLayout: String
    get() =
        preferences[PreferenceKeys.PageLayoutMode] as String

val State.scrollContinuously: Boolean
    get() =
        preferences[PreferenceKeys.PageScrollContinuous] as Boolean

val State.fitPageToWidth: Boolean
    get() =
        preferences[PreferenceKeys.FitPageToWidth] as Boolean

val State.restoreLastViewedPage: Boolean
    get() =
        preferences[PreferenceKeys.RestoreLastViewedPage] as Boolean

val State.showPageNumberOverlay: Boolean
    get() =
        preferences[PreferenceKeys.ShowPageNumberOverlay] as Boolean

val State.showPageLabels: Boolean
    get() =
        preferences[PreferenceKeys.ShowPageLabels] as Boolean

val State.userInterfaceViewMode: String
    get() =
        preferences[PreferenceKeys.SystemUserInterfaceMode] as String

val State.hideUiWhenCreatingAnnotations: Boolean
    get() =
        preferences[PreferenceKeys.HideUiWhenCreatingAnnotations] as Boolean

val State.themeMode: String
    get() =
        preferences[PreferenceKeys.ThemeMode] as String

val State.displayFirstPageAsSingle: Boolean
    get() =
        preferences[PreferenceKeys.FirstPageAsSingle] as Boolean

val State.showGapsBetweenPages: Boolean
    get() =
        preferences[PreferenceKeys.ShowGapBetweenPages] as Boolean

val State.showSearchAction: Boolean
    get() =
        preferences[PreferenceKeys.ShowSearchAction] as Boolean

val State.inlineSearch: Boolean
    get() =
        preferences[PreferenceKeys.InlineSearch] as Boolean

val State.thumbnailBarMode: String
    get() =
        preferences[PreferenceKeys.ThumbnailBarMode] as String

val State.showThumbnailGrid: Boolean
    get() =
        preferences[PreferenceKeys.ShowThumbnailGridAction] as Boolean

val State.enableDocumentOutline: Boolean
    get() =
        preferences[PreferenceKeys.EnableDocumentOutline] as Boolean

val State.enableAnnotationList: Boolean
    get() =
        preferences[PreferenceKeys.ShowAnnotationListAction] as Boolean

val State.enableAnnotationEditing: Boolean
    get() =
        preferences[PreferenceKeys.EnableAnnotationEditing] as Boolean

val State.enableAnnotationRotation: Boolean
    get() =
        preferences[PreferenceKeys.EnableAnnotationRotation] as Boolean

val State.annotationReplies: String
    get() =
        preferences[PreferenceKeys.AnnotationReplies] as String

val State.enableFormEditing: Boolean
    get() =
        preferences[PreferenceKeys.EnableFormEditing] as Boolean

val State.showShareAction: Boolean
    get() =
        preferences[PreferenceKeys.ShowShareAction] as Boolean

val State.showPrintAction: Boolean
    get() =
        preferences[PreferenceKeys.ShowPrintAction] as Boolean

val State.invertPageColors: Boolean
    get() =
        preferences[PreferenceKeys.InvertColors] as Boolean

val State.grayscale: Boolean
    get() =
        preferences[PreferenceKeys.Grayscale] as Boolean

val State.enableTextSelection: Boolean
    get() =
        preferences[PreferenceKeys.EnableTextSelection] as Boolean

val State.enableVolumeButtonNavigation: Boolean
    get() =
        preferences[PreferenceKeys.EnableVolumeButtonsNavigation] as Boolean

val State.enableImmersiveMode: Boolean
    get() =
        preferences[PreferenceKeys.ImmersiveMode] as Boolean

val State.startPage: Int
    get() =
        preferences[PreferenceKeys.StartPage] as Int

val State.enableMultithreadingRendering: Boolean
    get() =
        preferences[PreferenceKeys.MultiThreadedRendering] as Boolean

val State.enableLeakCanary: Boolean
    get() =
        preferences[PreferenceKeys.LeakCanaryEnabled] as Boolean
