package org.battlo.freegrilly.data.stream

import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.battlo.freegrilly.data.api.BaseUrlInterceptor
import org.battlo.freegrilly.data.api.models.GrillStatusResponse
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * §8 — Push / real-time updates via Server-Sent Events.
 *
 * Connects to `GET /api/events` on the active device and emits a [GrillStatusResponse]
 * for every SSE frame. The [GrillyRepository] switches to this source instead of the
 * 1-second polling loop when the `events` capability flag is present.
 *
 * Reconnect: up to [MAX_RETRIES] attempts with exponential back-off (1 s → 2 s → … → 30 s).
 * If all retries are exhausted the Flow completes with an exception, and the repository
 * falls back to 1-second polling automatically.
 */
@Singleton
class GrillEventSource @Inject constructor(
    @Named("sse") private val sseClient: OkHttpClient,
    private val baseUrlInterceptor: BaseUrlInterceptor,
) {
    private val TAG = "GrillEventSource"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    companion object {
        private const val MAX_RETRIES = 10L
        private const val MAX_BACKOFF_MS = 30_000L
    }

    /**
     * Returns a cold [Flow] of [GrillStatusResponse] pushed by the device over SSE.
     * Cancelled when the caller's coroutine is cancelled (e.g. [GrillyRepository.stopPolling]).
     */
    fun stream(): Flow<GrillStatusResponse> = callbackFlow<GrillStatusResponse> {
        val host = baseUrlInterceptor.currentHost.value
        val request = Request.Builder()
            .url("http://$host/api/events")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        val factory = EventSources.createFactory(sseClient)
        val source = factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                if (data.isBlank()) return
                runCatching {
                    val status = json.decodeFromString<GrillStatusResponse>(data)
                    trySend(status)
                }.onFailure {
                    Log.w(TAG, "Failed to parse SSE frame: $data", it)
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                Log.w(TAG, "SSE connection failed (code=${response?.code})", t)
                close(t ?: RuntimeException("SSE connection failed (HTTP ${response?.code})"))
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE connection closed by server")
                close()
            }
        })

        awaitClose {
            Log.d(TAG, "SSE coroutine cancelled, closing EventSource")
            source.cancel()
        }
    }.retryWhen { cause, attempt ->
        if (attempt >= MAX_RETRIES) {
            Log.w(TAG, "SSE max retries ($MAX_RETRIES) exceeded, giving up")
            return@retryWhen false
        }
        val backoffMs = minOf(1_000L * (attempt + 1), MAX_BACKOFF_MS)
        Log.d(TAG, "SSE retry #${attempt + 1} in ${backoffMs}ms (cause: ${cause.message})")
        delay(backoffMs)
        true
    }
}
