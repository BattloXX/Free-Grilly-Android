package org.battlo.freegrilly

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.battlo.freegrilly.data.api.BaseUrlInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class BaseUrlInterceptorTest {
    @Test fun `invalid hosts fail as IOExceptions`() {
        val interceptor = BaseUrlInterceptor()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        listOf("Grilly Garten.local", "192.168.1.5:80", "http://192.168.1.5").forEach { host ->
            interceptor.currentHost.value = host
            val error = assertThrows(IOException::class.java) {
                client.newCall(Request.Builder().url("http://192.168.1.5/").build()).execute()
            }
            assertEquals("Invalid device host: $host", error.message)
            assertTrue(error.cause is IllegalArgumentException)
        }
    }

    @Test fun `enqueue delivers invalid host failure to callback`() {
        val interceptor = BaseUrlInterceptor().apply { currentHost.value = "Grilly Garten.local" }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val completed = CountDownLatch(1)
        val failure = AtomicReference<IOException>()
        try {
            client.newCall(Request.Builder().url("http://192.168.1.5/").build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    failure.set(e)
                    completed.countDown()
                }

                override fun onResponse(call: Call, response: Response) {
                    response.close()
                    completed.countDown()
                }
            })
            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals("Invalid device host: Grilly Garten.local", failure.get()?.message)
        } finally {
            client.dispatcher.executorService.shutdownNow()
        }
    }

    @Test fun `exceptions from proceed are preserved`() {
        val expected = IllegalStateException("Downstream failure")
        val client = OkHttpClient.Builder()
            .addInterceptor(BaseUrlInterceptor())
            .addInterceptor { throw expected }
            .build()
        val error = assertThrows(IllegalStateException::class.java) {
            client.newCall(Request.Builder().url("http://192.168.1.5/").build()).execute()
        }
        assertSame(expected, error)
    }
}
