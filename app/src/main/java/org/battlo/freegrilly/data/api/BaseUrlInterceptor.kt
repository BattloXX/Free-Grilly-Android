package org.battlo.freegrilly.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlInterceptor @Inject constructor() : Interceptor {
    val currentHost = MutableStateFlow("192.168.200.10")

    override fun intercept(chain: Interceptor.Chain): Response {
        val host = currentHost.value
        val original = chain.request()
        val newUrl = original.url.newBuilder().host(host).build()
        val request = original.newBuilder().url(newUrl).build()

        // The grill only ever speaks plaintext HTTP, so this is the one place that can enforce
        // "LAN only": Android's network security config can't express an IP-range allowlist for
        // a DHCP-assigned address, so the OS-level cleartext permission stays broad. This check
        // is the real gate — it refuses to send cleartext to anything but a private/link-local
        // host, even if currentHost was ever set to something unexpected.
        if (request.url.scheme == "http" && !isPrivateOrLocalHost(request.url.host)) {
            throw IOException("Refusing cleartext HTTP request to non-local host: ${request.url.host}")
        }

        return chain.proceed(request)
    }

    private fun isPrivateOrLocalHost(host: String): Boolean {
        if (host.endsWith(".local")) return true

        val octets = host.split(".").mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        val (a, b, _, _) = octets

        return when {
            a == 10 -> true                       // 10.0.0.0/8
            a == 172 && b in 16..31 -> true        // 172.16.0.0/12
            a == 192 && b == 168 -> true            // 192.168.0.0/16
            a == 169 && b == 254 -> true            // 169.254.0.0/16 (link-local)
            a == 127 -> true                        // loopback (demo/testing)
            else -> false
        }
    }
}
