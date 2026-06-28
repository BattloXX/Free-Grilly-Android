package org.battlo.freegrilly.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaseUrlInterceptor @Inject constructor() : Interceptor {
    val currentHost = MutableStateFlow("192.168.200.10")

    override fun intercept(chain: Interceptor.Chain): Response {
        val host = currentHost.value
        val original = chain.request()
        val newUrl = original.url.newBuilder().host(host).build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
