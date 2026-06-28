package org.battlo.freegrilly.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.battlo.freegrilly.data.DeviceStore
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val deviceStore: DeviceStore,
) : ViewModel() {

    val startDestination = deviceStore.selectedDeviceUuid
        .map { uuid -> if (uuid != null) "dashboard" else "onboarding" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "onboarding")
}
