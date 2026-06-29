package org.battlo.freegrilly.data.update

data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val assetSizeBytes: Long,
    val releasePageUrl: String = "",
)
