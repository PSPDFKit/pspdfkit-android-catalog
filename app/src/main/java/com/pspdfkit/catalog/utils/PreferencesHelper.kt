/*
 *   Copyright © 2021-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.ui.model.ButtonPreference
import com.pspdfkit.catalog.ui.model.CheckboxPreference
import com.pspdfkit.catalog.ui.model.IntegerPreference
import com.pspdfkit.catalog.ui.model.PreferenceKeys
import com.pspdfkit.catalog.ui.model.PreferencesSection
import com.pspdfkit.catalog.ui.model.RadioPreference
import com.pspdfkit.catalog.utils.CatalogPreferences.annotationReplies
import com.pspdfkit.catalog.utils.CatalogPreferences.clearAppData
import com.pspdfkit.catalog.utils.CatalogPreferences.clearCache
import com.pspdfkit.catalog.utils.CatalogPreferences.displayFirstPageAsSingle
import com.pspdfkit.catalog.utils.CatalogPreferences.enableAnnotationEditing
import com.pspdfkit.catalog.utils.CatalogPreferences.enableAnnotationList
import com.pspdfkit.catalog.utils.CatalogPreferences.enableAnnotationRotation
import com.pspdfkit.catalog.utils.CatalogPreferences.enableDocumentOutline
import com.pspdfkit.catalog.utils.CatalogPreferences.enableFormEditing
import com.pspdfkit.catalog.utils.CatalogPreferences.enableImmersiveMode
import com.pspdfkit.catalog.utils.CatalogPreferences.enableLeakCanary
import com.pspdfkit.catalog.utils.CatalogPreferences.enableMultithreadingRendering
import com.pspdfkit.catalog.utils.CatalogPreferences.enableTextSelection
import com.pspdfkit.catalog.utils.CatalogPreferences.enableVolumeButtonNavigation
import com.pspdfkit.catalog.utils.CatalogPreferences.fitPageToWidth
import com.pspdfkit.catalog.utils.CatalogPreferences.grayscale
import com.pspdfkit.catalog.utils.CatalogPreferences.hideUiWhenCreatingAnnotations
import com.pspdfkit.catalog.utils.CatalogPreferences.inlineSearch
import com.pspdfkit.catalog.utils.CatalogPreferences.invertPageColors
import com.pspdfkit.catalog.utils.CatalogPreferences.pageLayout
import com.pspdfkit.catalog.utils.CatalogPreferences.restoreLastViewedPage
import com.pspdfkit.catalog.utils.CatalogPreferences.scrollContinuously
import com.pspdfkit.catalog.utils.CatalogPreferences.scrollDirection
import com.pspdfkit.catalog.utils.CatalogPreferences.showGapsBetweenPages
import com.pspdfkit.catalog.utils.CatalogPreferences.showPageLabels
import com.pspdfkit.catalog.utils.CatalogPreferences.showPageNumberOverlay
import com.pspdfkit.catalog.utils.CatalogPreferences.showPrintAction
import com.pspdfkit.catalog.utils.CatalogPreferences.showSearchAction
import com.pspdfkit.catalog.utils.CatalogPreferences.showShareAction
import com.pspdfkit.catalog.utils.CatalogPreferences.showThumbnailGrid
import com.pspdfkit.catalog.utils.CatalogPreferences.startPage
import com.pspdfkit.catalog.utils.CatalogPreferences.themeMode
import com.pspdfkit.catalog.utils.CatalogPreferences.thumbnailBarMode
import com.pspdfkit.catalog.utils.CatalogPreferences.userInterfaceViewMode
import com.pspdfkit.configuration.activity.ThumbnailBarMode
import com.pspdfkit.configuration.activity.UserInterfaceViewMode
import com.pspdfkit.configuration.annotations.AnnotationReplyFeatures
import com.pspdfkit.configuration.page.PageLayoutMode
import com.pspdfkit.configuration.page.PageScrollDirection
import com.pspdfkit.configuration.theming.ThemeMode

/** Catalog-wide helper for accessing the shared preferences. */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object CatalogPreferences {
    fun scrollDirection(context: Context) = RadioPreference(
        title = context.getString(R.string.radio_preference_scroll_direction),
        key = PreferenceKeys.PageScrollDirection,
        possibleValuesResource = R.array.page_scroll_directions
    )

    fun pageLayout(context: Context) = RadioPreference(
        title = context.getString(R.string.radio_preference_page_layout),
        key = PreferenceKeys.PageLayoutMode,
        possibleValuesResource = R.array.page_layouts
    )

    fun scrollContinuously(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_scroll_continuously),
        key = PreferenceKeys.PageScrollContinuous
    )

    fun fitPageToWidth(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_fit_page_to_width),
        key = PreferenceKeys.FitPageToWidth
    )

    fun restoreLastViewedPage(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_restore_last_viewed_page),
        key = PreferenceKeys.RestoreLastViewedPage
    )

    fun showPageNumberOverlay(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_show_page_number_overlay),
        key = PreferenceKeys.ShowPageNumberOverlay
    )

    fun showPageLabels(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_show_page_labels),
        key = PreferenceKeys.ShowPageLabels
    )

    fun userInterfaceViewMode(context: Context) = RadioPreference(
        title = context.getString(R.string.radio_preference_user_interface_view_mode),
        key = PreferenceKeys.SystemUserInterfaceMode,
        possibleValuesResource = R.array.user_interface_view_modes
    )

    fun hideUiWhenCreatingAnnotations(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_hide_ui_when_creating_annotations),
        key = PreferenceKeys.HideUiWhenCreatingAnnotations
    )

    fun themeMode(context: Context) = RadioPreference(
        title = context.getString(R.string.radio_preference_theme_mode),
        key = PreferenceKeys.ThemeMode,
        possibleValuesResource = R.array.theme_mode
    )

    fun displayFirstPageAsSingle(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_display_first_page_as_single),
        key = PreferenceKeys.FirstPageAsSingle
    )

    fun showGapsBetweenPages(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_show_gaps_between_pages),
        key = PreferenceKeys.ShowGapBetweenPages
    )

    fun showSearchAction(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_show_search_action),
        key = PreferenceKeys.ShowSearchAction
    )

    fun inlineSearch(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_inline_search),
        key = PreferenceKeys.InlineSearch
    )

    fun thumbnailBarMode(context: Context) = RadioPreference(
        title = context.getString(R.string.radio_preference_thumbnail_bar_mode),
        key = PreferenceKeys.ThumbnailBarMode,
        possibleValuesResource = R.array.thumbnail_bar_modes
    )

    fun showThumbnailGrid(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_show_thumbnail_grid),
        key = PreferenceKeys.ShowThumbnailGridAction
    )

    fun enableDocumentOutline(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_enable_document_outline),
        key = PreferenceKeys.EnableDocumentOutline
    )

    fun enableAnnotationList(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_enable_annotation_list),
        key = PreferenceKeys.ShowAnnotationListAction
    )

    fun enableAnnotationEditing(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_enable_annotation_editing),
        key = PreferenceKeys.EnableAnnotationEditing
    )

    fun enableAnnotationRotation(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_enable_annotation_rotation),
        key = PreferenceKeys.EnableAnnotationRotation
    )

    fun annotationReplies(context: Context) = RadioPreference(
        title = context.getString(R.string.radio_preference_annotation_replies),
        key = PreferenceKeys.AnnotationReplies,
        possibleValuesResource = R.array.annotation_reply_modes
    )

    fun enableFormEditing(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_enable_annotation_editing),
        key = PreferenceKeys.EnableFormEditing
    )

    fun showShareAction(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_show_share_action),
        key = PreferenceKeys.ShowShareAction
    )

    fun showPrintAction(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_show_print_action),
        key = PreferenceKeys.ShowPrintAction
    )

    fun invertPageColors(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_invert_page_colors),
        key = PreferenceKeys.InvertColors
    )

    fun grayscale(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_grayscale),
        key = PreferenceKeys.Grayscale
    )

    fun enableTextSelection(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_enable_text_selection),
        key = PreferenceKeys.EnableTextSelection
    )

    fun enableVolumeButtonNavigation(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_enable_volume_buttons_navigation),
        key = PreferenceKeys.EnableVolumeButtonsNavigation
    )

    fun enableImmersiveMode(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_immersive_mode),
        key = PreferenceKeys.ImmersiveMode
    )

    fun startPage(context: Context) = IntegerPreference(
        title = context.getString(R.string.integer_preference_start_page),
        key = PreferenceKeys.StartPage,
        description = context.getString(R.string.integer_preference_start_page_description)
    )

    fun enableMultithreadingRendering(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_multithreaded_rendering),
        key = PreferenceKeys.MultiThreadedRendering
    )

    fun enableLeakCanary(context: Context) = CheckboxPreference(
        title = context.getString(R.string.checkbox_preference_enable_leakcanary),
        key = PreferenceKeys.LeakCanaryEnabled
    )

    fun clearCache(context: Context) = ButtonPreference(
        title = context.getString(R.string.button_preference_clear_cache),
        key = PreferenceKeys.ClearCache
    )

    fun clearAppData(context: Context) = ButtonPreference(
        title = context.getString(R.string.button_preference_clear_app_data),
        key = PreferenceKeys.ClearAppData,
        description = context.getString(R.string.button_preference_clear_app_data_description)
    )
}

/**
 * Used for the main preferences layout.
 */
fun preferenceSections(context: Context): List<PreferencesSection> =
    listOf(
        elements = arrayOf(
            PreferencesSection(
                title = context.getString(R.string.preference_section_layout),
                preferences = arrayOf(
                    scrollDirection(context),
                    pageLayout(context),
                    scrollContinuously(context),
                    fitPageToWidth(context),
                    restoreLastViewedPage(context),
                    showPageNumberOverlay(context),
                    showPageLabels(context),
                    userInterfaceViewMode(context),
                    hideUiWhenCreatingAnnotations(context),
                    themeMode(context)
                )
            ),

            PreferencesSection(
                title = context.getString(R.string.preference_section_double_page),
                preferences = arrayOf(
                    displayFirstPageAsSingle(context),
                    showGapsBetweenPages(context)
                )
            ),

            PreferencesSection(
                title = context.getString(R.string.preference_section_search),
                preferences = arrayOf(
                    showSearchAction(context),
                    inlineSearch(context)
                )
            ),

            PreferencesSection(
                title = context.getString(R.string.preference_section_thumbnails_outline),
                preferences = arrayOf(
                    thumbnailBarMode(context),
                    showThumbnailGrid(context),
                    enableDocumentOutline(context),
                    enableAnnotationList(context)
                )
            ),

            PreferencesSection(
                title = context.getString(R.string.preference_section_annotations),
                preferences = arrayOf(
                    enableAnnotationEditing(context),
                    enableAnnotationRotation(context),
                    annotationReplies(context)
                )
            ),

            PreferencesSection(
                title = context.getString(R.string.preference_section_forms),
                preferences = arrayOf(
                    enableFormEditing(context)
                )
            ),

            PreferencesSection(
                title = context.getString(R.string.preference_section_actions),
                preferences = arrayOf(
                    showShareAction(context),
                    showPrintAction(context)
                )
            ),

            PreferencesSection(
                title = context.getString(R.string.preference_section_customization),
                preferences = arrayOf(
                    invertPageColors(context),
                    grayscale(context),
                    enableTextSelection(context),
                    enableVolumeButtonNavigation(context)
                )
            ),

            PreferencesSection(
                title = context.getString(R.string.preference_section_activity),
                preferences = arrayOf(
                    enableImmersiveMode(context)
                )
            ),

            PreferencesSection(
                title = context.getString(R.string.preference_section_other),
                preferences = arrayOf(
                    startPage(context),
                    enableMultithreadingRendering(context),
                    enableLeakCanary(context),
                    clearCache(context),
                    clearAppData(context)
                )
            )
        )
    )

fun getRadioOptionStringFromEnumName(enumName: String, context: Context): String =
    when (enumName) {
        PageScrollDirection.HORIZONTAL.name -> context.getString(R.string.page_scroll_direction_horizontal)
        PageScrollDirection.VERTICAL.name -> context.getString(R.string.page_scroll_direction_vertical)
        PageLayoutMode.AUTO.name -> context.getString(R.string.page_layout_auto)
        PageLayoutMode.SINGLE.name -> context.getString(R.string.page_layout_single)
        PageLayoutMode.DOUBLE.name -> context.getString(R.string.page_layout_double)
        UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_AUTOMATIC.name -> context.getString(R.string.user_interface_view_mode_automatic)
        UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_AUTOMATIC_BORDER_PAGES.name -> context.getString(R.string.user_interface_view_mode_automatic_border_pages)
        UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_HIDDEN.name -> context.getString(R.string.user_interface_view_mode_hidden)
        UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_MANUAL.name -> context.getString(R.string.user_interface_view_mode_manual)
        UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_VISIBLE.name -> context.getString(R.string.user_interface_view_mode_visible)
        ThumbnailBarMode.THUMBNAIL_BAR_MODE_PINNED.name -> context.getString(R.string.thumbnail_bar_mode_pinned)
        ThumbnailBarMode.THUMBNAIL_BAR_MODE_SCROLLABLE.name -> context.getString(R.string.thumbnail_bar_mode_scrollable)
        ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE.name -> context.getString(R.string.thumbnail_bar_mode_none)
        ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING.name -> context.getString(R.string.thumbnail_bar_mode_floating)
        AnnotationReplyFeatures.DISABLED.name -> context.getString(R.string.annotation_reply_disabled)
        AnnotationReplyFeatures.ENABLED.name -> context.getString(R.string.annotation_reply_enabled)
        AnnotationReplyFeatures.READ_ONLY.name -> context.getString(R.string.annotation_reply_read_only)
        ThemeMode.DEFAULT.name -> context.getString(R.string.theme_mode_default)
        ThemeMode.NIGHT.name -> context.getString(R.string.theme_mode_night)
        else -> throw Exception("Failed to get Preference String from EnumName in PreferencesHelper.")
    }

fun getPageScrollDirectionFromString(value: String, context: Context): PageScrollDirection =
    when (value) {
        context.getString(R.string.page_scroll_direction_horizontal) -> PageScrollDirection.HORIZONTAL
        context.getString(R.string.page_scroll_direction_vertical) -> PageScrollDirection.VERTICAL
        else -> throw Exception("Failed to get PageScrollDirection from string in PreferencesHelper.")
    }

fun getPageLayoutFromString(value: String, context: Context): PageLayoutMode =
    when (value) {
        context.getString(R.string.page_layout_auto) -> PageLayoutMode.AUTO
        context.getString(R.string.page_layout_single) -> PageLayoutMode.SINGLE
        context.getString(R.string.page_layout_double) -> PageLayoutMode.DOUBLE
        else -> throw Exception("Failed to get PageLayoutMode from string in PreferencesHelper.")
    }

fun getUserInterfaceModeFromString(value: String, context: Context): UserInterfaceViewMode =
    when (value) {
        context.getString(R.string.user_interface_view_mode_automatic) -> UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_AUTOMATIC
        context.getString(R.string.user_interface_view_mode_automatic_border_pages) -> UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_AUTOMATIC_BORDER_PAGES
        context.getString(R.string.user_interface_view_mode_hidden) -> UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_HIDDEN
        context.getString(R.string.user_interface_view_mode_manual) -> UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_MANUAL
        context.getString(R.string.user_interface_view_mode_visible) -> UserInterfaceViewMode.USER_INTERFACE_VIEW_MODE_VISIBLE
        else -> throw Exception("Failed to get UserInterfaceViewMode from string in PreferencesHelper.")
    }

fun getThumbnailBarModeFromString(value: String, context: Context): ThumbnailBarMode =
    when (value) {
        context.getString(R.string.thumbnail_bar_mode_pinned) -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_PINNED
        context.getString(R.string.thumbnail_bar_mode_scrollable) -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_SCROLLABLE
        context.getString(R.string.thumbnail_bar_mode_none) -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_NONE
        context.getString(R.string.thumbnail_bar_mode_floating) -> ThumbnailBarMode.THUMBNAIL_BAR_MODE_FLOATING
        else -> throw Exception("Failed to get ThumbnailBarMode from string in PreferencesHelper.")
    }

fun getAnnotationReplyFeaturesFromString(value: String, context: Context): AnnotationReplyFeatures =
    when (value) {
        context.getString(R.string.annotation_reply_disabled) -> AnnotationReplyFeatures.DISABLED
        context.getString(R.string.annotation_reply_enabled) -> AnnotationReplyFeatures.ENABLED
        context.getString(R.string.annotation_reply_read_only) -> AnnotationReplyFeatures.READ_ONLY
        else -> throw Exception("Failed to get AnnotationReplyFeatures from string in PreferencesHelper.")
    }

fun getThemeModeFromString(value: String, context: Context): ThemeMode =
    when (value) {
        context.getString(R.string.theme_mode_default) -> ThemeMode.DEFAULT
        context.getString(R.string.theme_mode_night) -> ThemeMode.NIGHT
        else -> throw Exception("Failed to get ThemeMode from string in PreferencesHelper.")
    }
