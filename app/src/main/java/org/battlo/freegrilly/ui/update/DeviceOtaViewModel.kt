package org.battlo.freegrilly.ui.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.battlo.freegrilly.data.Capabilities
import org.battlo.freegrilly.data.GrillyRepository
import org.battlo.freegrilly.data.device.GrillyDeviceApiHolder
import org.battlo.freegrilly.data.device.GrillyPlusApiAdapter
import org.battlo.freegrilly.data.hasFlag
import org.battlo.freegrilly.data.security.OtaPasswordStore
import org.battlo.freegrilly.data.update.DeviceFirmwareChecker
import org.battlo.freegrilly.data.update.DeviceFirmwareInfo
import java.io.File
import javax.inject.Inject

/**
 * §8 — OTA firmware update for the physical grill device.
 *
 * State machine:
 *   Idle → Checking → UpToDate / Available → Downloading → ReadyToUpload → Uploading → Done / Error
 *
 * Capabilities gate: the UI only shows this when [GrillyRepository.capabilitiesFlow]
 * contains [Capabilities.OTA]. The ViewModel never calls the device if that flag is absent.
 */
@HiltViewModel
class DeviceOtaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firmwareChecker: DeviceFirmwareChecker,
    private val deviceApiHolder: GrillyDeviceApiHolder,
    private val repository: GrillyRepository,
    private val otaPasswordStore: OtaPasswordStore,
) : ViewModel() {

    sealed interface OtaState {
        object Idle : OtaState
        object Checking : OtaState
        object UpToDate : OtaState
        data class Available(val info: DeviceFirmwareInfo) : OtaState
        data class Downloading(val progress: Int, val info: DeviceFirmwareInfo) : OtaState
        data class ReadyToUpload(val file: File, val info: DeviceFirmwareInfo) : OtaState
        /** Password never belongs to this state; it stays in the composable input only. */
        class NeedsPassword(val file: File, val info: DeviceFirmwareInfo, val wrong: Boolean) : OtaState {
            override fun toString() = "NeedsPassword(wrong=$wrong)"
        }
        data class Uploading(val progress: Int, val info: DeviceFirmwareInfo) : OtaState
        data class Done(val info: DeviceFirmwareInfo) : OtaState
        data class Error(val message: String) : OtaState
    }

    private val _state = MutableStateFlow<OtaState>(OtaState.Idle)
    val state: StateFlow<OtaState> = _state.asStateFlow()

    val supportsOta: StateFlow<Boolean> = repository.capabilitiesFlow
        .map { it.hasFlag(Capabilities.OTA) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun checkForFirmwareUpdate() {
        if (!repository.activeCapabilities.hasFlag(Capabilities.OTA)) return
        _state.value = OtaState.Checking
        viewModelScope.launch {
            val info = runCatching { firmwareChecker.checkForFirmwareUpdate() }.getOrNull()
            _state.value = when {
                info != null -> OtaState.Available(info)
                else -> OtaState.UpToDate
            }
        }
    }

    fun downloadFirmware(info: DeviceFirmwareInfo) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val destFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "firmware-${info.version}.bin",
        )
        if (destFile.exists()) destFile.delete()

        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("Firmware ${info.version}")
            .setDescription("Free-Grilly Firmware-Update")
            .setDestinationUri(Uri.fromFile(destFile))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val downloadId = dm.enqueue(request)
        _state.value = OtaState.Downloading(0, info)

        viewModelScope.launch {
            pollDownload(dm, downloadId, info, destFile)
        }
    }

    private suspend fun pollDownload(
        dm: DownloadManager,
        downloadId: Long,
        info: DeviceFirmwareInfo,
        destFile: File,
    ) {
        while (true) {
            val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
            if (cursor == null || !cursor.moveToFirst()) {
                delay(500)
                continue
            }
            val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val downloadedCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

            val status = cursor.getInt(statusCol)
            val downloaded = cursor.getLong(downloadedCol)
            val total = cursor.getLong(totalCol)
            cursor.close()

            when (status) {
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                    val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    _state.value = OtaState.Downloading(progress, info)
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    _state.value = OtaState.ReadyToUpload(destFile, info)
                    return
                }
                DownloadManager.STATUS_FAILED -> {
                    _state.value = OtaState.Error("Firmware-Download fehlgeschlagen.")
                    return
                }
            }
            delay(500)
        }
    }

    fun beginUpload(file: File, info: DeviceFirmwareInfo) {
        viewModelScope.launch {
            // ElegantOTA / Free-Grilly remains its existing unauthenticated PUT flow.
            if (deviceApiHolder.api !is GrillyPlusApiAdapter) {
                uploadFirmware(file, info, "", usedStoredPassword = false)
                return@launch
            }
            val uuid = selectedDeviceUuid() ?: run {
                uploadFirmware(file, info, "", usedStoredPassword = false)
                return@launch
            }
            val stored = otaPasswordStore.load(uuid)
            when (nextOtaPasswordAction(
                repository.activeCapabilities.hasFlag(Capabilities.OTA_AUTH), stored != null,
            )) {
                OtaPasswordAction.ASK_FOR_PASSWORD -> _state.value = OtaState.NeedsPassword(file, info, wrong = false)
                OtaPasswordAction.UPLOAD_WITH_STORED_PASSWORD -> uploadFirmware(file, info, stored.orEmpty(), true)
                OtaPasswordAction.UPLOAD_WITHOUT_PASSWORD -> uploadFirmware(file, info, "", false)
            }
        }
    }

    fun submitPassword(file: File, info: DeviceFirmwareInfo, password: String, remember: Boolean) {
        viewModelScope.launch {
            val uuid = selectedDeviceUuid()
            if (remember && uuid != null) runCatching { otaPasswordStore.save(uuid, password) }
            uploadFirmware(file, info, password, usedStoredPassword = false)
        }
    }

    fun forgetRememberedPassword() {
        viewModelScope.launch { selectedDeviceUuid()?.let { otaPasswordStore.delete(it) } }
    }

    private suspend fun selectedDeviceUuid(): String? = repository.selectedDeviceUuid()

    private fun uploadFirmware(file: File, info: DeviceFirmwareInfo, adminPassword: String, usedStoredPassword: Boolean) {
        _state.value = OtaState.Uploading(0, info)
        viewModelScope.launch {
            var password = adminPassword
            runCatching {
                deviceApiHolder.api.uploadFirmware(file, password)
            }.onSuccess {
                _state.value = OtaState.Done(info)
                file.delete()
            }.onFailure { e ->
                val code = (e as? retrofit2.HttpException)?.code()
                if (deviceApiHolder.api is GrillyPlusApiAdapter && code == 401) {
                    // A password saved before a failed manual attempt is stale too.
                    if (shouldDiscardStoredPassword(code, usedStoredPassword) || !usedStoredPassword) {
                        selectedDeviceUuid()?.let { otaPasswordStore.delete(it) }
                    }
                    _state.value = OtaState.NeedsPassword(file, info, wrong = true)
                } else {
                    _state.value = OtaState.Error("Upload fehlgeschlagen.")
                }
            }.also {
                // Do not retain a secret in a ViewModel field after this request completes.
                password = ""
            }
        }
    }

    fun dismissError() {
        _state.value = OtaState.Idle
    }

    fun cancelPasswordPrompt(file: File, info: DeviceFirmwareInfo) {
        _state.value = OtaState.ReadyToUpload(file, info)
    }
}
