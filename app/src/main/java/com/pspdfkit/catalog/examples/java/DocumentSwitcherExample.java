/*
 *   Copyright © 2014-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.activity.TabBarHidingMode;
import com.pspdfkit.document.DocumentSource;
import com.pspdfkit.document.providers.AssetDataProvider;
import com.pspdfkit.ui.DocumentDescriptor;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/**
 * This example extends the default `PdfActivity` and adds a side navigation drawer that allows to
 * switch between documents without leaving the activity.
 */
public class DocumentSwitcherExample extends SdkExample {

    public DocumentSwitcherExample(Context context) {
        super(context, R.string.documentSwitcherExampleTitle, R.string.documentSwitcherExampleDescription);
    }

    @Override
    public void launchExample(
            @NonNull final Context context, @NonNull final PdfActivityConfiguration.Builder configuration) {
        // Use a custom activity and a custom PdfActivity subclass.
        configuration
                .hideUserInterfaceWhenCreatingAnnotations(false)
                // Disable tab bar - example displays list of documents in a drawer instead.
                .setTabBarHidingMode(TabBarHidingMode.HIDE)
                // The custom layout has no content editor. In order to prevent the activity from accessing
                // it we have to deactivate it in the configuration.
                .contentEditingEnabled(false)
                .layout(R.layout.activity_document_switcher);

        // Launch the custom example activity using the (read-only) document from assets and
        // configuration.
        final Intent intent = PdfActivityIntentBuilder.fromDataProvider(context, new AssetDataProvider(WELCOME_DOC))
                .configuration(configuration.build())
                .activityClass(DocumentSwitcherActivity.class)
                .build();
        context.startActivity(intent);
    }

    /**
     * This example activity adds a navigation drawer menu to the default {@link PdfActivity} which
     * allows to navigate between different documents within the same activity.
     */
    public static class DocumentSwitcherActivity extends PdfActivity {

        /** List of documents presented in the navigation menu */
        private static final String[] assetFiles =
                new String[] {SdkExample.WELCOME_DOC, "Classbook.pdf", "Aviation.pdf", "Annotations.pdf"};

        private DrawerLayout drawerLayout;
        private ListView drawerListView;

        @SuppressWarnings("ConstantConditions")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Extract the drawer views from the layout.
            drawerLayout = findViewById(R.id.drawerLayout);
            drawerListView = findViewById(R.id.drawerListView);

            // We create a list of documents that can be loaded by this example. In a real-word scenario
            // this list would
            // probably be dynamic.
            drawerListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, assetFiles));
            drawerListView.setOnItemClickListener((parent, view, position, id) -> {
                // Get the name of the clicked asset file.
                final String assetName = (String) parent.getItemAtPosition(position);

                // Create document source for the document from assets.
                DocumentSource documentSource = new DocumentSource(new AssetDataProvider(assetName));

                // Search for document with the same source in the document coordinator.
                for (DocumentDescriptor documentDescriptor :
                        getDocumentCoordinator().getDocuments()) {
                    if (documentDescriptor.getDocumentSource().getUid().equals(documentSource.getUid())) {
                        // Show existing document immediately.
                        showDocument(documentDescriptor);
                        return;
                    }
                }

                // Document was not found in the document coordinator - create it manually
                // before showing.
                DocumentDescriptor documentDescriptor = DocumentDescriptor.fromDocumentSource(documentSource);
                if (getDocumentCoordinator().addDocument(documentDescriptor)) {
                    showDocument(documentDescriptor);
                }
            });

            // Setup the navigation drawer menu icon.
            final DrawerArrowDrawable drawerDrawable = new DrawerArrowDrawable(this);

            // We want to use the same color as the main toolbar items are tinted.
            final TypedArray a = getTheme()
                    .obtainStyledAttributes(
                            null,
                            com.pspdfkit.R.styleable.pspdf__ActionBarIcons,
                            com.pspdfkit.R.attr.pspdf__actionBarIconsStyle,
                            com.pspdfkit.R.style.PSPDFKit_ActionBarIcons);
            int toolbarIconsColor = a.getColor(
                    com.pspdfkit.R.styleable.pspdf__ActionBarIcons_pspdf__iconsColor,
                    ContextCompat.getColor(this, com.pspdfkit.R.color.pspdf__onPrimaryLight));
            a.recycle();

            drawerDrawable.setColor(toolbarIconsColor);

            // Get toolbar and set navigation icon directly
            final Toolbar toolbar = findViewById(com.pspdfkit.R.id.pspdf__toolbar_main);
            if (toolbar != null) {
                toolbar.setNavigationIcon(drawerDrawable);
            }
        }

        /**
         * If the user presses the navigation drawer icon, we toggle the drawer visibility. Otherwise we
         * pass the event on to the default implementation.
         */
        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == android.R.id.home) {
                if (drawerLayout.isDrawerOpen(drawerListView)) {
                    drawerLayout.closeDrawer(drawerListView);
                } else {
                    drawerLayout.openDrawer(drawerListView);
                }

                return true;
            }

            return super.onOptionsItemSelected(item);
        }

        private void showDocument(@NonNull final DocumentDescriptor visibleItem) {
            // Close the drawer.
            drawerLayout.closeDrawer(drawerListView, false);
            // Show the visible document.
            getDocumentCoordinator().setVisibleDocument(visibleItem);
        }
    }
}
