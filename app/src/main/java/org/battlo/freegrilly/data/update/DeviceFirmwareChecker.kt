package org.battlo.freegrilly.data.update

import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.FirmwareVariant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * §8 — Checks for firmware updates for the physical grill device.
 *
 * Fetches the latest release from [BattloXX/Free-Grilly][GITHUB_OWNER]/[GITHUB_REPO].
 * Returns [DeviceFirmwareInfo] if a newer firmware is available (compared to the device's
 * current firmware version from /api/info), or null if already up-to-date or on error.
 *
 * Only called when the device reports the `ota` capability.
 * Reuses [GitHubApiService] (same client pointed at api.github.com, separate from device Retrofit).
 */
@Singleton
class DeviceFirmwareChecker @Inject constructor(
    private val api: GitHubApiService,
    private val repository: GrillyRepository,
) {

    /**
     * Checks the latest firmware release against the device's current firmware version.
     * @return [DeviceFirmwareInfo] if an update is available, null otherwise.
     */
    suspend fun checkForFirmwareUpdate(): DeviceFirmwareInfo? {
        val isPlus = repository.firmwareVariant == FirmwareVariant.GRILLY_PLUS
        val release = runCatching {
            api.getLatestRelease(if (isPlus) GRILLY_PLUS_OWNER else GITHUB_OWNER, if (isPlus) GRILLY_PLUS_REPO else GITHUB_REPO)
        }.getOrNull() ?: return null

        if (release.draft || release.prerelease) return null

        val remoteVersion = release.tagName.trimStart('v')

        // Get the current firmware version from the connected device
        val deviceInfo = repository.getDeviceInfo() ?: return null
        val localVersion = deviceInfo.resolvedFirmwareVersion.trimStart('v')

        if (!isNewer(remote = remoteVersion, local = localVersion)) return null

        // Find the .bin asset (ElegantOTA firmware binary)
        val binAsset = selectFirmwareAsset(release.assets, isPlus) ?: return null

        return DeviceFirmwareInfo(
            version = remoteVersion,
            releaseNotes = release.body,
            downloadUrl = binAsset.downloadUrl,
            assetSizeBytes = binAsset.size,
            releasePageUrl = release.htmlUrl,
            currentDeviceVersion = localVersion,
        )
    }

    internal fun isNewer(remote: String, local: String): Boolean {
        val r = semver(remote)
        val l = semver(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }

    private fun semver(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }

    internal fun selectFirmwareAsset(assets: List<GitHubAsset>, grillyPlus: Boolean): GitHubAsset? =
        assets.firstOrNull { if (grillyPlus) it.name.endsWith("-ota.bin", true) else it.name.endsWith(".bin", true) }

    companion object {
        const val GITHUB_OWNER = "BattloXX"
        const val GITHUB_REPO = "Free-Grilly"
        const val GRILLY_PLUS_OWNER = "bardesss"
        const val GRILLY_PLUS_REPO = "grilly-plus"
    }
}

/** Firmware update info for the physical device (distinct from [UpdateInfo] for the APK). */
data class DeviceFirmwareInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val assetSizeBytes: Long,
    val releasePageUrl: String = "",
    val currentDeviceVersion: String = "",
)
