/*
 *   Copyright © 2014-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.compose.toolbar.state.ToolbarIcon;
import com.pspdfkit.compose.toolbar.state.ToolbarItem;
import com.pspdfkit.compose.toolbar.state.ToolbarItemState;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.configuration.activity.TabBarHidingMode;
import com.pspdfkit.document.DocumentSource;
import com.pspdfkit.document.providers.AssetDataProvider;
import com.pspdfkit.ui.DocumentDescriptor;
import com.pspdfkit.ui.PdfActivity;
import com.pspdfkit.ui.PdfActivityIntentBuilder;
import java.util.ArrayList;
import java.util.List;

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
        public void onCreate(Bundle savedInstanceState) {
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

            // The main toolbar is Compose now: instead of setting a navigation icon on the legacy
            // AppCompat Toolbar, declare a drawer item pinned to the toolbar's leading edge via the
            // toolbar items provider. The icon picks up the toolbar's icon tint automatically.
            setToolbarItemsProvider(defaultItems -> {
                final List<ToolbarItem> items = new ArrayList<>(defaultItems.size() + 1);
                items.add(new ToolbarItem.Button(
                        "catalog.document_switcher.drawer",
                        R.id.document_switcher_drawer,
                        "Open document list",
                        new ToolbarIcon.Resource(R.drawable.ic_drawer_menu),
                        new ToolbarItemState(),
                        ToolbarItem.ItemEdge.Start,
                        ToolbarItem.OverflowBehavior.AlwaysShow));
                items.addAll(defaultItems);
                return items;
            });

            // Toggle the drawer when the item is tapped; everything else keeps the SDK handling.
            setOnToolbarItemClickListener(id -> {
                if (id == R.id.document_switcher_drawer) {
                    if (drawerLayout.isDrawerOpen(drawerListView)) {
                        drawerLayout.closeDrawer(drawerListView);
                    } else {
                        drawerLayout.openDrawer(drawerListView);
                    }
                    return true;
                }
                return false;
            });
        }

        private void showDocument(@NonNull final DocumentDescriptor visibleItem) {
            // Close the drawer.
            drawerLayout.closeDrawer(drawerListView, false);
            // Show the visible document.
            getDocumentCoordinator().setVisibleDocument(visibleItem);
        }
    }
}
