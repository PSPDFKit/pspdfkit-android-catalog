/*
 *   Copyright © 2018-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.java.activities

import android.graphics.RectF
import android.net.Uri
import com.pspdfkit.annotations.LinkAnnotation
import com.pspdfkit.annotations.actions.UriAction
import com.pspdfkit.catalog.examples.java.DynamicMultimediaAnnotationExample
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.ui.PdfActivity
import org.intellij.lang.annotations.Language
import java.io.File

/**
 * This activity is part of the [DynamicMultimediaAnnotationExample] and shows how to dynamically add multimedia annotations to a PDF document.
 */
class MultimediaAnnotationsActivity : PdfActivity() {
    companion object {
        /** This is the filesystem path of a video file we're going to dynamically add to the PDF document. */
        const val EXTRA_VIDEO_PATH = "videoPath"
    }

    override fun onDocumentLoaded(document: PdfDocument) {
        super.onDocumentLoaded(document)

        // We add a video link to the first page.
        addVideoAnnotation()

        // We add a gallery link to the second page.
        addGalleryAnnotation()
    }

    /**
     * This method adds a link annotation to a video on the local file system.
     */
    private fun addVideoAnnotation() {
        // Get the rect for the link annotation we want to add. We're using absolute page coordinates here, but your app can use different positioning techniques
        // like for example using multimedia position data that you would retrieve from your server. This rect is in the center of the page, so tapping the
        // page will trigger the video.
        val linkAnnotationRect = RectF(0f, 768f, 768f, 256f)

        // We're going to place the video on the first page of the document.
        val pageIndex = 0

        // The activity was launched with the path to a video on the local file system. We're going to use this file for playback with the multimedia annotation.
        val videoPathUri = intent.getStringExtra(EXTRA_VIDEO_PATH)?.let { Uri.fromFile(File(it)) }
            ?: throw IllegalStateException("No string value found for $EXTRA_VIDEO_PATH.")

        // Create the link annotation. The LinkAnnotation constructor takes the index of the page on which the annotation will be added.
        val multimediaLinkAnnotation = LinkAnnotation(pageIndex).apply {
            // Set the position of the link annotation on the page.
            boundingBox = linkAnnotationRect

            // To let the link point to the multimedia content, we have to set a UriAction on the link. The UriAction encodes the actual URL of the content.
            // The multimedia URL uses the pspdfkit:// URL scheme, has optional options in square brackets, followed by the local file system URI of the file.
            // Note the the videoPathUri also carries a file:// scheme which is required for video discovery. For a comprehensive list of supported URI formats
            // and options, please consult our Multimedia Annotation online guides at: https://pspdfkit.com/guides/android/current/annotations/multimedia-annotations/
            action = UriAction("pspdfkit://[autoplay:true]$videoPathUri")
        }

        // Add the annotation to the document and show it.
        requirePdfFragment().addAnnotationToPage(multimediaLinkAnnotation, false)

        // The line above uses the fragment to add the annotation to the document. This will immediately update the rendered page too, making the newly added
        // annotation visible. Alternatively, you can use the document's annotation provider, and manually trigger an annotation updated notification on the
        // fragment. Like so:
        //
        //   document.annotationProvider.addAnnotationToPage(multimediaLinkAnnotation)
        //   fragment.notifyAnnotationHasChanged(multimediaLinkAnnotation)
    }

    /**
     * This method adds a link annotation to a custom gallery on the local file system.
     */
    private fun addGalleryAnnotation() {
        // This example defines a custom JSON content for the gallery. It writes this JSON to a local file and then create an annotation pointing to the file.
        // See our online guides at https://pspdfkit.com/guides/android/current/annotations/multimedia-annotations/ for a full specification of the JSON format
        // used by galleries.
        @Language("JSON")
        val galleryJson = """
            [
              {
                "contentURL": "https://farm4.staticflickr.com/3701/13630138733_abf2411bd1_z.jpg",
                "caption": "This is a local image. Captions are optional"
              },
              {
                "contentURL": "https://farm3.staticflickr.com/2157/3527157206_f3ebec9909_z.jpg",
                "caption": "This is a local image. Captions are optional"
              }
            ]
        """.trimIndent()

        // Write the JSON to a local gallery file on the file system. It is important that the file uses the .gallery file extension so that PSPDFKit can
        // recognize the file for showing a gallery.
        val outputFile = filesDir.resolve("sample.gallery").apply {
            // For the sake of this example, we recreate the gallery file from scratch. We therefore delete it if it already exists.
            if (exists()) delete()
            // Write the whole JSON to the output file.
            writeText(galleryJson)
        }

        // Get the rect for the link annotation we want to add. We're using absolute page coordinates here. These are the same coordinates used by the video
        // annotation created in the method above, but we're adding the gallery to the second page (instead of the first one).
        val linkAnnotationRect = RectF(0f, 768f, 768f, 256f)

        // We're going to place the gallery on the second page of the document.
        val pageIndex = 1

        // Pointing the link to the gallery file is enough for showing the gallery. It is essential that Uri.fromFile() is used, so that the path inside the
        // link annotation has the correct format and includes the file:// URL scheme.
        val galleryPathUri = Uri.fromFile(outputFile)

        // Create the link annotation. The LinkAnnotation constructor takes the index of the page on which the annotation will be added.
        val galleryLinkAnnotation = LinkAnnotation(pageIndex).apply {
            // Set the position of the link annotation on the page.
            boundingBox = linkAnnotationRect

            // To let the link point to the gallery file, we have to set a UriAction on the link. The UriAction encodes the actual URL of the content.
            // The multimedia URL uses the pspdfkit:// URL scheme followed by the local file system URI of the file. Galleries don't support multimedia options.
            // Note the the galleryPathUri also carries a file:// scheme which is required for gallery discovery. For a comprehensive list of supported URI
            // formats, please consult our Multimedia Annotation online guides at: https://pspdfkit.com/guides/android/current/annotations/multimedia-annotations/
            action = UriAction("pspdfkit://$galleryPathUri")
        }

        // Add the annotation to the document and show it.
        pdfFragment?.addAnnotationToPage(galleryLinkAnnotation, false)
    }
}
