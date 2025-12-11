/*
 *   Copyright © 2024 PSPDFKit GmbH. All rights reserved.
 *
 *   THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
 *   AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE PSPDFKIT LICENSE AGREEMENT.
 *   UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
 *   This notice may not be removed from this file.
 */
package com.pspdfkit.catalog.utils
import android.content.Context
import android.util.Base64
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date

/**
 * Class responsible for generating JWT tokens using the RS256 algorithm.
 * @param context The application context used to access assets.
 */
class JwtGenerator(private val context: Context) {

    /**
     * Generates a JWT token with the RS256 algorithm using a private key from the assets folder.
     * @param claims Map of claims to include in the JWT.
     * @param privateKeyPath Path to the .pem file in the assets folder.
     * @return The generated JWT token.
     * @throws RuntimeException if the token generation fails.
     */
    fun generateJwtToken(
        claims: Map<String, Any>,
        privateKeyPath: String = "keys/jwt.pem" // Default path in assets
    ): String {
        try {
            // Load the private key from the specified path in assets
            val privateKey = loadPrivateKeyFromAssets(privateKeyPath)
            // Build and sign the JWT token
            return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 hour expiry
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact()
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate JWT token", e)
        }
    }

    /**
     * Loads a private key from a .pem file in the assets folder.
     * @param path The path to the .pem file in the assets folder.
     * @return The loaded PrivateKey object.
     * @throws RuntimeException if the private key loading fails.
     */
    private fun loadPrivateKeyFromAssets(path: String): PrivateKey {
        try {
            // Read the PEM file from assets
            val privateKeyPEM = context.assets.open(path).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
            // Remove PEM headers and whitespace
            val keyString = privateKeyPEM
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim()

            // Decode the base64 encoded key and generate the PrivateKey object
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePrivate(keySpec)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load private key from assets", e)
        }
    }

    companion object {
        /**
         * Static method to generate a JWT token.
         * @param context The application context used to access assets.
         * @param claims Map of claims to include in the JWT.
         * @param privateKeyPath Path to the .pem file in the assets folder.
         * @return The generated JWT token.
         */
        fun generateJwtToken(
            context: Context,
            claims: Map<String, Any>,
            privateKeyPath: String = "keys/jwt.pem"
        ) = JwtGenerator(context).generateJwtToken(claims, privateKeyPath)
    }
}
