package org.battlo.freegrilly

import kotlinx.coroutines.runBlocking
import org.battlo.freegrilly.data.Capabilities
import org.battlo.freegrilly.data.api.FakeGrillyApi
import org.battlo.freegrilly.data.device.DemoGrillyDeviceApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAdapterTest {
    @Test fun `demo adapter maps legacy dto into neutral state`() = runBlocking {
        val state = DemoGrillyDeviceApi(FakeGrillyApi()).status()
        assertEquals("Demo Griller", state.name)
        assertEquals(1, state.probes.first().id)
        assertTrue(state.probes.first().connected)
    }

    @Test fun `plus fixture keeps nullable temperature`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; explicitNulls = false }
        val dto = json.decodeFromString<org.battlo.freegrilly.data.api.models.GrillStatusResponse>(fixture("grilly-plus-grill.json"))
        assertNull(dto.probes.single().temperature)
        assertNull(dto.resolvedCookSessionId)
    }

    @Test fun `grill cook session accepts object and scalar forms`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; explicitNulls = false }
        val objectForm = json.decodeFromString<org.battlo.freegrilly.data.api.models.GrillStatusResponse>(
            fixture("grilly-plus-grill-cook-session.json")
        )
        val scalarForm = json.decodeFromString<org.battlo.freegrilly.data.api.models.GrillStatusResponse>(
            "{\"cook_session_id\":\"cook-43\"}"
        )
        assertEquals("cook-42", objectForm.resolvedCookSessionId)
        assertEquals("cook-43", scalarForm.resolvedCookSessionId)
    }

    @Test fun `plus info fixture identifies neutral firmware metadata`() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; explicitNulls = false }
        val dto = json.decodeFromString<org.battlo.freegrilly.data.api.models.DeviceInfo>(fixture("grilly-plus-info-v2.json"))
        assertEquals("grilly-plus", dto.firmware)
        assertEquals("plus-1", dto.resolvedUuid)
    }

    @Test fun `capability aliases are normalized`() {
        val caps = Capabilities.normalize(listOf("sse", "probe_calibration", "ota_upload"))
        assertTrue(Capabilities.EVENTS in caps)
        assertTrue(Capabilities.CALIBRATION_OFFSET in caps)
        assertTrue(Capabilities.OTA in caps)
    }

    private fun fixture(name: String) = requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")).readText()
}
