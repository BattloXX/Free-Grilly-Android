package org.battlo.freegrilly.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Runtime-permission helpers.
 *
 * The device is discovered via mDNS/NSD over Wi-Fi. On Android 13+ (API 33) an app that
 * targets API 33+ must hold [Manifest.permission.NEARBY_WIFI_DEVICES] for
 * `NsdManager.discoverServices(...)` to return any results — without it discovery silently
 * finds nothing, which is fatal whenever the device's DHCP IP has changed. Below API 33 the
 * NSD APIs need no runtime permission (the `neverForLocation` flag avoids the location
 * requirement on 33+), so these helpers no-op there.
 */
object Permissions {

    /** The Nearby-Wi-Fi runtime permission on API 33+, or null on older versions. */
    val nearbyWifi: String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.NEARBY_WIFI_DEVICES
        else null

    /** True when NSD discovery is allowed (granted, or not required on this OS version). */
    fun hasNearbyWifi(context: Context): Boolean {
        val perm = nearbyWifi ?: return true
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Dangerous permissions to request at app start. On API 33+ this is Nearby-Wi-Fi
     * (for NSD) and Post-Notifications (for background alarm alerts); empty below 33.
     */
    fun startupRequests(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        else emptyArray()
}
