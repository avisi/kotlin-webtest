package nl.avisi.kotlinwebtest.http

import nl.avisi.kotlinwebtest.Credentials
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients

fun getHttpClient(credentials: Credentials?): CloseableHttpClient =
        HttpClients.custom()
                .setSSLSocketFactory(SSLConnectionSocketFactory(sslContext(), sslHostnameVerifier()))
                .setDefaultCredentialsProvider(credentialsProvider(credentials))
                .build()