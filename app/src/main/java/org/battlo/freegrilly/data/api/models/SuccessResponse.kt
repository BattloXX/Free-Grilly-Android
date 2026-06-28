package org.battlo.freegrilly.data.api.models
import kotlinx.serialization.Serializable

@Serializable
data class SuccessResponse(
    val success: Boolean,
    val message: String? = null
)
