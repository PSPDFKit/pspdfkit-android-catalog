/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.examples.java.FilterableThumbnailGridExample;
import java.util.ArrayList;
import java.util.List;

/**
 * A view used in {@link FilterableThumbnailGridExample} to provide filtering options in the
 * thumbnail grid header. See example's documentation for more info.
 */
public class FilterPickerView extends LinearLayout {

    @NonNull
    private final List<Filter> filters = new ArrayList<>();

    @Nullable
    private Filter selectedFilter = null;

    @Nullable
    private OnFilterClickedListener listener = null;

    @ColorInt
    private int backgroundColor;

    @ColorInt
    private int foregroundColor;

    @ColorInt
    private int backgroundColorSelected;

    @ColorInt
    private int foregroundColorSelected;

    public FilterPickerView(@NonNull final Context context) {
        super(context);

        backgroundColor = ContextCompat.getColor(context, com.pspdfkit.R.color.pspdf__color_white);
        foregroundColor = ContextCompat.getColor(context, com.pspdfkit.R.color.pspdf__color_gray_dark);
        backgroundColorSelected = ContextCompat.getColor(context, com.pspdfkit.R.color.pspdf__color);
        foregroundColorSelected = ContextCompat.getColor(context, com.pspdfkit.R.color.pspdf__color_white);

        int padding =
                (int) getContext().getResources().getDimension(R.dimen.pspdf__thumbnailGridFilterPickerViewPadding);
        setPadding(padding, padding, padding, padding);

        setId(R.id.filter_view);
        setOrientation(HORIZONTAL);
        setBackgroundColor(backgroundColor);
        setShowDividers(SHOW_DIVIDER_MIDDLE);
        setDividerDrawable(ContextCompat.getDrawable(context, R.drawable.thumbnail_grid_filter_view_divider));
    }

    public void setOnFilterClickedListener(@Nullable OnFilterClickedListener listener) {
        this.listener = listener;
    }

    public void setFilters(@NonNull List<Filter> filters) {
        this.filters.clear();
        this.filters.addAll(filters);
        generateLayout();
    }

    public void setSelectedFilter(@NonNull Filter selectedFilter) {
        this.selectedFilter = selectedFilter;
        invalidate();
    }

    @Nullable
    public Filter getSelectedFilter() {
        return selectedFilter;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0, n = getChildCount(); i < n; i++) {
            TextView tv = (TextView) getChildAt(i);
            tv.setTextColor(tv.getTag() == selectedFilter ? foregroundColorSelected : foregroundColor);
            tv.setBackgroundColor(tv.getTag() == selectedFilter ? backgroundColorSelected : backgroundColor);
        }
    }

    private void generateLayout() {
        removeAllViews();
        for (Filter filter : filters) {
            TextView filterTextView = new TextView(getContext());
            filterTextView.setTag(filter);
            filterTextView.setText(filter.getTitle());
            filterTextView.setClickable(true);
            filterTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            filterTextView.setOnClickListener(v -> {
                if (selectedFilter == v.getTag()) return;
                if (listener != null) listener.onFilterClicked((Filter) v.getTag());
            });
            int padding = (int) getContext()
                    .getResources()
                    .getDimension(R.dimen.pspdf__thumbnailGridFilterPickerViewSelectionPadding);
            filterTextView.setPadding(padding, padding, padding, padding);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.weight = 1;
            filterTextView.setLayoutParams(params);
            addView(filterTextView);
        }
    }

    public enum Filter {
        ALL("All"),
        ANNOTATED("Annotated"),
        BOOKMARKED("Bookmarked");

        @NonNull
        private String title;

        Filter(@NonNull String title) {
            this.title = title;
        }

        @NonNull
        public String getTitle() {
            return title;
        }
    }

    public interface OnFilterClickedListener {
        void onFilterClicked(@NonNull Filter filter);
    }
}
