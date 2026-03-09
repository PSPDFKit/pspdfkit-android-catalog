/*
 *   Copyright © 2026 PSPDFKit GmbH. All rights reserved.
 *
 *   The PSPDFKit Sample applications are licensed with a modified BSD license.
 *   Please see License for details. This notice may not be removed from this file.
 */

package com.pspdfkit.catalog.examples.kotlin.instant.api

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Client for the Node/Rails example server endpoints used by the catalog Instant flow.
 *
 * It authenticates via Basic auth where the username is required and the password is empty,
 * matching the example server's /api endpoints.
 *
 * Example server implementation:
 * https://github.com/PSPDFKit/pspdfkit-server-example-nodejs
 */
internal class ExampleServerClient(serverUrl: String, username: String) {
    private val apiService: ExampleServerService

    init {
        val authHeader = Credentials.basic(username, "")
        val okHttpClient =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    Interceptor { chain ->
                        val request =
                            chain
                                .request()
                                .newBuilder()
                                .header("Authorization", authHeader)
                                .build()
                        chain.proceed(request)
                    },
                ).connectTimeout(30, TimeUnit.SECONDS)
                .build()
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(ensureTrailingSlash(serverUrl))
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        apiService = retrofit.create(ExampleServerService::class.java)
    }

    /** Lists documents from `/api/documents` for the given example server user. */
    suspend fun getDocuments(): List<ExampleServerDocument> = apiService.getDocuments().documents

    /** Retrieves a JWT for a single document from `/api/document/:id`. */
    suspend fun getJwt(documentId: String): String = apiService.getDocument(documentId).token

    private fun ensureTrailingSlash(url: String): String = if (url.endsWith("/")) url else "$url/"

    companion object {
        /** Document Engine port used by the example docker-compose setup. */
        const val INSTANT_SERVER_PORT = 5000

        /** Web example server port used by the example docker-compose setup. */
        const val WEB_EXAMPLE_SERVER_PORT = 3000
    }
}

internal data class ExampleServerDocumentList(val documents: List<ExampleServerDocument>)

internal data class ExampleServerDocument(val id: String, val title: String, val layers: List<String>, val tokens: List<String>)

internal data class ExampleServerTokenResponse(val success: Boolean, val token: String, val message: String? = null)

private interface ExampleServerService {
    @GET("api/documents")
    suspend fun getDocuments(): ExampleServerDocumentList

    @GET("api/document/{id}")
    suspend fun getDocument(@Path("id") documentId: String): ExampleServerTokenResponse
}
