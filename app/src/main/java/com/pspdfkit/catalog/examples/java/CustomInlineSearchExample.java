/*
 *   Copyright © 2017-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import static com.pspdfkit.catalog.tasks.ExtractAssetTask.extract;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.search.SearchType;
import com.pspdfkit.listeners.OnVisibilityChangedListener;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import com.pspdfkit.ui.search.PdfSearchViewInline;

/**
 * This example takes the {@link PdfSearchViewInline} and places it inside a custom layout. To do so
 * it uses a custom activity {@link CustomInlineSearchExampleActivity} which will manage creation of
 * the new layout as well as showing and hiding of the inline search.
 */
public class CustomInlineSearchExample extends SdkExample {

    public CustomInlineSearchExample(@NonNull final Context context) {
        super(context, R.string.customInlineSearchExampleTitle, R.string.customInlineSearchExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // This example requires inline search, so override any catalog configuration.
        // To change the location of the PdfSearchViewInline as well as the way it is integrated,
        // you have to use a custom activity.
        configuration.setSearchType(SearchType.INLINE).searchEnabled(true);

        // Load the example document from the assets and launch the activity.
        extract(WELCOME_DOC, getTitle(), context, documentFile -> {
            final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                    .configuration(configuration.build())
                    .activityClass(CustomInlineSearchExampleActivity.class)
                    .build();

            // Start the CustomInlineSearchExampleActivity for the extracted document.
            context.startActivity(intent);
        });
    }

    /**
     * This activity supports all features of the original {@link PdfActivity} and uses its {@link
     * PdfSearchViewInline} in a custom layout.
     */
    public static class CustomInlineSearchExampleActivity extends PdfActivity {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setSupportActionBar(null);
            // Extract the content area of the PdfActivity. It is a FrameLayout and will be used as a
            // top-level view group holding our
            // inline search view.
            final FrameLayout content = findViewById(R.id.pspdf__activity_content);
            assert content != null;

            // The inline search is retrieved from the PdfActivity. That way it is already hooked up to
            // the fragment and will handle search
            // result highlighting, and showing of results.
            final PdfSearchViewInline inlineSearchView =
                    (PdfSearchViewInline) getPSPDFKitViews().getSearchView();
            if (inlineSearchView == null) {
                throw new IllegalStateException("Inline search view was null!");
            }

            // This example will add the inline search on the top of the activity, as a sliding panel –
            // right below the action bar.
            final FrameLayout container = (FrameLayout)
                    getLayoutInflater().inflate(R.layout.activity_custom_inline_search_container, content, false);
            // Remove the inline search from it's original parent.
            if (inlineSearchView.getParent() != null) {
                ((ViewGroup) inlineSearchView.getParent()).removeView(inlineSearchView);
            }
            // Add the inline search to the sliding panel container.
            container.addView(
                    inlineSearchView,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            // Add the container to the activity layout.
            content.addView(
                    container,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));
            // And make it invisible by default.
            container.setVisibility(View.INVISIBLE);

            // Apply any system window insets from the top (without actually consuming them) so that the
            // inline search is not covered
            // by the action bar or the status bar.
            ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
                ((ViewGroup.MarginLayoutParams) container.getLayoutParams()).topMargin =
                        insets.getSystemWindowInsetTop();
                return insets;
            });

            // To show and hide the inline search, register an OnVisibilityChangedListener which will be
            // called every time the view was
            // requested or dismissed by the user. Since the PdfActivity has by default its own listener
            // which handles attaching it to the
            // action bar, existing listeners have to be cleared first.
            inlineSearchView.clearOnVisibilityChangedListeners();
            inlineSearchView.addOnVisibilityChangedListener(new OnVisibilityChangedListener() {
                @Override
                public void onShow(@NonNull View view) {
                    container.setVisibility(View.VISIBLE);
                    container.setTranslationY(-view.getHeight());
                    container.animate().translationY(0).start();
                }

                @Override
                public void onHide(@NonNull final View view) {
                    container
                            .animate()
                            .translationY(-container.getHeight())
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    container.setVisibility(View.INVISIBLE);
                                    container.animate().setListener(null);
                                }
                            })
                            .start();
                }
            });
        }
    }
}
