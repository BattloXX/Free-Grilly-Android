package org.battlo.freegrilly.data.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object DeviceHost {
    fun normalize(input: String): String? {
        var host = input.trim()
        if (host.startsWith("http://", ignoreCase = true)) host = host.substring(7)
        else if (host.startsWith("https://", ignoreCase = true)) host = host.substring(8)
        host = host.substringBefore('/').replace(',', '.')
        if (host.contains(':')) {
            if (host.substringAfter(':') != "80") return null
            host = host.substringBefore(':')
        }
        host = host.removeSuffix(".").lowercase()
        if (host.isEmpty() || host.any { it.isWhitespace() }) return null
        return host.takeIf { "http://$it/".toHttpUrlOrNull()?.host == it }
    }

    fun isValid(host: String): Boolean = normalize(host) == host
}
