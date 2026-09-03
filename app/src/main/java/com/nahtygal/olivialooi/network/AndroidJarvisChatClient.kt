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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
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
    private val cleanupExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "looloo-jarvis-cleanup").apply { isDaemon = true }
    }
    private val generation = AtomicLong(0L)
    private val activeConnection = AtomicReference<ConnectionHandle?>(null)

    @Volatile
    private var isClosed = false

    fun send(prompt: String, onResult: (JarvisChatResult) -> Unit) {
        if (isClosed) return
        val requestGeneration = generation.incrementAndGet()
        disconnectActiveConnectionAsync()
        Log.d(TAG, "JARVIS_REQUEST_QUEUED")

        executor.execute {
            Log.d(TAG, "JARVIS_WORKER_STARTED")
            val result = performRequest(prompt, requestGeneration)
            val callbackPosted = mainHandler.post {
                if (!isClosed && generation.get() == requestGeneration) onResult(result)
            }
            if (callbackPosted) Log.d(TAG, "JARVIS_CALLBACK_POSTED")
        }
    }

    fun cancel() {
        generation.incrementAndGet()
        disconnectActiveConnectionAsync()
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        cancel()
        executor.shutdownNow()
        cleanupExecutor.shutdown()
    }

    private fun performRequest(prompt: String, requestGeneration: Long): JarvisChatResult {
        if (bearerToken.isBlank()) {
            Log.w(TAG, "JARVIS_CONFIG_ERROR category=missing_token")
            return JarvisChatResult.Failure(JarvisChatFailure.Configuration)
        }

        val requestUrl = try {
            URL(baseUrl + JarvisChatConfig.CHAT_PATH).also {
                if (it.protocol != "https") throw IllegalArgumentException("Jarvis URL must use HTTPS")
            }
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "JARVIS_CONFIG_ERROR category=invalid_url")
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
            Log.w(TAG, "JARVIS_CONFIG_ERROR category=tls_setup")
            return JarvisChatResult.Failure(JarvisChatFailure.Configuration)
        }
        Log.d(TAG, "JARVIS_TLS_READY")

        val connectionHandle = ConnectionHandle(connection)
        if (generation.get() != requestGeneration || isClosed) {
            connectionHandle.disconnect()
            return JarvisChatResult.Failure(JarvisChatFailure.Connection)
        }
        activeConnection.set(connectionHandle)
        if (generation.get() != requestGeneration || isClosed) {
            activeConnection.compareAndSet(connectionHandle, null)
            connectionHandle.disconnect()
            return JarvisChatResult.Failure(JarvisChatFailure.Connection)
        }

        return try {
            val requestBody = serializeJarvisRequest(prompt).toByteArray(StandardCharsets.UTF_8)
            Log.d(
                TAG,
                "JARVIS_REQUEST_SIZE prompt_bytes=${prompt.toByteArray(StandardCharsets.UTF_8).size} " +
                    "body_bytes=${requestBody.size}",
            )
            connection.setFixedLengthStreamingMode(requestBody.size)
            connection.outputStream.use { it.write(requestBody) }
            Log.d(TAG, "JARVIS_CONNECTED")

            val statusCode = connection.responseCode
            Log.d(TAG, "JARVIS_HTTP_STATUS code=$statusCode")
            val responseStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = responseStream?.use(::readBoundedResponse).orEmpty()
            Log.d(TAG, "JARVIS_RESPONSE_RECEIVED")
            val result = mapJarvisResponse(statusCode, responseBody)
            if (result is JarvisChatResult.Failure) {
                if (statusCode !in 200..299) {
                    Log.w(TAG, "JARVIS_HTTP_ERROR code=$statusCode")
                } else {
                    Log.w(TAG, "JARVIS_PARSE_ERROR code=$statusCode")
                }
            }
            result
        } catch (error: SocketTimeoutException) {
            Log.w(TAG, "JARVIS_IO_ERROR category=timeout")
            JarvisChatResult.Failure(JarvisChatFailure.Timeout)
        } catch (error: IOException) {
            Log.w(TAG, "JARVIS_IO_ERROR category=connection")
            JarvisChatResult.Failure(JarvisChatFailure.Connection)
        } finally {
            activeConnection.compareAndSet(connectionHandle, null)
            connectionHandle.disconnect()
        }
    }

    private fun disconnectActiveConnectionAsync() {
        val connectionHandle = activeConnection.getAndSet(null) ?: return
        // HttpsURLConnection.disconnect() may block on TLS/socket cleanup; never run it on main.
        cleanupExecutor.execute(connectionHandle::disconnect)
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

    private class ConnectionHandle(private val connection: HttpsURLConnection) {
        private val isDisconnected = AtomicBoolean(false)

        fun disconnect() {
            if (isDisconnected.compareAndSet(false, true)) connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "LooLooJarvis"
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 70_000
        const val MAX_RESPONSE_BYTES = 8_192
    }
}
