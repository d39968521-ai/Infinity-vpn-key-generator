package com.example.ui.tv

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.ProxyKeyEntity
import com.example.ui.KeyFilter
import com.example.ui.KeySort
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.theme.AppColorTheme
import com.example.util.AppLauncherHelper
import com.example.util.DateUtil
import com.example.util.DeviceLayoutMode
import com.example.util.rememberQrPainter
import kotlinx.coroutines.delay

/**
 * Android TV / Smart TV 10-foot optimized interface.
 * Designed specifically for remote control navigation (D-pad), high visual contrast,
 * large readable fonts from couch distance, and big-screen QR code scanning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvHomeScreen(
    viewModel: MainViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val allKeys by viewModel.keys.collectAsStateWithLifecycle()
    val keys by viewModel.filteredKeys.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
    val currentSort by viewModel.sort.collectAsStateWithLifecycle()
    val currentDomain by viewModel.baseDomain.collectAsStateWithLifecycle()
    val currentColorTheme by viewModel.colorTheme.collectAsStateWithLifecycle()
    val currentDeviceMode by viewModel.deviceLayoutMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showBatchDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var tvQrTargetKey by remember { mutableStateOf<ProxyKeyEntity?>(null) }
    var batchCount by remember { mutableStateOf(1) }
    var manualKeyInput by remember { mutableStateOf("") }

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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp, vertical = 20.dp)
        ) {
            // 1. Top TV Header (Brand, Mode Badge, Quick Remote Controls)
            TvHeaderBar(
                activeCount = keys.size,
                totalCount = allKeys.size,
                currentDomain = currentDomain,
                isLoading = uiState is UiState.Loading,
                onCreateSingle = { viewModel.generateKey() },
                onOpenBatch = { showBatchDialog = true },
                onOpenManual = { showManualDialog = true },
                onRefreshAll = { viewModel.refreshAllKeys() },
                onOpenSettings = { showSettingsDialog = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Filter & Sort Row (Large D-Pad Focusable Chips)
            TvFilterSortBar(
                currentFilter = currentFilter,
                currentSort = currentSort,
                onFilterChanged = { viewModel.setFilter(it) },
                onSortChanged = { viewModel.setSort(it) },
                allCount = allKeys.size
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Grid of TV Subscription Cards
            if (keys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TvEmptyStateCard(
                        isEmpty = allKeys.isEmpty(),
                        onCreateClick = { viewModel.generateKey() }
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(keys, key = { it.id }) { keyEntity ->
                        TvSubscriptionCard(
                            keyEntity = keyEntity,
                            onShowQr = { tvQrTargetKey = keyEntity },
                            onLaunchApp = {
                                if (keyEntity.subUrl != null) {
                                    AppLauncherHelper.copyAndLaunchHapp(
                                        context = context,
                                        subUrl = keyEntity.subUrl,
                                        onCopied = { clipboardManager.setText(AnnotatedString(keyEntity.subUrl)) }
                                    )
                                }
                            },
                            onCopyUrl = {
                                if (keyEntity.subUrl != null) {
                                    clipboardManager.setText(AnnotatedString(keyEntity.subUrl))
                                    android.widget.Toast.makeText(context, "Ссылка подписки скопирована", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRefreshKey = { viewModel.refreshKey(keyEntity.id) },
                            onDeleteKey = { viewModel.deleteKey(keyEntity.id) },
                            onClick = { onNavigateToDetail(keyEntity.id) }
                        )
                    }
                }
            }
        }
    }

    // --- TV Fullscreen QR Dialog (couch scanning feature) ---
    tvQrTargetKey?.let { targetKey ->
        BackHandler { tvQrTargetKey = null }
        TvFullscreenQrDialog(
            keyEntity = targetKey,
            onDismiss = { tvQrTargetKey = null }
        )
    }

    // --- TV Batch Creation Dialog ---
    if (showBatchDialog) {
        BackHandler { showBatchDialog = false }
        TvBatchGenerateDialog(
            batchCount = batchCount,
            isLoading = uiState is UiState.Loading,
            onCountChanged = { batchCount = it },
            onConfirm = { count ->
                showBatchDialog = false
                viewModel.generateMultipleKeys(count)
            },
            onDismiss = { showBatchDialog = false }
        )
    }

    // --- TV Manual Key Dialog ---
    if (showManualDialog) {
        BackHandler { showManualDialog = false }
        TvManualKeyDialog(
            keyInput = manualKeyInput,
            onKeyInputChanged = { manualKeyInput = it },
            onConfirm = { key ->
                if (key.isNotBlank()) viewModel.addManualKey(key)
                manualKeyInput = ""
                showManualDialog = false
            },
            onDismiss = {
                manualKeyInput = ""
                showManualDialog = false
            }
        )
    }

    // --- TV Settings Dialog ---
    if (showSettingsDialog) {
        BackHandler { showSettingsDialog = false }
        TvSettingsDialog(
            viewModel = viewModel,
            currentDeviceMode = currentDeviceMode,
            currentColorTheme = currentColorTheme,
            currentDomain = currentDomain,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

/**
 * Top TV bar with status, device badge and primary remote action buttons.
 */
@Composable
fun TvHeaderBar(
    activeCount: Int,
    totalCount: Int,
    currentDomain: String,
    isLoading: Boolean,
    onCreateSingle: () -> Unit,
    onOpenBatch: () -> Unit,
    onOpenManual: () -> Unit,
    onRefreshAll: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title & Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STRAUS TOP",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "SMART TV",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "Подписок: $activeCount из $totalCount • Зеркало: ${currentDomain.removePrefix("https://")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick TV Remote Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TvButton(
                    text = "Новая подписка",
                    icon = Icons.Default.Add,
                    isPrimary = true,
                    isLoading = isLoading,
                    onClick = onCreateSingle
                )
                TvButton(
                    text = "Пакет (+N)",
                    icon = Icons.Default.Layers,
                    onClick = onOpenBatch
                )
                TvButton(
                    text = "Вручную",
                    icon = Icons.Default.Key,
                    onClick = onOpenManual
                )
                TvButton(
                    text = "Обновить всё",
                    icon = Icons.Default.Refresh,
                    onClick = onRefreshAll
                )
                TvButton(
                    text = "Настройки",
                    icon = Icons.Default.Settings,
                    onClick = onOpenSettings
                )
            }
        }
    }
}

/**
 * Filter & Sort Bar for Smart TV with visible D-pad selection.
 */
@Composable
fun TvFilterSortBar(
    currentFilter: KeyFilter,
    currentSort: KeySort,
    onFilterChanged: (KeyFilter) -> Unit,
    onSortChanged: (KeySort) -> Unit,
    allCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filters
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Фильтр:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TvSelectableChip(
                text = "Все ($allCount)",
                selected = currentFilter == KeyFilter.ALL,
                onClick = { onFilterChanged(KeyFilter.ALL) }
            )
            TvSelectableChip(
                text = "Только активные",
                selected = currentFilter == KeyFilter.ACTIVE_ONLY,
                onClick = { onFilterChanged(KeyFilter.ACTIVE_ONLY) }
            )
        }

        // Sorts
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Сортировка:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TvSelectableChip(
                text = "Новые",
                selected = currentSort == KeySort.NEWEST,
                onClick = { onSortChanged(KeySort.NEWEST) }
            )
            TvSelectableChip(
                text = "Срок действия",
                selected = currentSort == KeySort.TIME_LEFT_ASC,
                onClick = { onSortChanged(KeySort.TIME_LEFT_ASC) }
            )
            TvSelectableChip(
                text = "Кабинет",
                selected = currentSort == KeySort.CABINET,
                onClick = { onSortChanged(KeySort.CABINET) }
            )
        }
    }
}

/**
 * Android TV Subscription Card with large typography, focus zoom, glow border,
 * and high-priority D-pad remote buttons.
 */
@Composable
fun TvSubscriptionCard(
    keyEntity: ProxyKeyEntity,
    onShowQr: () -> Unit,
    onLaunchApp: () -> Unit,
    onCopyUrl: () -> Unit,
    onRefreshKey: () -> Unit,
    onDeleteKey: () -> Unit,
    onClick: () -> Unit
) {
    var isCardFocused by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(DateUtil.calculateTimeLeft(keyEntity.expiresAt)) }
    val isExpired = DateUtil.isExpired(keyEntity.expiresAt)

    LaunchedEffect(keyEntity.expiresAt) {
        while (true) {
            timeLeft = DateUtil.calculateTimeLeft(keyEntity.expiresAt)
            delay(30000)
        }
    }

    val cardScale by animateFloatAsState(targetValue = if (isCardFocused) 1.025f else 1.0f, label = "card_scale")
    val borderStroke = if (isCardFocused) {
        BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .onFocusChanged { isCardFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = if (isCardFocused) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCardFocused) 8.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: ID + Status + Countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ключ: ${keyEntity.id.take(12)}...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isExpired) MaterialTheme.colorScheme.errorContainer else Color(0xFF1B5E20).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isExpired) MaterialTheme.colorScheme.error else Color(0xFF4CAF50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isExpired) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isExpired) "Истекла" else "Активна: $timeLeft",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isExpired) MaterialTheme.colorScheme.error else Color(0xFF81C784)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Кабинет №${keyEntity.cabinetNo ?: "—"}" + (if (keyEntity.panelUser != null) " • ${keyEntity.panelUser}" else ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Истекает: ${keyEntity.expiresAt ?: "Неизвестно"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // Remote Actions Row (Large, high-contrast TV buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Giant QR button (Most important TV feature)
                TvActionButton(
                    text = "QR-код",
                    icon = Icons.Default.QrCode,
                    highlight = true,
                    onClick = onShowQr,
                    modifier = Modifier.weight(1.3f)
                )

                // Launch in Happ / Client
                TvActionButton(
                    text = "Открыть в Happ",
                    icon = Icons.Default.RocketLaunch,
                    onClick = onLaunchApp,
                    modifier = Modifier.weight(1.5f)
                )

                // Copy URL
                TvActionButton(
                    text = "Копировать",
                    icon = Icons.Default.ContentCopy,
                    onClick = onCopyUrl,
                    modifier = Modifier.weight(1.2f)
                )

                // Refresh Status
                TvIconActionButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "Обновить статус",
                    onClick = onRefreshKey
                )

                // Delete
                TvIconActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Удалить подписку",
                    isDestructive = true,
                    onClick = onDeleteKey
                )
            }
        }
    }
}

/**
 * Smart TV Fullscreen QR Code dialog designed for scanning directly from couch.
 */
@Composable
fun TvFullscreenQrDialog(
    keyEntity: ProxyKeyEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCloseFocused by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "QR-КОД ДЛЯ СМАРТФОНА",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Наведите камеру смартфона для быстрого импорта в Happ / Hiddify / v2rayNG",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(44.dp)
                                    .onFocusChanged { isCloseFocused = it.isFocused }
                                    .focusable()
                                    .border(
                                        if (isCloseFocused) 2.dp else 0.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Big High-Res QR Code Card
                        if (keyEntity.subUrl != null) {
                            val qrPainter = rememberQrPainter(keyEntity.subUrl, size = 700)
                            if (qrPainter != null) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    tonalElevation = 6.dp,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Image(
                                        painter = qrPainter,
                                        contentDescription = "Большой QR-код подписки",
                                        modifier = Modifier
                                            .size(280.dp)
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Подписка ID: ${keyEntity.id}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Bottom Remote Control Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvButton(
                                text = "Скопировать ссылку",
                                icon = Icons.Default.ContentCopy,
                                onClick = {
                                    keyEntity.subUrl?.let {
                                        clipboardManager.setText(AnnotatedString(it))
                                        android.widget.Toast.makeText(context, "Ссылка скопирована", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            TvButton(
                                text = "Открыть в Happ / Клиенте",
                                icon = Icons.Default.RocketLaunch,
                                isPrimary = true,
                                onClick = {
                                    keyEntity.subUrl?.let {
                                        AppLauncherHelper.copyAndLaunchHapp(
                                            context = context,
                                            subUrl = it,
                                            onCopied = { clipboardManager.setText(AnnotatedString(it)) }
                                        )
                                    }
                                    onDismiss()
                                }
                            )
                            TvButton(
                                text = "Закрыть (Back)",
                                icon = Icons.Default.Check,
                                onClick = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * TV Batch Generation Dialog with D-Pad controls for incrementing/decrementing.
 */
@Composable
fun TvBatchGenerateDialog(
    batchCount: Int,
    isLoading: Boolean,
    onCountChanged: (Int) -> Unit,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Пакетное создание подписок")
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = "Используйте пульт для выбора количества подписок:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvButton(
                        text = "–",
                        icon = null,
                        onClick = { if (batchCount > 1) onCountChanged(batchCount - 1) },
                        modifier = Modifier.width(60.dp)
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    Text(
                        text = "$batchCount",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    TvButton(
                        text = "+",
                        icon = null,
                        onClick = { if (batchCount < 10) onCountChanged(batchCount + 1) },
                        modifier = Modifier.width(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(1, 3, 5, 10).forEach { preset ->
                        TvSelectableChip(
                            text = "+$preset",
                            selected = batchCount == preset,
                            onClick = { onCountChanged(preset) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TvButton(
                text = "Создать $batchCount подписок",
                icon = Icons.Default.Add,
                isPrimary = true,
                isLoading = isLoading,
                onClick = { onConfirm(batchCount) }
            )
        },
        dismissButton = {
            TvButton(
                text = "Отмена",
                icon = null,
                onClick = onDismiss
            )
        }
    )
}

/**
 * TV Manual Key Dialog.
 */
@Composable
fun TvManualKeyDialog(
    keyInput: String,
    onKeyInputChanged: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить ключ вручную") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Введите или вставьте ключ подписки Straus Top:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = onKeyInputChanged,
                    label = { Text("Ключ подписки (например, c03df...)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TvButton(
                text = "Добавить",
                icon = Icons.Default.Check,
                isPrimary = true,
                onClick = { onConfirm(keyInput.trim()) }
            )
        },
        dismissButton = {
            TvButton(
                text = "Отмена",
                icon = null,
                onClick = onDismiss
            )
        }
    )
}

/**
 * TV Settings Dialog with device layout switcher (Auto/TV/Phone), Color theme, and Mirror domain.
 */
@Composable
fun TvSettingsDialog(
    viewModel: MainViewModel,
    currentDeviceMode: DeviceLayoutMode,
    currentColorTheme: AppColorTheme,
    currentDomain: String,
    onDismiss: () -> Unit
) {
    var mirrorInput by remember { mutableStateOf(currentDomain) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Настройки Smart TV")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Device UI Layout Mode
                Text("Режим интерфейса устройства", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Выберите автоматическое определение или принудительный режим:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                DeviceLayoutMode.values().forEach { mode ->
                    TvSelectableCard(
                        title = mode.title,
                        selected = currentDeviceMode == mode,
                        onClick = { viewModel.setDeviceLayoutMode(mode) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                // 2. Color Themes
                Text("Цветовая палитра", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                val palettes = listOf(
                    AppColorTheme.SKY_BLUE to "Голубая (Sky Blue)",
                    AppColorTheme.DEEP_OCEAN to "Глубокий океан",
                    AppColorTheme.CYBER_NEON to "Кибер Неон",
                    AppColorTheme.EMERALD to "Изумрудная бирюза",
                    AppColorTheme.PURPLE to "Фиолетовая классика",
                    AppColorTheme.DYNAMIC to "Material You (Обои)"
                )

                palettes.forEach { (palette, title) ->
                    TvSelectableCard(
                        title = title,
                        selected = currentColorTheme == palette,
                        onClick = { viewModel.setColorTheme(palette) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                // 3. Mirror Domain
                Text("Базовый домен API (Зеркало)", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = mirrorInput,
                    onValueChange = { mirrorInput = it },
                    label = { Text("URL зеркала") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvButton(
                        text = "Сохранить зеркало",
                        icon = Icons.Default.Save,
                        onClick = { viewModel.setBaseDomain(mirrorInput.trim()) }
                    )
                    TvButton(
                        text = "Сброс",
                        icon = Icons.Default.Restore,
                        onClick = {
                            viewModel.resetBaseDomain()
                            mirrorInput = "https://straustopp.online"
                        }
                    )
                }
            }
        },
        confirmButton = {
            TvButton(
                text = "Готово",
                icon = Icons.Default.Check,
                isPrimary = true,
                onClick = onDismiss
            )
        }
    )
}

/**
 * Reusable D-Pad Remote Button with focus scale and luminous border.
 */
@Composable
fun TvButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isLoading: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1.0f, label = "btn_scale")

    val containerColor by animateColorAsState(
        targetValue = when {
            isPrimary && isFocused -> MaterialTheme.colorScheme.primary
            isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            isFocused -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        label = "btn_color"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        tonalElevation = if (isFocused) 6.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * TV Card action button.
 */
@Composable
fun TvActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.04f else 1.0f, label = "action_scale")

    Surface(
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = when {
            highlight && isFocused -> MaterialTheme.colorScheme.primary
            highlight -> MaterialTheme.colorScheme.primaryContainer
            isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (highlight && isFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (highlight && isFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * TV Icon action button.
 */
@Composable
fun TvIconActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.15f else 1.0f, label = "icon_scale")

    Surface(
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isFocused) {
            if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest
        } else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isFocused) BorderStroke(2.dp, if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) else null
    ) {
        Box(
            modifier = Modifier.padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Focusable Selectable Chip for Smart TV.
 */
@Composable
fun TvSelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.06f else 1.0f, label = "chip_scale")

    Surface(
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            if (isFocused) 2.dp else 1.dp,
            if (isFocused) MaterialTheme.colorScheme.primary else if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * TV Settings Selectable Card.
 */
@Composable
fun TvSelectableCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isFocused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Empty state card on Smart TV with prominent button.
 */
@Composable
fun TvEmptyStateCard(
    isEmpty: Boolean,
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.6f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(
            modifier = Modifier.padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isEmpty) "Подписок пока нет" else "Нет активных подписок по фильтру",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Нажмите кнопку ниже на пульте ДУ, чтобы мгновенно сгенерировать подписку с QR-кодом для вашего ТВ и смартфона.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            TvButton(
                text = "Создать подписку сейчас",
                icon = Icons.Default.Add,
                isPrimary = true,
                onClick = onCreateClick
            )
        }
    }
}
