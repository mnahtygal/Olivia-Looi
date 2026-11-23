package com.nahtygal.olivialooi.network

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.net.URL
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import com.nahtygal.olivialooi.R

internal data class ResolvedJarvisEndpoint(
    val endpoint: JarvisEndpoint,
    val network: android.net.Network? = null,
)

internal class JarvisEndpointResolver(context: Context) {

    private val appContext = context.applicationContext

    fun resolve(): ResolvedJarvisEndpoint {
        if (!hasLocalNetworkPermission()) {
            return ResolvedJarvisEndpoint(JarvisEndpoint.AWAY)
        }

        val connectivity = appContext.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val networks = connectivity.allNetworks

        for (network in networks) {
            val capabilities = connectivity.getNetworkCapabilities(network)
                ?: continue

            val localTransport =
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

            if (!localTransport) continue

            if (probeHome(network)) {
                return ResolvedJarvisEndpoint(JarvisEndpoint.HOME, network)
            }
        }

        return ResolvedJarvisEndpoint(JarvisEndpoint.AWAY)
    }

    private fun hasLocalNetworkPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 37) return true

        return appContext.checkSelfPermission(
            "android.permission.ACCESS_LOCAL_NETWORK"
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun probeHome(network: android.net.Network): Boolean {
        val url = URL(
            JarvisChatConfig.baseUrl + "/health"
        )

        var connection: HttpsURLConnection? = null

        return try {
            connection = network.openConnection(url) as HttpsURLConnection
            connection.sslSocketFactory = createHomeSocketFactory()
            connection.requestMethod = "GET"
            connection.connectTimeout = 1500
            connection.readTimeout = 1500
            connection.instanceFollowRedirects = false

            connection.responseCode == 200 &&
                connection.inputStream.bufferedReader().use { reader ->
                    val body = reader.readText()
                    body.length <= 256 &&
                        org.json.JSONObject(body).optString("status") == "ok"
                }
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun createHomeSocketFactory(): SSLSocketFactory {
        val certificate = appContext.resources
            .openRawResource(R.raw.jarvis_handheld_ca)
            .use {
                CertificateFactory.getInstance("X.509")
                    .generateCertificate(it)
            }

        val keyStore = KeyStore.getInstance(
            KeyStore.getDefaultType()
        ).apply {
            load(null)
            setCertificateEntry("jarvis-handheld-ca", certificate)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        ).apply {
            init(keyStore)
        }

        return SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }.socketFactory
    }
}
