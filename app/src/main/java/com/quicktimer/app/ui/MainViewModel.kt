package com.quicktimer.app.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicktimer.app.data.TimerEntity
import com.quicktimer.app.data.TimerRepository
import com.quicktimer.app.service.VolumeAccessibilityService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: TimerRepository) : ViewModel() {

    val allTimers: StateFlow<List<TimerEntity>> = repository.allTimers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTimers: StateFlow<List<TimerEntity>> = repository.favoriteTimers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    fun checkAccessibilityService(context: Context) {
        val expectedComponentName = "${context.packageName}/${VolumeAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val isEnabled = enabledServices.split(":").any { it.equals(expectedComponentName, ignoreCase = true) }
        _isAccessibilityEnabled.value = isEnabled || VolumeAccessibilityService.isServiceRunning
    }

    fun addTimer(name: String, minutes: Int, seconds: Int) {
        val totalSeconds = (minutes * 60) + seconds
        if (totalSeconds <= 0) {
            viewModelScope.launch { _userMessage.emit("La duración debe ser mayor a 0") }
            return
        }
        val cleanName = name.trim().ifEmpty { "Temporizador ${minutes}m" }
        viewModelScope.launch {
            val newTimer = TimerEntity(
                name = cleanName,
                durationSeconds = totalSeconds,
                isFavorite = false
            )
            repository.insert(newTimer)
        }
    }

    fun updateTimer(timer: TimerEntity, name: String, minutes: Int, seconds: Int) {
        val totalSeconds = (minutes * 60) + seconds
        if (totalSeconds <= 0) {
            viewModelScope.launch { _userMessage.emit("La duración debe ser mayor a 0") }
            return
        }
        val cleanName = name.trim().ifEmpty { timer.name }
        viewModelScope.launch {
            repository.update(
                timer.copy(
                    name = cleanName,
                    durationSeconds = totalSeconds
                )
            )
        }
    }

    fun deleteTimer(timer: TimerEntity) {
        viewModelScope.launch {
            repository.delete(timer)
        }
    }

    fun toggleFavorite(timer: TimerEntity) {
        viewModelScope.launch {
            val success = repository.toggleFavorite(timer)
            if (!success) {
                _userMessage.emit("Máximo 3 favoritos permitidos para el menú de acceso rápido")
            }
        }
    }
}

class MainViewModelFactory(private val repository: TimerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
