package org.battlo.freegrilly

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.battlo.freegrilly.data.Capabilities
import org.battlo.freegrilly.data.hasFlag
import org.battlo.freegrilly.data.api.FakeGrillyApi
import org.battlo.freegrilly.data.api.GrillyApiService
import org.battlo.freegrilly.data.api.models.DeviceInfo
import org.battlo.freegrilly.data.api.models.GrillStatusResponse
import org.battlo.freegrilly.data.device.GrillyPlusApiAdapter
import org.battlo.freegrilly.data.api.models.GrillyPlusHistoryTier
import org.battlo.freegrilly.data.history.GrillyPlusHistoryMapper
import org.junit.Assert.*
import org.junit.Test

class GrillyPlusCompatibilityTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test fun `parses Free-Grilly fixture`() {
        val info = json.decodeFromString<DeviceInfo>(fixture("free-grilly-info.json"))
        val grill = json.decodeFromString<GrillStatusResponse>("""{"uuid":"free-1","alarm_active":true,"probes":[{"id":1,"temperature":64.5,"target_temperature":70}]}""")
        assertEquals("free-1", info.resolvedUuid); assertEquals("26.09.1", info.resolvedFirmwareVersion)
        assertTrue(grill.isAlarmSounding); assertEquals(1, grill.probes.single().resolvedId)
    }

    @Test fun `parses Grilly Plus fixture including nullable temperatures`() {
        val info = json.decodeFromString<DeviceInfo>(fixture("grilly-plus-info.json"))
        val grill = json.decodeFromString<GrillStatusResponse>("""{"unique_id":"plus-1","alarm_sounding":true,"probes":[{"probe_id":2,"temperature":null,"target_temperature":null}]}""")
        assertEquals("plus-1", info.resolvedUuid); assertEquals("26.09.28.2", info.resolvedFirmwareVersion)
        assertTrue(info.isGrillyPlus); assertTrue(grill.isAlarmSounding); assertEquals(2, grill.probes.single().resolvedId); assertEquals(0f, grill.probes.single().resolvedTemperature)
    }

    @Test fun `Grilly Plus history mapper calculates timestamps and skips gaps`() {
        val samples = GrillyPlusHistoryMapper.samples(3, GrillyPlusHistoryTier(interval = 10, age = 20, values = listOf(100, null, 120)), 100_000)
        assertEquals(2, samples.size); assertEquals(60_000, samples[0].tsMs); assertEquals(80_000, samples[1].tsMs)
    }

    @Test fun `capability aliases normalize ota upload`() {
        val caps = Capabilities.normalize(listOf("ota_upload", "clear_history", "diagnostics"))
        assertTrue(caps.hasFlag(Capabilities.OTA)); assertTrue(caps.hasFlag(Capabilities.CLEAR_HISTORY)); assertFalse(caps.hasFlag(Capabilities.EVENTS))
    }

    @Test fun `Grilly Plus v3 capabilities and status map through adapter`() = runBlocking {
        val info = json.decodeFromString<DeviceInfo>(fixture("grilly-plus-info-v3.json"))
        val capabilities = Capabilities.normalize(info.capabilities)
        assertTrue(capabilities.hasFlag(Capabilities.ALARM_PROBE_MUTE))
        assertTrue(capabilities.hasFlag(Capabilities.OTA_AUTH))
        assertTrue(capabilities.hasFlag(Capabilities.COOK_SESSION))
        assertFalse(capabilities.hasFlag(Capabilities.EVENTS))

        val grill = json.decodeFromString<GrillStatusResponse>(fixture("grilly-plus-grill-cook-session.json"))
        val adapter = GrillyPlusApiAdapter(object : GrillyApiService by FakeGrillyApi() {
            override suspend fun getGrillStatus() = grill
        })
        val status = adapter.status()
        assertEquals(7_500L, status.diagnostics.uptimeSeconds)
        assertEquals("c-1a2b3c4d-0001", status.cookSessionId)
    }

    @Test fun `Grilly Plus selects ota asset never full image`() {
        val assets = listOf("grilly-plus-26.09.28.2-full.bin", "grilly-plus-26.09.28.2-ota.bin")
        assertEquals("grilly-plus-26.09.28.2-ota.bin", assets.first { it.endsWith("-ota.bin") })
    }

    @Test fun `CalVer compares all four segments and ignores prerelease suffix`() {
        fun newer(remote: String, local: String): Boolean {
            val r = remote.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }; val l = local.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            return (0 until maxOf(r.size, l.size)).firstOrNull { r.getOrElse(it) { 0 } != l.getOrElse(it) { 0 } }?.let { r.getOrElse(it) { 0 } > l.getOrElse(it) { 0 } } ?: false
        }
        assertTrue(newer("26.09.28.2", "26.09.28.1")); assertFalse(newer("27.08.25-beta", "27.08.25"))
    }

    private fun fixture(name: String): String = requireNotNull(javaClass.classLoader?.getResource("fixtures/$name"))
        .readText()
}
