/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui.theming

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.configuration.theming.ThemeMode

/**
 * Applies the XML theme a [PdfActivityConfiguration] carries — the catalog theme, for a
 * catalog-prepared configuration — resolving it the way [com.pspdfkit.ui.PdfActivity] resolves its
 * own: `darkTheme` when the configuration's [ThemeMode] is [ThemeMode.NIGHT], `theme` otherwise,
 * each falling back to the matching stock SDK theme when left at
 * [PdfActivityConfiguration.NO_THEME].
 *
 * Examples that host the SDK in a plain activity (a `DocumentView`, or a bare
 * [com.pspdfkit.ui.PdfFragment]) get no PdfActivity theme handling, and the SDK UI they host —
 * the main toolbar, the inspector, the fragment chrome — resolves its colors from the *activity's*
 * XML theme. Without this they would render with stock SDK styling instead of the catalog's.
 *
 * Unlike PdfActivity, this always sets a theme: PdfActivity skips the stock fallback when the
 * activity already declares an `android:theme` in the manifest, whereas this overrides one. The
 * catalog's configurations always carry an explicit theme, so the fallback never applies here —
 * but a host that themes its activity through the manifest should set the theme itself instead of
 * calling this.
 *
 * Call it before `super.onCreate`, so the theme is in place before any content inflates.
 */
fun Activity.applyConfigurationTheme(configuration: PdfActivityConfiguration?) {
    setTheme(
        if (configuration.isNightTheme) {
            configuration?.darkTheme?.takeIf { it != PdfActivityConfiguration.NO_THEME }
                ?: com.pspdfkit.R.style.PSPDFKit_Theme_Dark
        } else {
            configuration?.theme?.takeIf { it != PdfActivityConfiguration.NO_THEME }
                ?: com.pspdfkit.R.style.PSPDFKit_Theme_Default
        },
    )
}

/**
 * Whether this configuration resolves to the dark theme.
 *
 * This is the *only* place these examples decide light vs dark. The XML theme
 * ([applyConfigurationTheme]), the system bar icons ([enableConfigurationEdgeToEdge]) and the
 * Compose chrome (`CatalogTheme(darkTheme = …)`) must all answer it the same way: reading the
 * system's night mode for some of them and the configuration for the others leaves the window in
 * a mixed state — dark-mode chrome over a light document, and system bar icons tinted for a
 * background that isn't there.
 */
val PdfActivityConfiguration?.isNightTheme: Boolean
    get() = this?.configuration?.themeMode == ThemeMode.NIGHT

/**
 * Goes edge-to-edge with the system bar icons tinted for [configuration]'s theme rather than for
 * the system's night mode.
 *
 * Plain `enableEdgeToEdge()` defaults to `SystemBarStyle.auto`, which picks the icon tint from the
 * system's night mode. An example that themes itself from the configuration instead — as every
 * host of a `DocumentView` or a bare `PdfFragment` here does — then gets light status bar icons
 * over its light document whenever the device is in dark mode, which is unreadable. Swapping only
 * `auto`'s dark-mode detector keeps everything else it does, the scrims included: transparent
 * bars on the levels where the platform enforces contrast itself, and the same navigation bar
 * scrim androidx falls back to on API 26–28, where it can't.
 *
 * The two bars are decided separately because they sit over different content. Both default to
 * the configuration's theme, which is what fills the window behind them. Only the status bar is
 * routinely overridden — with [hasDarkMainToolbar], in a host that lets the SDK main toolbar draw
 * up into the status bar strip; pass [navigationBarIsDark] only in a host whose own chrome
 * likewise covers the bottom edge.
 *
 * Call it in `onCreate` next to [applyConfigurationTheme], before `super.onCreate`.
 */
fun ComponentActivity.enableConfigurationEdgeToEdge(
    configuration: PdfActivityConfiguration?,
    statusBarIsDark: Boolean = configuration.isNightTheme,
    navigationBarIsDark: Boolean = configuration.isNightTheme,
) {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { _: Resources -> statusBarIsDark },
        navigationBarStyle = SystemBarStyle.auto(NavigationBarLightScrim, NavigationBarDarkScrim) { _: Resources ->
            navigationBarIsDark
        },
    )
}

/**
 * `androidx.activity`'s own `DefaultLightScrim` and `DefaultDarkScrim` — the navigation bar scrims
 * `enableEdgeToEdge()` passes by default — copied because they are `internal` to that library.
 *
 * They only reach the window on API 26–28: from API 29 `auto` hands the platform a transparent bar
 * and lets `isNavigationBarContrastEnforced` draw the scrim. On those older levels there is no such
 * fallback, so this is the only thing keeping 3-button navigation off the bare document.
 */
private val NavigationBarLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val NavigationBarDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/**
 * Whether the themed main toolbar is dark enough that light system bar icons read over it.
 *
 * Pass this as [enableConfigurationEdgeToEdge]'s `statusBarIsDark` in a host that lets the SDK
 * main toolbar draw up into the status bar strip: there the icons sit over the toolbar, not over
 * the window background, so it is the toolbar's colour they have to contrast with. A host whose
 * chrome floats below the strip — a pill, a rounded bar — leaves the default alone.
 */
fun Context.hasDarkMainToolbar(): Boolean = ColorUtils.calculateLuminance(mainToolbarBackgroundColor()) < LIGHT_ICONS_LUMINANCE_CROSSOVER

/**
 * The relative luminance at which white and black contrast equally against a background, so the
 * point below which light system bar icons read better than dark ones.
 *
 * WCAG contrast is `(lighter + 0.05) / (darker + 0.05)`, which puts the crossover at
 * `sqrt(1.05 * 0.05) - 0.05`, not at the midpoint 0.5: a background at, say, 0.3 looks "light"
 * by a naive halfway test while white icons in fact contrast better over it.
 */
private const val LIGHT_ICONS_LUMINANCE_CROSSOVER = 0.1791

/**
 * The themed main toolbar background: `pspdf__mainToolbarStyle`'s `pspdf__backgroundColor`, or
 * `colorPrimary` where the style leaves it unset — the same resolution the SDK's own main toolbar
 * does.
 */
private fun Context.mainToolbarBackgroundColor(): Int {
    val colorPrimary = theme.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorPrimary)).use {
        it.getColor(0, Color.BLACK)
    }
    val toolbarStyle = theme.obtainStyledAttributes(intArrayOf(com.pspdfkit.R.attr.pspdf__mainToolbarStyle)).use {
        it.getResourceId(0, 0)
    }
    if (toolbarStyle == 0) return colorPrimary
    return theme.obtainStyledAttributes(toolbarStyle, com.pspdfkit.R.styleable.pspdf__MainToolbar).use {
        it.getColor(com.pspdfkit.R.styleable.pspdf__MainToolbar_pspdf__backgroundColor, colorPrimary)
    }
}
