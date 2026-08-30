package com.nahtygal.olivialooi.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nahtygal.olivialooi.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

/**
 * Owns one background worker and at most one Jarvis request.
 *
 * Each request has a generation number so a cancelled or older response cannot update the UI.
 */
internal class AndroidJarvisChatClient(
    context: Context,
    private val baseUrl: String = JarvisChatConfig.baseUrl,
    private val bearerToken: String = JarvisChatConfig.bearerToken,
) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "looloo-jarvis").apply { isDaemon = true }
    }
    private val generation = AtomicLong(0L)

    @Volatile
    private var activeConnection: HttpsURLConnection? = null

    @Volatile
    private var isClosed = false

    fun send(prompt: String, onResult: (JarvisChatResult) -> Unit) {
        if (isClosed) return
        val requestGeneration = generation.incrementAndGet()
        activeConnection?.disconnect()

        executor.execute {
            val result = performRequest(prompt, requestGeneration)
            mainHandler.post {
                if (!isClosed && generation.get() == requestGeneration) onResult(result)
            }
        }
    }

    fun cancel() {
        generation.incrementAndGet()
        activeConnection?.disconnect()
        activeConnection = null
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        cancel()
        executor.shutdownNow()
    }

    private fun performRequest(prompt: String, requestGeneration: Long): JarvisChatResult {
        if (bearerToken.isBlank()) {
            Log.w(TAG, "Jarvis handheld token is not configured")
            return JarvisChatResult.Failure(JarvisChatFailure.Configuration)
        }

        val requestUrl = try {
            URL(baseUrl + JarvisChatConfig.CHAT_PATH).also {
                if (it.protocol != "https") throw IllegalArgumentException("Jarvis URL must use HTTPS")
            }
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Jarvis URL configuration is invalid", error)
            return JarvisChatResult.Failure(JarvisChatFailure.Configuration)
        }

        val connection = try {
            (requestUrl.openConnection() as HttpsURLConnection).apply {
                sslSocketFactory = createJarvisSocketFactory()
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                instanceFollowRedirects = false
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        } catch (error: Exception) {
            Log.w(TAG, "Could not prepare the Jarvis HTTPS connection", error)
            return JarvisChatResult.Failure(JarvisChatFailure.Configuration)
        }

        if (generation.get() != requestGeneration || isClosed) {
            connection.disconnect()
            return JarvisChatResult.Failure(JarvisChatFailure.Connection)
        }
        activeConnection = connection

        return try {
            val requestBody = serializeJarvisRequest(prompt).toByteArray(StandardCharsets.UTF_8)
            connection.setFixedLengthStreamingMode(requestBody.size)
            connection.outputStream.use { it.write(requestBody) }

            val statusCode = connection.responseCode
            val responseStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = responseStream?.use(::readBoundedResponse).orEmpty()
            val result = mapJarvisResponse(statusCode, responseBody)
            if (result is JarvisChatResult.Failure) {
                Log.w(TAG, "Jarvis request failed: status=$statusCode reason=${result.reason}")
            }
            result
        } catch (error: SocketTimeoutException) {
            Log.w(TAG, "Jarvis request timed out", error)
            JarvisChatResult.Failure(JarvisChatFailure.Timeout)
        } catch (error: IOException) {
            Log.w(TAG, "Jarvis request could not connect", error)
            JarvisChatResult.Failure(JarvisChatFailure.Connection)
        } finally {
            if (activeConnection === connection) activeConnection = null
            connection.disconnect()
        }
    }

    private fun createJarvisSocketFactory(): SSLSocketFactory {
        val certificate = applicationContext.resources
            .openRawResource(R.raw.jarvis_handheld_ca)
            .use { CertificateFactory.getInstance("X.509").generateCertificate(it) }
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setCertificateEntry("jarvis-handheld-ca", certificate)
        }
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm(),
        ).apply {
            init(keyStore)
        }
        return SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, null)
        }.socketFactory
    }

    private fun readBoundedResponse(input: java.io.InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var totalBytes = 0
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead < 0) break
            totalBytes += bytesRead
            if (totalBytes > MAX_RESPONSE_BYTES) throw IOException("Jarvis response is too large")
            output.write(buffer, 0, bytesRead)
        }
        return output.toString(StandardCharsets.UTF_8.name())
    }

    private companion object {
        const val TAG = "LooLooJarvis"
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 70_000
        const val MAX_RESPONSE_BYTES = 8_192
    }
}
