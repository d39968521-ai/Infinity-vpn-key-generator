package com.example.ui.detail

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.util.rememberQrPainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    keyId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val keys by viewModel.keys.collectAsStateWithLifecycle()
    val keyEntity = keys.find { it.id == keyId }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    var selectedFormat by remember { mutableStateOf("clash") }
    val formats = listOf("clash", "v2ray", "singbox", "base64")

    LaunchedEffect(uiState) {
        when (uiState) {
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
                title = { Text("Детали подписки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (keyEntity == null) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                Text("Подписка не найдена")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ключ подписки", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(keyEntity.id, fontFamily = FontFamily.Monospace)
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(keyEntity.id)) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sub URL", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(keyEntity.subUrl ?: "N/A", fontFamily = FontFamily.Monospace)
                        if (keyEntity.subUrl != null) {
                            Row {
                                IconButton(onClick = {
                                    com.example.util.AppLauncherHelper.copyAndLaunchHapp(
                                        context = context,
                                        subUrl = keyEntity.subUrl,
                                        onCopied = { clipboardManager.setText(AnnotatedString(keyEntity.subUrl)) }
                                    )
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = "Launch Happ")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { clipboardManager.setText(AnnotatedString(keyEntity.subUrl)) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                    }
                    if (keyEntity.subUrl != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = {
                                com.example.util.AppLauncherHelper.copyAndLaunchHapp(
                                    context = context,
                                    subUrl = keyEntity.subUrl,
                                    onCopied = { clipboardManager.setText(AnnotatedString(keyEntity.subUrl)) }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Sub URL & Launch Happ")
                        }
                    }
                }
            }

            if (keyEntity.subUrl != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Subscription QR Code", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val qrPainter = rememberQrPainter(keyEntity.subUrl, size = 600)
                        if (qrPainter != null) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 2.dp,
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Image(
                                    painter = qrPainter,
                                    contentDescription = "Subscription QR Code",
                                    modifier = Modifier.size(240.dp).padding(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Point Happ or another proxy client camera here to import",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text("Fetch Config", style = MaterialTheme.typography.titleMedium)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                formats.forEach { format ->
                    FilterChip(
                        selected = selectedFormat == format,
                        onClick = { selectedFormat = format },
                        label = { Text(format.uppercase()) }
                    )
                }
            }

            Button(
                onClick = { 
                    if(keyEntity.subUrl != null) {
                        viewModel.fetchConfig(keyEntity.subUrl, selectedFormat) 
                    }
                },
                enabled = keyEntity.subUrl != null && uiState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Download $selectedFormat Config")
                }
            }

            if (uiState is UiState.ConfigFetched) {
                val configFetched = uiState as UiState.ConfigFetched
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Config Payload", style = MaterialTheme.typography.titleSmall)
                            Row {
                                IconButton(onClick = { clipboardManager.setText(AnnotatedString(configFetched.config)) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, configFetched.config)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = configFetched.config,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
