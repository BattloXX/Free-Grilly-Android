package org.battlo.freegrilly.ui.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.battlo.freegrilly.BuildConfig
import org.battlo.freegrilly.data.update.UpdateChecker
import org.battlo.freegrilly.data.update.UpdateInfo
import javax.inject.Inject

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val progress: Int, val info: UpdateInfo) : UpdateState
    data class ReadyToInstall(val downloadId: Long, val info: UpdateInfo) : UpdateState
    data class Error(val message: String) : UpdateState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val checker: UpdateChecker,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    fun checkForUpdate() {
        if (_state.value is UpdateState.Checking) return
        viewModelScope.launch {
            _state.value = UpdateState.Checking
            val info = checker.checkForUpdate()
            _state.value = if (info != null) UpdateState.Available(info) else UpdateState.UpToDate
        }
    }

    fun startDownload(info: UpdateInfo) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val filename = "free-grilly-${info.version}.apk"
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("Free-Grilly ${info.version}")
            .setDescription("Update wird heruntergeladen…")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename)
        val downloadId = dm.enqueue(request)
        _state.value = UpdateState.Downloading(0, info)
        pollDownload(dm, downloadId, info)
    }

    private fun pollDownload(dm: DownloadManager, downloadId: Long, info: UpdateInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (!cursor.moveToFirst()) { cursor.close(); delay(500); continue }

                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                cursor.close()

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        _state.value = UpdateState.ReadyToInstall(downloadId, info)
                        break
                    }
                    DownloadManager.STATUS_FAILED -> {
                        _state.value = UpdateState.Error("Download fehlgeschlagen.")
                        break
                    }
                    else -> {
                        val pct = if (total > 0) (done * 100 / total).toInt() else 0
                        _state.value = UpdateState.Downloading(pct, info)
                    }
                }
                delay(500)
            }
        }
    }

    fun installUpdate(downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = dm.getUriForDownloadedFile(downloadId)
        if (uri == null) {
            _state.value = UpdateState.Error("APK nicht gefunden.")
            return
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        runCatching { context.startActivity(intent) }.onFailure {
            _state.value = UpdateState.Error("Installation konnte nicht gestartet werden: ${it.message}")
        }
    }

    fun dismiss() { _state.value = UpdateState.Idle }
    fun dismissError() { _state.value = UpdateState.Idle }
}
