/**
 * For licensing, see LICENSE.txt
 * @author Jareau Davies
 */
package nl.avisi.kotlinwebtest.http

import nl.avisi.kotlinwebtest.Credentials
import nl.avisi.kotlinwebtest.UsernamePassword
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.ssl.SSLContextBuilder
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext

fun sslHostnameVerifier(): HostnameVerifier =
        HostnameVerifier { _, _ -> true }

fun sslContext(): SSLContext =
        SSLContextBuilder().loadTrustMaterial(null, TrustSelfSignedStrategy()).build()

fun credentialsProvider(credentials: Credentials?) =
        credentials?.let {
            BasicCredentialsProvider().apply {
                setCredentials(AuthScope.ANY, mapCredentials(credentials))
            }
        }

fun mapCredentials(credentials: Credentials): UsernamePasswordCredentials =
        when (credentials) {
            is UsernamePassword -> UsernamePasswordCredentials(credentials.user, credentials.password)
            else -> error("Unsupported credential type: " + credentials)
        }
