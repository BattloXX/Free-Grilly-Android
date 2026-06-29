package org.battlo.freegrilly.data.update

import org.battlo.freegrilly.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateChecker @Inject constructor(private val api: GitHubApiService) {

    companion object {
        const val GITHUB_OWNER = "BattloXX"
        const val GITHUB_REPO = "Free-Grilly-Android"
    }

    /** Returns [UpdateInfo] if a newer release exists, null otherwise. */
    suspend fun checkForUpdate(): UpdateInfo? {
        val release = runCatching {
            api.getLatestRelease(GITHUB_OWNER, GITHUB_REPO)
        }.getOrNull() ?: return null

        if (release.draft || release.prerelease) return null

        val remoteVersion = release.tagName.trimStart('v')
        if (!isNewer(remote = remoteVersion, local = BuildConfig.VERSION_NAME)) return null

        val apkAsset = release.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true)
        } ?: return null

        return UpdateInfo(
            version = remoteVersion,
            releaseNotes = release.body,
            downloadUrl = apkAsset.downloadUrl,
            assetSizeBytes = apkAsset.size,
            releasePageUrl = release.htmlUrl,
        )
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = semver(remote)
        val l = semver(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    private fun semver(v: String) = v.split('.').map { it.toIntOrNull() ?: 0 }
}
