package org.battlo.freegrilly.data.device

import javax.inject.Inject
import javax.inject.Singleton
import org.battlo.freegrilly.data.FirmwareVariant

/** The connector selects the concrete firmware adapter exactly once per connection. */
@Singleton
class GrillyDeviceApiHolder @Inject constructor(
    val freeGrilly: FreeGrillyApiAdapter,
    val grillyPlus: GrillyPlusApiAdapter,
    val demo: DemoGrillyDeviceApi,
) {
    @Volatile private var selected: GrillyDeviceApi = freeGrilly
    @Volatile var firmwareVariant: FirmwareVariant = FirmwareVariant.FREE_GRILLY
        private set
    val api: GrillyDeviceApi get() = selected

    fun selectForFirmware(firmware: String?) {
        val plus = firmware.equals("grilly-plus", ignoreCase = true)
        selected = if (plus) grillyPlus else freeGrilly
        firmwareVariant = if (plus) FirmwareVariant.GRILLY_PLUS else FirmwareVariant.FREE_GRILLY
    }

    fun selectDemo() { selected = demo; firmwareVariant = FirmwareVariant.FREE_GRILLY }
}
