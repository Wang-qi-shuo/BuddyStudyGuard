package com.buddy.studyguard.study.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.drop
import com.buddy.studyguard.R
import com.buddy.studyguard.common.data.db.entity.ChatMessageEntity
import com.buddy.studyguard.common.data.db.entity.ChatSenderType
import com.buddy.studyguard.common.ui.components.ChatImageWithSaveButton
import com.buddy.studyguard.common.ui.components.PixelEmptyState
import com.buddy.studyguard.common.ui.theme.BgCard
import com.buddy.studyguard.common.ui.theme.BgDeepest
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.ui.theme.TextPrimary
import com.buddy.studyguard.common.ui.theme.TextSecondary
import com.buddy.studyguard.common.ui.theme.neonBorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 家长-学生聊天界面（像素科技风 + 电路板背景）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    isParentMode: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isSending by viewModel.isSending.collectAsStateWithLifecycle()
    val sendError by viewModel.sendError.collectAsStateWithLifecycle()
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
        val file = File(dir, "chat_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        ).also { cameraTempUri = it }
    }

    LaunchedEffect(isParentMode) {
        if (isParentMode) viewModel.markAsRead()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 滚动到顶部时加载更早消息
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect { (index, offset) ->
                if (index == 0 && offset == 0) viewModel.loadOlderMessages()
            }
    }

    val accentColor = if (isParentMode) NeonMagenta else NeonCyan

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.neonBorder(accentColor, width = 1.dp, glowWidth = 2.dp),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "家庭聊天",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = accentColor,
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
                    .alpha(0.12f)
            )

            Column(Modifier.fillMaxSize()) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelEmptyState(
                            icon = "[ - ]",
                            title = "暂无消息",
                            subtitle = "发送第一条消息开始聊天",
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageBubble(msg, isParentMode = isParentMode)
                        }
                    }
                }

                if (selectedImageUri != null) {
                    ImagePreviewBar(
                        uri = selectedImageUri!!,
                        onRemove = { selectedImageUri = null }
                    )
                }

                if (sendError != null) {
                    Text(
                        text = sendError!!,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFFF4444),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                InputBar(
                    value = input,
                    onChange = { input = it },
                    onSend = {
                        viewModel.sendMessage(
                            content = input,
                            imageUri = selectedImageUri?.toString(),
                            isParent = isParentMode
                        )
                        input = ""
                        selectedImageUri = null
                    },
                    onPickImage = { showImagePickerSheet = true },
                    hasContent = input.isNotBlank() || selectedImageUri != null,
                    accentColor = accentColor
                )
            }
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

/** 单条消息气泡（带像素边框装饰）。 */
@Composable
private fun MessageBubble(msg: ChatMessageEntity, isParentMode: Boolean) {
    val isParent = msg.senderType == ChatSenderType.PARENT
    val isSelf = if (isParentMode) isParent else !isParent
    val hArrangement = if (isSelf) Arrangement.End else Arrangement.Start
    val hAlign = if (isSelf) Alignment.End else Alignment.Start
    val bubbleColor = if (isSelf) NeonCyan.copy(alpha = 0.12f) else NeonMagenta.copy(alpha = 0.10f)
    val borderColor = if (isSelf) NeonCyan else NeonMagenta

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = hArrangement
    ) {
        Column(
            horizontalAlignment = hAlign,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // 发送者名称标签（仅非本人消息显示）
            if (!isSelf) {
                val label = if (isParent) {
                    msg.senderName?.takeIf { it.isNotBlank() } ?: "家长"
                } else {
                    "我"
                }
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NeonCyan.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
                )
            }
            // 图片消息
            if (!msg.imageUri.isNullOrBlank()) {
                Box(modifier = Modifier.padding(bottom = 2.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BgCard,
                        modifier = Modifier.neonBorder(borderColor, width = 1.dp, glowWidth = 2.dp)
                    ) {
                        ChatImageWithSaveButton(
                            imageUri = msg.imageUri,
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .height(200.dp)
                        )
                    }
                }
            }
            // 文本消息
            if (msg.content.isNotBlank()) {
                Box(modifier = Modifier.padding(bottom = 2.dp)) {
                    Surface(
                        color = bubbleColor,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.neonBorder(borderColor, width = 1.dp, glowWidth = 2.dp)
                    ) {
                        Text(
                            text = msg.content,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            // 时间戳
            Text(
                text = formatTime(msg.timestamp),
                fontFamily = FontFamily.Monospace,
                color = TextSecondary.copy(alpha = 0.4f),
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

/** 已选图片预览条。 */
@Composable
private fun ImagePreviewBar(uri: Uri, onRemove: () -> Unit) {
    Surface(
        color = BgCard,
        modifier = Modifier
            .fillMaxWidth()
            .neonBorder(NeonCyan, width = 1.dp, glowWidth = 2.dp)
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = uri,
                contentDescription = "待发送图片",
                modifier = Modifier.size(60.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "已选择图片",
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除",
                    tint = NeonMagenta,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** 底部输入栏（带霓虹脉冲动画）。 */
@Composable
private fun InputBar(
    value: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    hasContent: Boolean,
    accentColor: Color
) {
    val transition = rememberInfiniteTransition(label = "inputPulse")
    val pulseBorderAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inputBorderPulse"
    )

    Surface(
        tonalElevation = 2.dp,
        color = BgCard,
        modifier = Modifier.neonBorder(
            accentColor.copy(alpha = pulseBorderAlpha),
            width = 1.dp,
            glowWidth = 3.dp
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPickImage) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "选择图片",
                    tint = accentColor
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = {
                    Text(
                        text = "输入消息…",
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary.copy(alpha = 0.5f),
                    )
                },
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = BgDeepest,
                    unfocusedContainerColor = BgDeepest,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    cursorColor = accentColor,
                ),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = hasContent
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.confirm),
                    tint = if (hasContent) accentColor else TextSecondary
                )
            }
        }
    }
}

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun formatTime(millis: Long): String = timeFormatter.format(Date(millis))

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
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Text("拍照", fontFamily = FontFamily.Monospace)
            }
        }
    }
}
