/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pspdfkit.catalog.R
import com.pspdfkit.catalog.SdkExample
import com.pspdfkit.catalog.SdkExample.Companion.WELCOME_DOC
import com.pspdfkit.configuration.PdfConfiguration
import com.pspdfkit.configuration.activity.PdfActivityConfiguration
import com.pspdfkit.document.DocumentSource
import com.pspdfkit.document.PdfDocument
import com.pspdfkit.document.PdfDocumentLoader
import com.pspdfkit.document.providers.AssetDataProvider
import com.pspdfkit.document.search.TextSearch
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asFlowable
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.rx3.rxSingle

/**
 * Shows how to keep an existing RxJava codebase working against the SDK's `suspend` and `Flow` APIs
 * by adapting at the boundary, rather than converting your own call sites.
 *
 * The SDK itself contains no RxJava. Everything here comes from `kotlinx-coroutines-rx3`, which you
 * add to your own build file:
 *
 * ```
 * implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:<version>")
 * ```
 *
 * Three bridges cover essentially every case, and this example runs all three against real SDK APIs:
 *
 * | SDK API | returns | bridge |
 * |---|---|---|
 * | [PdfDocumentLoader.openDocumentAsync] | `suspend T` | `rxSingle { }` |
 * | [PdfDocument.initPageCacheAsync] | `suspend Unit` | `rxCompletable { }` |
 * | [TextSearch.performSearchAsync] | `Flow<T>` | `asFlowable()` |
 *
 * `rxSingle` and `rxCompletable` take a suspending lambda, so they need a Kotlin call site.
 * `asFlowable` does not: it is an ordinary static underneath, so Java can convert a `Flow` with no
 * Kotlin in the call path at all — `CustomSearchUiExample` does that, and is worth a look if your
 * own call sites are Java.
 *
 * The full mapping is in `documentation/rxjava-to-coroutines-migration-guide.md`.
 */
class RxJavaInteropExample(context: Context) :
    SdkExample(context, R.string.rxJavaInteropExampleTitle, R.string.rxJavaInteropExampleDescription) {
    override fun launchExample(context: Context, configuration: PdfActivityConfiguration.Builder) {
        context.startActivity(Intent(context, RxJavaInteropActivity::class.java))
    }
}

class RxJavaInteropActivity : AppCompatActivity() {
    private val disposables = CompositeDisposable()
    private lateinit var log: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log = TextView(this).apply {
            setPadding(PADDING, PADDING, PADDING, PADDING)
            gravity = Gravity.TOP
            setTextIsSelectable(true)
        }
        setContentView(ScrollView(this).apply { addView(log) })

        append("Running each bridge against a migrated SDK API.\n")
        runBridges()
    }

    private fun runBridges() {
        // rxSingle wraps a suspend function returning a value. The lambda is the coroutine; the
        // Single is what the rest of your Rx chain sees.
        val openDocument: Single<PdfDocument> =
            rxSingle {
                // `this` inside the builder is the coroutine scope, not the Activity.
                PdfDocumentLoader.openDocumentAsync(
                    this@RxJavaInteropActivity,
                    DocumentSource(AssetDataProvider(WELCOME_DOC)),
                )
            }

        disposables.add(
            openDocument
                .doOnSuccess { append("rxSingle — opened a ${it.pageCount}-page document") }
                // rxCompletable wraps a suspend function returning Unit. andThen resumes the chain
                // with the document once the cache is warm.
                .flatMap { document ->
                    rxCompletable { document.initPageCacheAsync() }
                        .doOnComplete { append("rxCompletable — page cache initialised") }
                        .andThen(Single.just(document))
                }
                // asFlowable turns a Flow-returning API back into a Flowable, so the rest of the
                // chain stays Rx.
                .flatMapPublisher { document ->
                    TextSearch(document, PdfConfiguration.Builder().build())
                        .performSearchAsync(SEARCH_TERM)
                        .asFlowable()
                }
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { results ->
                        append("asFlowable — '$SEARCH_TERM' found ${results.size} times\n\nAll three bridges completed.")
                    },
                    { error -> append("Failed: $error") },
                ),
        )
    }

    override fun onDestroy() {
        // A CompositeDisposable still works exactly as it always did — the SDK returning coroutines
        // changes nothing about how you manage your own subscriptions.
        disposables.clear()
        super.onDestroy()
    }

    private fun append(line: String) = runOnUiThread { log.append("$line\n") }

    private companion object {
        const val SEARCH_TERM = "PDF"
        const val PADDING = 48
    }
}
