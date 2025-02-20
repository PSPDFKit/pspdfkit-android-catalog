/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.pspdfkit.catalog.R;
import com.pspdfkit.catalog.SdkExample;
import com.pspdfkit.catalog.examples.java.activities.MultimediaAnnotationsActivity;
import com.pspdfkit.catalog.tasks.ExtractAssetTask;
import com.pspdfkit.configuration.activity.PdfActivityConfiguration;
import com.pspdfkit.ui.PdfActivityIntentBuilder;

/** This example showcases how to dynamically add multimedia content to a PDF document. */
public class DynamicMultimediaAnnotationExample extends SdkExample {

    public DynamicMultimediaAnnotationExample(Context context) {
        super(
                context.getString(R.string.dynamicMultimediaExampleTitle),
                context.getString(R.string.dynamicMultimediaExampleDescription));
    }

    @Override
    public void launchExample(@NonNull Context context, @NonNull PdfActivityConfiguration.Builder configuration) {
        configuration.videoPlaybackEnabled(true);

        // Before launching the example, we extract one video file to the private app folder. This
        // file will be used to dynamically add
        // another link annotation to the document at runtime.
        ExtractAssetTask.extract("media/videos/small.mp4", getTitle(), context, false, "mp4", videoFile -> {
            // Next extract the demo document and launch it.
            ExtractAssetTask.extract(QUICK_START_GUIDE, getTitle(), context, documentFile -> {
                // For normal multimedia content playback, it is not necessary to
                // subclass PdfActivity as no custom code is required (only
                // annotations using
                // the pspdfkit:// scheme have to be present). However, if you want
                // to dynamically add multimedia annotations to a document, it is
                // preferable
                // to do this using a custom activity class (as done by this
                // example).
                final Intent intent = PdfActivityIntentBuilder.fromUri(context, Uri.fromFile(documentFile))
                        .configuration(configuration.build())
                        .activityClass(MultimediaAnnotationsActivity.class)
                        .build();

                // Pass the file system path to our video file to the activity. The
                // activity will use the path to dynamically add a multimedia link
                // annotation
                // to the PDF for opening the extracted video.
                intent.putExtra(MultimediaAnnotationsActivity.EXTRA_VIDEO_PATH, videoFile.getAbsolutePath());
                context.startActivity(intent);
            });
        });
    }
}
