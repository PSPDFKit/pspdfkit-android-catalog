/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.pspdfkit.catalog.R;

public class Utils {
    /**
     * Returns {@code true} if the app has read and write permission to external storage.
     *
     * @param context The context used as context for permission request.
     * @return {@code true} if the app has read and write access to external storage, otherwise
     *     {@code false}.
     */
    public static boolean hasExternalStorageRwPermission(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Requests read and write permissions to external storage.
     *
     * @param activity The activity used as context for permission request.
     * @param requestCode Application specific request code to match with a result reported to
     *     {@link ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int,
     *     String[], int[])}.
     * @return True when permission has been already granted.
     */
    public static boolean requestExternalStorageRwPermission(@NonNull final Activity activity, final int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format(
                        "package:%s", activity.getApplicationContext().getPackageName())));
                activity.startActivityForResult(intent, requestCode);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, requestCode);
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        requestCode);
                return false;
            }
        }
        return true;
    }

    /**
     * Licensed under the Apache License, Version 2.0, from <a
     * href="https://github.com/consp1racy/material-navigation-drawer/blob/master/navigation-drawer/src/main/java/net/xpece/material/navigationdrawer/NavigationDrawerUtils.java">source</a>
     */
    public static void setProperNavigationDrawerWidth(final View view) {
        final Context context = view.getContext();
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int smallestWidthPx = Math.min(
                        context.getResources().getDisplayMetrics().widthPixels,
                        context.getResources().getDisplayMetrics().heightPixels);
                int drawerMargin = context.getResources().getDimensionPixelOffset(R.dimen.drawer_margin);

                view.getLayoutParams().width = Math.min(
                        context.getResources().getDimensionPixelSize(R.dimen.drawer_max_width),
                        smallestWidthPx - drawerMargin);
                view.requestLayout();
            }
        });
    }

    /**
     * Hide keyboard if visible.
     *
     * @param view view used to retrieve context and window token for hiding keyboard
     */
    public static void hideKeyboard(final @NonNull View view) {
        IBinder windowToken = view.getWindowToken();

        // If the window token is null, keyboard hiding will only work on some devices. Samsung
        // devices are known to require a window token for that action.
        if (windowToken == null) {
            if (view.getContext() instanceof Activity) {
                final Activity activity = (Activity) view.getContext();
                if (activity.getWindow() != null) {
                    windowToken = activity.getWindow().getDecorView().getWindowToken();
                }
            }
            if (windowToken == null) {
                Log.w(
                        "PSPDFKit",
                        "KeyboardUtils#hideKeyboard was called with a detached view. Hiding keyboard will not work on some device.");
            }
        }

        // Hide if shown before.
        final InputMethodManager imm =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(windowToken, 0);
    }

    /**
     * Extract color from theme attribute.
     *
     * @param context Android context.
     * @param attribute Attribute with color.
     * @param defaultColorResource Default color resource to use if attribute is not present in the
     *     theme.
     * @return color resource from theme
     */
    @ColorInt
    public static int getThemeColor(
            @NonNull final Context context, @AttrRes int attribute, @ColorRes int defaultColorResource) {
        final TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attribute, value, true)) {
            return value.data;
        } else {
            return ContextCompat.getColor(context, defaultColorResource);
        }
    }

    /** Converts dip values to pixels on current device. */
    public static int dpToPx(@NonNull Context ctx, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics());
    }
}
