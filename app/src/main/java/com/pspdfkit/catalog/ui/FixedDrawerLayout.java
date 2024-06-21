/*
 *   Copyright © 2017-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Drawer layout has a crash bug while handling multi-touch. Temporary workaround is to eat the
 * exception when that happens - it does not effect functionality.
 */
public class FixedDrawerLayout extends DrawerLayout {

    public FixedDrawerLayout(Context context) {
        super(context);
    }

    public FixedDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (Exception t) {
            Log.e("FixedDrawerLayout", "Exception: ", t);
            return false;
        }
    }
}
