package com.buddy.studyguard.parent.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buddy.studyguard.R
import com.buddy.studyguard.common.data.db.entity.ChatMessageEntity
import com.buddy.studyguard.common.data.db.entity.ChatSenderType
import com.buddy.studyguard.common.ui.components.ChatImageWithSaveButton
import com.buddy.studyguard.study.ui.components.PixelCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.drop
import java.io.File
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun ParentMessageScreen(viewModel: ParentMessageViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val sendError by viewModel.sendError.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
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
        val file = File(dir, "parent_chat_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        ).also { cameraTempUri = it }
    }

    // 滚动到顶部时加载更早消息
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect { (index, offset) ->
                if (index == 0 && offset == 0) viewModel.loadOlderMessages()
            }
    }

    val hasContent = input.isNotBlank() || selectedImageUri != null

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "发送消息给孩子",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("消息内容") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        if (selectedImageUri != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "待发送图片",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "已选择图片",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(onClick = { selectedImageUri = null }) {
                    Icon(Icons.Default.Close, contentDescription = "移除图片")
                }
            }
        }

        if (sendError != null) {
            Text(
                text = sendError!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "从相册选择")
            }
            IconButton(onClick = { cameraLauncher.launch(createCameraUri()) }) {
                Icon(Icons.Default.CameraAlt, contentDescription = "拍照")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.sendMessage(input, selectedImageUri?.toString())
                    input = ""
                    selectedImageUri = null
                },
                enabled = hasContent,
                modifier = Modifier.weight(1f)
            ) { Text(stringResource(R.string.save)) }
        }

        Text(
            text = "历史消息",
            style = MaterialTheme.typography.titleSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageHistoryRow(msg)
            }
        }
    }
}

@Composable
private fun MessageHistoryRow(msg: ChatMessageEntity) {
    val isParent = msg.senderType == ChatSenderType.PARENT
    PixelCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isParent) "家长" else "孩子",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = msg.content, style = MaterialTheme.typography.bodyMedium)
        if (!msg.imageUri.isNullOrBlank()) {
            ChatImageWithSaveButton(
                imageUri = msg.imageUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
        Text(
            text = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
