package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProxyRepository
import com.example.data.db.AppDatabase
import com.example.data.db.ProxyKeyEntity
import com.example.ui.theme.ThemeMode
import com.example.util.AppPreferences
import com.example.util.ExpiryNotificationReceiver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
    data class ConfigFetched(val config: String, val format: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProxyRepository

    val rawKeys: StateFlow<List<ProxyKeyEntity>>
    val keys: StateFlow<List<ProxyKeyEntity>> get() = rawKeys

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _themeMode = MutableStateFlow(
        try {
            val saved = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(saved)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _colorTheme = MutableStateFlow(AppPreferences.getColorTheme(application))
    val colorTheme: StateFlow<com.example.ui.theme.AppColorTheme> = _colorTheme.asStateFlow()

    private val _baseDomain = MutableStateFlow(AppPreferences.getBaseDomain(application))
    val baseDomain: StateFlow<String> = _baseDomain.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(AppPreferences.areNotificationsEnabled(application))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _deviceLayoutMode = MutableStateFlow(AppPreferences.getDeviceLayoutMode(application))
    val deviceLayoutMode: StateFlow<com.example.util.DeviceLayoutMode> = _deviceLayoutMode.asStateFlow()

    private val _filter = MutableStateFlow(KeyFilter.ALL)
    val filter: StateFlow<KeyFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(KeySort.NEWEST)
    val sort: StateFlow<KeySort> = _sort.asStateFlow()

    val filteredKeys: StateFlow<List<ProxyKeyEntity>>

    init {
        val dao = AppDatabase.getDatabase(application).proxyKeyDao()
        repository = ProxyRepository(dao)
        rawKeys = repository.allKeys.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        filteredKeys = combine(rawKeys, _filter, _sort) { keysList, currentFilter, currentSort ->
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val filtered = when (currentFilter) {
                KeyFilter.ALL -> keysList
                KeyFilter.ACTIVE_ONLY -> keysList.filter { key ->
                    if (key.expiresAt == null) true
                    else {
                        try {
                            val parsed = dateFormat.parse(key.expiresAt)
                            parsed != null && parsed.time > now
                        } catch (_: Exception) {
                            true
                        }
                    }
                }
            }

            when (currentSort) {
                KeySort.NEWEST -> filtered // default from DB order
                KeySort.TIME_LEFT_ASC -> filtered.sortedBy { key ->
                    if (key.expiresAt == null) Long.MAX_VALUE
                    else {
                        try {
                            dateFormat.parse(key.expiresAt)?.time ?: Long.MAX_VALUE
                        } catch (_: Exception) {
                            Long.MAX_VALUE
                        }
                    }
                }
                KeySort.CABINET -> filtered.sortedBy { key ->
                    key.cabinetNo?.toIntOrNull() ?: Int.MAX_VALUE
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().putString("theme_mode", mode.name).apply()
    }

    fun setColorTheme(palette: com.example.ui.theme.AppColorTheme) {
        val app = getApplication<Application>()
        AppPreferences.setColorTheme(app, palette)
        _colorTheme.value = palette
    }

    fun setBaseDomain(domain: String) {
        val app = getApplication<Application>()
        AppPreferences.setBaseDomain(app, domain)
        _baseDomain.value = AppPreferences.getBaseDomain(app)
    }

    fun resetBaseDomain() {
        val app = getApplication<Application>()
        AppPreferences.resetBaseDomain(app)
        _baseDomain.value = AppPreferences.getBaseDomain(app)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        AppPreferences.setNotificationsEnabled(app, enabled)
        _notificationsEnabled.value = enabled
    }

    fun setDeviceLayoutMode(mode: com.example.util.DeviceLayoutMode) {
        val app = getApplication<Application>()
        AppPreferences.setDeviceLayoutMode(app, mode)
        _deviceLayoutMode.value = mode
    }

    fun setFilter(filter: KeyFilter) {
        _filter.value = filter
    }

    fun setSort(sort: KeySort) {
        _sort.value = sort
    }

    fun generateKey() {
        generateMultipleKeys(1)
    }

    fun generateMultipleKeys(count: Int) {
        if (count <= 0) return
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            var successCount = 0
            var lastError: String? = null
            val currentDomain = _baseDomain.value

            for (i in 1..count) {
                val result = repository.generateNewKey(currentDomain)
                if (result.isSuccess) {
                    successCount++
                    val entity = result.getOrNull()
                    if (entity != null) {
                        ExpiryNotificationReceiver.scheduleExpiryReminder(
                            getApplication(),
                            entity.id,
                            entity.expiresAt
                        )
                    }
                } else {
                    lastError = result.exceptionOrNull()?.message
                }
            }

            if (successCount == count) {
                val msg = if (count == 1) "Подписка создана успешно" else "Создано подписок: $count"
                _uiState.update { UiState.Success(msg) }
            } else if (successCount > 0) {
                _uiState.update { UiState.Success("Создано $successCount из $count (Ошибка: ${lastError ?: "Неизвестно"})") }
            } else {
                _uiState.update { UiState.Error(lastError ?: "Не удалось создать подписку") }
            }
        }
    }

    fun addManualKey(key: String) {
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            val result = repository.addManualKey(key, _baseDomain.value)
            if (result.isSuccess) {
                val entity = result.getOrNull()
                if (entity != null) {
                    ExpiryNotificationReceiver.scheduleExpiryReminder(
                        getApplication(),
                        entity.id,
                        entity.expiresAt
                    )
                }
                _uiState.update { UiState.Success("Подписка успешно добавлена") }
            } else {
                _uiState.update { UiState.Error(result.exceptionOrNull()?.message ?: "Не удалось добавить подписку") }
            }
        }
    }

    fun refreshKey(keyId: String) {
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            val result = repository.refreshKey(keyId, _baseDomain.value)
            if (result.isSuccess) {
                val entity = result.getOrNull()
                if (entity != null) {
                    ExpiryNotificationReceiver.scheduleExpiryReminder(
                        getApplication(),
                        entity.id,
                        entity.expiresAt
                    )
                }
                _uiState.update { UiState.Success("Статус подписки обновлен") }
            } else {
                _uiState.update { UiState.Error(result.exceptionOrNull()?.message ?: "Не удалось обновить статус") }
            }
        }
    }

    fun refreshAllKeys() {
        viewModelScope.launch {
            val list = rawKeys.value
            if (list.isEmpty()) {
                _uiState.update { UiState.Error("Список подписок пуст") }
                return@launch
            }
            _uiState.update { UiState.Loading }
            var refreshed = 0
            for (key in list) {
                val res = repository.refreshKey(key.id, _baseDomain.value)
                if (res.isSuccess) {
                    refreshed++
                    res.getOrNull()?.let {
                        ExpiryNotificationReceiver.scheduleExpiryReminder(getApplication(), it.id, it.expiresAt)
                    }
                }
            }
            _uiState.update { UiState.Success("Обновлено подписок: $refreshed из ${list.size}") }
        }
    }

    fun deleteKey(key: String) {
        viewModelScope.launch {
            ExpiryNotificationReceiver.cancelExpiryReminder(getApplication(), key)
            repository.deleteKey(key)
        }
    }

    fun fetchConfig(url: String, format: String) {
        viewModelScope.launch {
            _uiState.update { UiState.Loading }
            val config = repository.fetchConfig(url, format)
            if (config != null) {
                _uiState.update { UiState.ConfigFetched(config, format) }
            } else {
                _uiState.update { UiState.Error("Не удалось получить конфигурацию") }
            }
        }
    }

    fun resetState() {
        _uiState.update { UiState.Idle }
    }
}
