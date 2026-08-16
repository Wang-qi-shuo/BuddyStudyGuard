package com.buddy.studyguard.common.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.buddy.studyguard.common.cloud.ImageBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 聊天图片 + 右上角"保存到相册"下载按钮。
 *
 * 点击下载按钮将图片保存到系统相册（MediaStore.Images）：
 * - Android 10+（API 29+）：直接写入，无需权限。
 * - Android 9 及以下（API 26-28）：运行时申请 WRITE_EXTERNAL_STORAGE 后写入。
 * 保存成功 Toast 提示"已保存到相册"，失败提示原因。
 */
@Composable
fun ChatImageWithSaveButton(
    imageUri: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val doSave: (String) -> Unit = { uri ->
        scope.launch {
            val error = withContext(Dispatchers.IO) {
                ImageBase64.saveToGallery(context, uri)
            }
            Toast.makeText(
                context,
                error ?: "已保存到相册",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            doSave(imageUri)
        } else {
            Toast.makeText(context, "未授予存储权限，无法保存到相册", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = imageUri,
            contentDescription = "图片",
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
        IconButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    doSave(imageUri)
                } else {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        doSave(imageUri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "保存到相册",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
