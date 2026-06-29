package org.battlo.freegrilly.data.api
import okhttp3.MultipartBody
import org.battlo.freegrilly.data.api.models.*
import retrofit2.http.*

interface GrillyApiService {
    @GET("/api/grill")
    suspend fun getGrillStatus(): GrillStatusResponse

    @GET("/api/probes")
    suspend fun getProbes(): List<ProbeConfig>

    @POST("/api/probes")
    suspend fun updateProbes(@Body probes: List<ProbeConfig>): SuccessResponse

    @GET("/api/settings")
    suspend fun getSettings(): DeviceSettings

    @POST("/api/settings")
    suspend fun updateSettings(@Body settings: DeviceSettings): SuccessResponse

    @GET("/api/probes/history")
    suspend fun getHistory(): HistoryResponse

    @POST("/api/alarm/mute")
    suspend fun muteAlarm(@Body body: Map<String, String> = emptyMap()): SuccessResponse

    @GET("/api/info")
    suspend fun getInfo(): DeviceInfo

    @GET("/api/wifiscan")
    suspend fun getWifiNetworks(): List<WifiNetwork>

    /**
     * §8 — OTA firmware upload (ElegantOTA-compatible, PUT /update).
     * Only called when the device reports the `ota` capability.
     * The multipart part must be named "firmware" and contain the .bin file bytes.
     */
    @Multipart
    @PUT("/update")
    suspend fun uploadFirmware(@Part firmware: MultipartBody.Part): SuccessResponse
}
