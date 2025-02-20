/*
 *   Copyright © 2020-2024 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.instant.api

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client for the PSPDFKit web preview server. In your own app, you would connect to your
 * own Document Engine (Previously Server) backend to get Instant document identifiers and authentication tokens.
 */
class WebPreviewClient constructor(serverUrl: String = "https://web-examples.services.demo.pspdfkit.com/api/") {
    private val apiService: WebPreviewService
    private val basicAuthInterceptor: BasicAuthInterceptor = BasicAuthInterceptor()

    init {
        val logging = HttpLoggingInterceptor { s -> println(s) }
        logging.level = HttpLoggingInterceptor.Level.HEADERS

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(basicAuthInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(okHttpClient)
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(WebPreviewService::class.java)
    }

    /**
     * Retrieves document descriptor for an existing group.
     */
    fun getDocument(url: String): Single<InstantExampleDocumentDescriptor> {
        return apiService.getDocument(url).subscribeOn(Schedulers.io())
    }

    /**
     * Creates a new group on web example server. Returns document descriptor.
     */
    fun createNewDocument(): Single<InstantExampleDocumentDescriptor> {
        return apiService.createDocument().subscribeOn(Schedulers.io())
    }

    /** Sets the basic auth credentials to use when doing API requests. */
    fun setBasicAuthCredentials(username: String, password: String) {
        basicAuthInterceptor.setCredentials(username, password)
    }
}

/**
 * Interface for the web preview retrofit service.
 */
private interface WebPreviewService {
    @Headers(ACCEPT_HEADER)
    @GET
    fun getDocument(@Url url: String): Single<InstantExampleDocumentDescriptor>

    @POST("instant-landing-page")
    fun createDocument(): Single<InstantExampleDocumentDescriptor>

    companion object {
        const val ACCEPT_HEADER = "Accept: application/vnd.instant-example+json"
    }
}

private class BasicAuthInterceptor : Interceptor {
    private var credentials: String? = null
    fun setCredentials(user: String, password: String) {
        credentials = Credentials.basic(user, password)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return if (credentials != null) {
            val authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials!!).build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(request)
        }
    }
}
