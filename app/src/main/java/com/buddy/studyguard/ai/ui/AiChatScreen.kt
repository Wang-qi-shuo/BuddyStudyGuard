package com.buddy.studyguard.ai.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.buddy.studyguard.R
import com.buddy.studyguard.common.data.db.entity.AiMessageEntity
import com.buddy.studyguard.common.data.db.entity.AiRole
import com.buddy.studyguard.common.ui.components.PixelEmptyState
import com.buddy.studyguard.common.ui.theme.BgCard
import com.buddy.studyguard.common.ui.theme.BgDeepest
import com.buddy.studyguard.common.ui.theme.BgPrimary
import com.buddy.studyguard.common.ui.theme.NeonAmber
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.ui.theme.PixelButtonStyles
import com.buddy.studyguard.common.ui.theme.TextPrimary
import com.buddy.studyguard.common.ui.theme.TextSecondary
import com.buddy.studyguard.common.ui.theme.neonBorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI 对话页（像素科技风 + 电路板背景）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBack: () -> Unit = {},
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showImagePickerSheet by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    var cameraTempUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) cameraTempUri?.let { selectedImageUri = it }
    }

    fun createCameraUri(): Uri {
        val dir = File(context.cacheDir, "camera_images").apply { mkdirs() }
        val file = File(dir, "ai_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        ).also { cameraTempUri = it }
    }

    var showApiKeyDialog by rememberSaveable { mutableStateOf(!uiState.apiKeySaved) }
    var dialogDismissed by rememberSaveable { mutableStateOf(false) }

    if (uiState.apiKeySaved) {
        showApiKeyDialog = false
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            onSave = { key -> viewModel.saveApiKey(key) },
            onCancel = {
                showApiKeyDialog = false
                dialogDismissed = true
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.neonBorder(NeonCyan, width = 1.dp, glowWidth = 2.dp),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "AI 学习助手",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = NeonCyan,
                            )
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = BgDeepest,
                    ),
                )
            }
        }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(BgDeepest)
        ) {
            // 电路板背景纹理
            Image(
                painter = painterResource(id = R.drawable.bg_circuit_board),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.10f)
            )

            Column(Modifier.fillMaxSize()) {
                if (dialogDismissed && !uiState.apiKeySaved) {
                    ApiKeyHint()
                }
                if (uiState.offline) OfflineBanner()

                if (uiState.messages.isEmpty() && !uiState.loading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelEmptyState(
                            icon = ">_",
                            title = "AI 学习助手",
                            subtitle = "向我提问任何学习问题",
                            color = NeonCyan
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { msg ->
                            MessageBubble(msg)
                        }
                        if (uiState.loading) item { TypingIndicator() }
                        if (uiState.error != null) item {
                            ErrorRetry(onRetry = { viewModel.retryLast() })
                        }
                    }
                }

                if (selectedImageUri != null) {
                    ImagePreviewBar(
                        uri = selectedImageUri!!,
                        onRemove = { selectedImageUri = null }
                    )
                }

                InputBar(
                    value = input,
                    onChange = { input = it },
                    onSend = {
                        viewModel.send(input, selectedImageUri?.toString())
                        input = ""
                        selectedImageUri = null
                    },
                    onPickImage = { showImagePickerSheet = true },
                    hasContent = input.isNotBlank() || selectedImageUri != null
                )
            }
        }

        if (showImagePickerSheet) {
            ImagePickerSheet(
                onGallery = {
                    showImagePickerSheet = false
                    galleryLauncher.launch("image/*")
                },
                onCamera = {
                    showImagePickerSheet = false
                    cameraLauncher.launch(createCameraUri())
                },
                onDismiss = { showImagePickerSheet = false }
            )
        }
    }

    val lastIndex = uiState.messages.size +
        (if (uiState.loading) 1 else 0) +
        (if (uiState.error != null) 1 else 0)
    LaunchedEffect(lastIndex) {
        if (lastIndex > 0) listState.animateScrollToItem(lastIndex - 1)
    }
}

@Composable
private fun ApiKeyHint() {
    Surface(
        color = NeonMagenta.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "AI功能需要API Key才能使用，可在设置中配置",
            color = NeonMagenta,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ApiKeyDialog(
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var keyInput by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = "请输入豆包 API Key",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "前往火山方舟控制台获取 API Key 以启用 AI 助手功能。",
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .neonBorder(NeonCyan, width = 1.dp, glowWidth = 4.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "sk-xxxxxxxxxxxxxxxx",
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BgDeepest,
                        unfocusedContainerColor = BgPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                        cursorColor = NeonCyan,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(keyInput) },
                enabled = keyInput.isNotBlank(),
                shape = PixelButtonStyles.Shape,
                border = BorderStroke(1.dp, NeonCyan),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = NeonCyan.copy(alpha = 0.10f),
                    contentColor = NeonCyan,
                    disabledContainerColor = BgCard,
                    disabledContentColor = TextSecondary,
                ),
            ) {
                Text("保存", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                shape = PixelButtonStyles.Shape,
                border = BorderStroke(1.dp, TextSecondary),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = BgCard, contentColor = TextSecondary
                ),
            ) {
                Text("取消", fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = BgCard,
        titleContentColor = NeonCyan,
        textContentColor = TextPrimary,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun OfflineBanner() {
    Surface(color = NeonAmber, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ai_offline_hint),
            color = BgDeepest,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun MessageBubble(msg: AiMessageEntity) {
    val isUser = msg.role == AiRole.USER
    val bg = if (isUser) NeonCyan else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isUser) BgDeepest else MaterialTheme.colorScheme.onSurfaceVariant
    val hAlign = if (isUser) Alignment.End else Alignment.Start
    val hArrangement = if (isUser) Arrangement.End else Arrangement.Start
    val borderColor = if (isUser) NeonCyan.copy(alpha = 0.5f) else NeonMagenta.copy(alpha = 0.3f)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = hArrangement) {
        Column(horizontalAlignment = hAlign) {
            Surface(
                color = bg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.neonBorder(borderColor, width = 1.dp, glowWidth = 2.dp)
            ) {
                Text(
                    text = msg.content,
                    color = fg,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            if (msg.fromOfflineCache) {
                Text(
                    text = "离线",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonAmber,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "● ● ●",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ErrorRetry(onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.ai_error),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    hasContent: Boolean
) {
    val transition = rememberInfiniteTransition(label = "aiInputPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.neonBorder(
            NeonCyan.copy(alpha = pulseAlpha),
            width = 1.dp,
            glowWidth = 3.dp
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPickImage) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = "选择图片",
                    tint = NeonCyan
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = stringResource(R.string.ai_input_hint),
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0xFF333355),
                    cursorColor = NeonCyan
                ),
                maxLines = 4,
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = hasContent,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = NeonCyan,
                    contentColor = BgDeepest
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.confirm)
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewBar(uri: Uri, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        color = BgCard,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = uri,
                    contentDescription = "图片预览",
                    modifier = Modifier.size(80.dp).neonBorder(NeonCyan, width = 1.dp, glowWidth = 2.dp),
                )
                Text(
                    text = "图片已选中",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除图片",
                    tint = NeonMagenta
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerSheet(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = BgCard,
        contentColor = TextPrimary
    ) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "选择图片来源",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = NeonCyan,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            TextButton(
                onClick = onGallery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Text("从相册选择", fontFamily = FontFamily.Monospace)
            }
            TextButton(
                onClick = onCamera,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Text("拍照", fontFamily = FontFamily.Monospace)
            }
        }
    }
}
