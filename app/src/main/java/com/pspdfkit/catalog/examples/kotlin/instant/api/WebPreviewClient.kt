/*
 *   Copyright © 2020-2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.instant.api

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client for the Nutrient web preview server. In your own app, you would connect to your
 * own Document Engine (Previously Server) backend to get Instant document identifiers and authentication tokens.
 */
class WebPreviewClient(serverUrl: String = "https://web-examples.our.services.nutrient-powered.io/api/") {
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
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(WebPreviewService::class.java)
    }

    /**
     * Retrieves document descriptor for an existing group.
     */
    suspend fun getDocument(url: String): InstantExampleDocumentDescriptor = apiService.getDocument(url)

    /**
     * Creates a new group on web example server. Returns document descriptor.
     */
    suspend fun createNewDocument(): InstantExampleDocumentDescriptor = apiService.createDocument()

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
    suspend fun getDocument(@Url url: String): InstantExampleDocumentDescriptor

    @POST("instant-landing-page")
    suspend fun createDocument(): InstantExampleDocumentDescriptor

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
