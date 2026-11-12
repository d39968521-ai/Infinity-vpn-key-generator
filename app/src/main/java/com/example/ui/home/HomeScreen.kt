package com.example.ui.home

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ProxyKeyEntity
import com.example.ui.KeyFilter
import com.example.ui.KeySort
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.ThemeMode
import com.example.util.AppPreferences
import com.example.util.DateUtil
import com.example.util.DeviceLayoutMode
import com.example.util.rememberQrPainter
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val keys by viewModel.filteredKeys.collectAsStateWithLifecycle()
    val allKeys by viewModel.rawKeys.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
    val currentSort by viewModel.sort.collectAsStateWithLifecycle()
    val currentDomain by viewModel.baseDomain.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val currentThemeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentColorTheme by viewModel.colorTheme.collectAsStateWithLifecycle()
    val currentDeviceMode by viewModel.deviceLayoutMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showManualKeyDialog by remember { mutableStateOf(false) }
    var showBatchGenerateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var batchCount by remember { mutableStateOf(1) }
    var manualKeyInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar((uiState as UiState.Success).message)
                viewModel.resetState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar((uiState as UiState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Straus Top Подписки") },
                actions = {
                    IconButton(onClick = { showBatchGenerateDialog = true }) {
                        Icon(Icons.Default.Layers, contentDescription = "Создать подписки")
                    }
                    IconButton(onClick = { showManualKeyDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить вручную")
                    }
                    IconButton(onClick = {
                        val allKeysText = allKeys.joinToString(separator = "\n\n") {
                            "Подписка ID: ${it.id}\nКабинет: ${it.cabinetNo ?: "N/A"}\nСсылка подписки: ${it.subUrl ?: "N/A"}\nИстекает: ${it.expiresAt ?: "N/A"}"
                        }
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, allKeysText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться всеми")
                    }
                    IconButton(onClick = { viewModel.refreshAllKeys() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить статус подписок")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBatchGenerateDialog = true },
                modifier = Modifier.testTag("generate_button")
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Создать подписку")
                }
            }
        }
    ) { padding ->
        // Settings & Mirror Domain Dialog
        if (showSettingsDialog) {
            var inputDomain by remember { mutableStateOf(currentDomain) }

            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Настройки")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Theme Mode (Light / Dark / Auto)
                        Text("Режим оформления", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = currentThemeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                icon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            ) {
                                Text("Светлая")
                            }
                            SegmentedButton(
                                selected = currentThemeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                icon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            ) {
                                Text("Тёмная")
                            }
                            SegmentedButton(
                                selected = currentThemeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                icon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            ) {
                                Text("Авто")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Color Palette (Голубая / Deep Ocean / Emerald / Neon / Purple / Dynamic)
                        Text("Цветовая палитра", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Выберите акцентный стиль интерфейса (голубой, океан, неон и др.):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val themes = listOf(
                            Triple(AppColorTheme.SKY_BLUE, "Голубая (Sky Blue)", androidx.compose.ui.graphics.Color(0xFF0288D1)),
                            Triple(AppColorTheme.DEEP_OCEAN, "Глубокий океан", androidx.compose.ui.graphics.Color(0xFF1565C0)),
                            Triple(AppColorTheme.CYBER_NEON, "Кибер Неон", androidx.compose.ui.graphics.Color(0xFF00E5FF)),
                            Triple(AppColorTheme.EMERALD, "Изумрудная бирюза", androidx.compose.ui.graphics.Color(0xFF00897B)),
                            Triple(AppColorTheme.PURPLE, "Фиолетовая классика", androidx.compose.ui.graphics.Color(0xFF6650a4)),
                            Triple(AppColorTheme.DYNAMIC, "Material You (Обои)", androidx.compose.ui.graphics.Color(0xFF546E7A))
                        )

                        themes.forEach { (themeItem, title, dotColor) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { viewModel.setColorTheme(themeItem) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentColorTheme == themeItem) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                border = if (currentColorTheme == themeItem) {
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        color = dotColor,
                                        modifier = Modifier.size(18.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (currentColorTheme == themeItem) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (currentColorTheme == themeItem) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                        // 3. Device UI Mode (Auto / Smart TV / Mobile)
                        Text("Режим интерфейса устройства", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Определение Smart TV / Мобильного вида:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DeviceLayoutMode.values().forEach { mode ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { viewModel.setDeviceLayoutMode(mode) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentDeviceMode == mode) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                border = if (currentDeviceMode == mode) {
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            DeviceLayoutMode.AUTO -> Icons.Default.BrightnessAuto
                                            DeviceLayoutMode.TV -> Icons.Default.Tv
                                            DeviceLayoutMode.MOBILE -> Icons.Default.PhoneAndroid
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (currentDeviceMode == mode) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (currentDeviceMode == mode) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                        // 3. Custom Mirror Domain
                        Text("Базовый домен API (Зеркало)", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "По умолчанию: ${AppPreferences.DEFAULT_DOMAIN}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = inputDomain,
                            onValueChange = { inputDomain = it },
                            placeholder = { Text("https://...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    inputDomain = AppPreferences.DEFAULT_DOMAIN
                                    viewModel.resetBaseDomain()
                                }
                            ) {
                                Text("Сбросить")
                            }
                            Button(
                                onClick = {
                                    if (inputDomain.isNotBlank()) {
                                        viewModel.setBaseDomain(inputDomain)
                                    }
                                }
                            ) {
                                Text("Сохранить домен")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                        // 3. Expiry Notifications Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Уведомление об истечении", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Напоминать за 30 мин до окончания подписки",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Готово")
                    }
                }
            )
        }

        // Batch Generate Dialog
        if (showBatchGenerateDialog) {
            AlertDialog(
                onDismissRequest = { showBatchGenerateDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Создание подписок")
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Выберите количество подписок для создания:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { if (batchCount > 1) batchCount-- },
                                    enabled = batchCount > 1
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Уменьшить")
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$batchCount",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (batchCount == 1) "Подписка" else "Подписок",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { if (batchCount < 10) batchCount++ },
                                    enabled = batchCount < 10
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Увеличить")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1, 3, 5, 10).forEach { preset ->
                                FilterChip(
                                    selected = batchCount == preset,
                                    onClick = { batchCount = preset },
                                    label = { Text("+$preset") }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val count = batchCount
                            showBatchGenerateDialog = false
                            viewModel.generateMultipleKeys(count)
                        },
                        enabled = uiState !is UiState.Loading
                    ) {
                        Text(if (batchCount == 1) "Создать 1 подписку" else "Создать $batchCount подписок")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchGenerateDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Manual Key Dialog
        if (showManualKeyDialog) {
            AlertDialog(
                onDismissRequest = { showManualKeyDialog = false },
                title = { Text("Добавить ключ вручную") },
                text = {
                    OutlinedTextField(
                        value = manualKeyInput,
                        onValueChange = { manualKeyInput = it },
                        label = { Text("Ключ подписки") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (manualKeyInput.isNotBlank()) {
                            viewModel.addManualKey(manualKeyInput.trim())
                        }
                        showManualKeyDialog = false
                        manualKeyInput = ""
                    }) {
                        Text("Добавить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualKeyDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter & Sort Bar
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Filter row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Фильтр:", style = MaterialTheme.typography.labelMedium)
                        FilterChip(
                            selected = currentFilter == KeyFilter.ALL,
                            onClick = { viewModel.setFilter(KeyFilter.ALL) },
                            label = { Text("Все (${allKeys.size})") }
                        )
                        FilterChip(
                            selected = currentFilter == KeyFilter.ACTIVE_ONLY,
                            onClick = { viewModel.setFilter(KeyFilter.ACTIVE_ONLY) },
                            label = { Text("Только активные") },
                            leadingIcon = if (currentFilter == KeyFilter.ACTIVE_ONLY) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }

                    // Sort row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Сортировка:", style = MaterialTheme.typography.labelMedium)
                        FilterChip(
                            selected = currentSort == KeySort.NEWEST,
                            onClick = { viewModel.setSort(KeySort.NEWEST) },
                            label = { Text("Новые") }
                        )
                        FilterChip(
                            selected = currentSort == KeySort.TIME_LEFT_ASC,
                            onClick = { viewModel.setSort(KeySort.TIME_LEFT_ASC) },
                            label = { Text("Срок (скоро истёк)") }
                        )
                        FilterChip(
                            selected = currentSort == KeySort.CABINET,
                            onClick = { viewModel.setSort(KeySort.CABINET) },
                            label = { Text("Кабинет") }
                        )
                    }
                }
            }

            if (keys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (allKeys.isEmpty()) "Подписок пока нет. Нажмите +, чтобы создать." else "Нет активных подписок по выбранному фильтру.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(keys, key = { it.id }) { keyEntity ->
                        KeyItemCard(
                            keyEntity = keyEntity,
                            onClick = { onNavigateToDetail(keyEntity.id) },
                            onRefresh = { viewModel.refreshKey(keyEntity.id) },
                            onDelete = { viewModel.deleteKey(keyEntity.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyItemCard(
    keyEntity: ProxyKeyEntity,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var timeLeft by remember { mutableStateOf(DateUtil.calculateTimeLeft(keyEntity.expiresAt)) }
    val isExpired = DateUtil.isExpired(keyEntity.expiresAt)

    LaunchedEffect(keyEntity.expiresAt) {
        while (true) {
            timeLeft = DateUtil.calculateTimeLeft(keyEntity.expiresAt)
            delay(30000)
        }
    }

    val context = LocalContext.current
    var showQrDialog by remember { mutableStateOf(false) }

    if (showQrDialog && keyEntity.subUrl != null) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("QR-код подписки") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    val qrPainter = rememberQrPainter(keyEntity.subUrl, size = 512)
                    if (qrPainter != null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Image(
                                painter = qrPainter,
                                contentDescription = "QR код подписки",
                                modifier = Modifier.size(220.dp).padding(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Отсканируйте код в приложении Happ или другом клиенте",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    com.example.util.AppLauncherHelper.copyAndLaunchHapp(
                        context = context,
                        subUrl = keyEntity.subUrl,
                        onCopied = { clipboardManager.setText(AnnotatedString(keyEntity.subUrl)) }
                    )
                    showQrDialog = false
                }) {
                    Text("Скопировать и открыть Happ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Подписка: ${keyEntity.id.take(8)}...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить статус", modifier = Modifier.size(18.dp))
                    }
                    if (keyEntity.subUrl != null) {
                        IconButton(onClick = { showQrDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.QrCode, contentDescription = "Показать QR-код", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = {
                            com.example.util.AppLauncherHelper.copyAndLaunchHapp(
                                context = context,
                                subUrl = keyEntity.subUrl,
                                onCopied = { clipboardManager.setText(AnnotatedString(keyEntity.subUrl)) }
                            )
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Скопировать и открыть Happ", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(keyEntity.subUrl))
                            android.widget.Toast.makeText(context, "Ссылка подписки скопирована", android.widget.Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать ссылку", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Кабинет: ${keyEntity.cabinetNo ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                if (keyEntity.panelUser != null) {
                    Text(text = "Пользователь: ${keyEntity.panelUser}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Истекает: ${keyEntity.expiresAt ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = if (isExpired) "Статус: Истекло" else "Осталось: $timeLeft",
                style = MaterialTheme.typography.bodySmall,
                color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (keyEntity.subUrl != null) {
                    FilledTonalButton(
                        onClick = { 
                            com.example.util.AppLauncherHelper.copyAndLaunchHapp(
                                context = context,
                                subUrl = keyEntity.subUrl,
                                onCopied = { clipboardManager.setText(AnnotatedString(keyEntity.subUrl)) }
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Скопировать и открыть Happ")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
